package com.hannto.io;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;

public abstract class HanntoRequest<T> implements Comparable<HanntoRequest<T>> {

    /** Callback to notify when the network request returns. */
    /* package */ interface NetworkRequestCompleteListener {

        /** Callback when a network response has been received. */
        void onResponseReceived(HanntoRequest request, HanntoResponse response);

        /** Callback when request returns from network without valid response. */
        void onNoUsableResponseReceived(HanntoRequest request);
    }

    /**
     * Request data of this request.
     */
    private final byte[] mData;

    /** Lock to guard state which can be mutated after a request is added to the queue. */
    private final Object mLock = new Object();

    /** Listener interface for errors. */
    @Nullable
    @GuardedBy("mLock")
    private HanntoResponse.ErrorListener mErrorListener;


    /** Sequence number of this request, used to enforce FIFO ordering. */
    private Integer mSequence;

    /** The request queue this request is associated with. */
    private HanntoRequestQueue mRequestQueue;

    /** Whether or not this request has been canceled. */
    @GuardedBy("mLock")
    private boolean mCanceled = false;

    /** Whether or not a response has been delivered for this request yet. */
    @GuardedBy("mLock")
    private boolean mResponseDelivered = false;

    /** The retry policy for this request. */
    private HanntoRetryPolicy mRetryPolicy;

    /** An opaque token tagging this request; used for bulk cancellation. */
    private Object mTag;

    /** Listener that will be notified when a response has been delivered. */
    @GuardedBy("mLock")
    private NetworkRequestCompleteListener mRequestCompleteListener;

    public HanntoRequest(byte[] data, @Nullable HanntoResponse.ErrorListener listener) {
        mData = data;
        mErrorListener = listener;
        setRetryPolicy(new HanntoDefaultRetryPolicy());

    }

    /** Return the method for this request.  */
    public byte[] getData() {
        return mData;
    }

    /**
     * Set a tag on this request. Can be used to cancel all requests with this tag by {@link
     * HanntoRequestQueue#cancelAll(Object)}.
     *
     * @return This Request object to allow for chaining.
     */
    public HanntoRequest setTag(Object tag) {
        mTag = tag;
        return this;
    }

    /**
     * Returns this request's tag.
     *
     * @see HanntoRequest#setTag(Object)
     */
    public Object getTag() {
        return mTag;
    }

    /** @return this request's {@link com.android.volley.Response.ErrorListener}. */
    @Nullable
    public HanntoResponse.ErrorListener getErrorListener() {
        synchronized (mLock) {
            return mErrorListener;
        }
    }

    /**
     * Sets the retry policy for this request.
     *
     * @return This Request object to allow for chaining.
     */
    public HanntoRequest setRetryPolicy(HanntoRetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
        return this;
    }

    /**
     * Notifies the request queue that this request has finished (successfully or with error).
     *
     * <p>Also dumps all events from this request's event log; for debugging.
     */
    void finish(final String tag) {
        if (mRequestQueue != null) {
            mRequestQueue.finish(this);
        }
//        if (MarkerLog.ENABLED) {
//            final long threadId = Thread.currentThread().getId();
//            if (Looper.myLooper() != Looper.getMainLooper()) {
//                // If we finish marking off of the main thread, we need to
//                // actually do it on the main thread to ensure correct ordering.
//                Handler mainThread = new Handler(Looper.getMainLooper());
//                mainThread.post(
//                        new Runnable() {
//                            @Override
//                            public void run() {
//                                mEventLog.add(tag, threadId);
//                                mEventLog.finish(HanntoRequest.this.toString());
//                            }
//                        });
//                return;
//            }
//
//            mEventLog.add(tag, threadId);
//            mEventLog.finish(this.toString());
//        }
    }

    /**
     * Associates this request with the given queue. The request queue will be notified when this
     * request has finished.
     *
     * @return This Request object to allow for chaining.
     */
    public HanntoRequest setRequestQueue(HanntoRequestQueue requestQueue) {
        mRequestQueue = requestQueue;
        return this;
    }

    /**
     * Sets the sequence number of this request. Used by {@link HanntoRequestQueue}.
     *
     * @return This Request object to allow for chaining.
     */
    public final HanntoRequest setSequence(int sequence) {
        mSequence = sequence;
        return this;
    }

    /** Returns the sequence number of this request. */
    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    /**
     * Mark this request as canceled.
     *
     * <p>No callback will be delivered as long as either:
     *
     * <ul>
     *   <li>This method is called on the same thread as the {@link HanntoResponseDelivery} is running on.
     *       By default, this is the main thread.
     *   <li>The request subclass being used overrides cancel() and ensures that it does not invoke
     *       the listener in {@link #deliverResponse} after cancel() has been called in a
     *       thread-safe manner.
     * </ul>
     *
     * <p>There are no guarantees if both of these conditions aren't met.
     */
    @CallSuper
    public void cancel() {
        synchronized (mLock) {
            mCanceled = true;
            mErrorListener = null;
        }
    }

    /** Returns true if this request has been canceled. */
    public boolean isCanceled() {
        synchronized (mLock) {
            return mCanceled;
        }
    }

    /**
     * Priority values. Requests will be processed from higher priorities to lower priorities, in
     * FIFO order.
     */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }

    /** Returns the {@link HanntoRequest.Priority} of this request; {@link HanntoRequest.Priority#NORMAL} by default. */
    public HanntoRequest.Priority getPriority() {
        return HanntoRequest.Priority.NORMAL;
    }

    /**
     * Returns the socket timeout in milliseconds per retry attempt. (This value can be changed per
     * retry attempt if a backoff is specified via backoffTimeout()). If there are no retry attempts
     * remaining, this will cause delivery of a {@link TimeoutError} error.
     */
    public final int getTimeoutMs() {
        return getRetryPolicy().getCurrentTimeout();
    }

    /** Returns the retry policy that should be used for this request. */
    public HanntoRetryPolicy getRetryPolicy() {
        return mRetryPolicy;
    }

    /**
     * Mark this request as having a response delivered on it. This can be used later in the
     * request's lifetime for suppressing identical responses.
     */
    public void markDelivered() {
        synchronized (mLock) {
            mResponseDelivered = true;
        }
    }

    /** Returns true if this request has had a response delivered for it. */
    public boolean hasHadResponseDelivered() {
        synchronized (mLock) {
            return mResponseDelivered;
        }
    }

    /**
     * Subclasses must implement this to parse the raw network response and return an appropriate
     * response type. This method will be called from a worker thread. The response will not be
     * delivered if you return null.
     *
     * @param response Response from the network
     * @return The parsed response, or null in the case of an error
     */
    protected abstract HanntoResponse<T> parseNetworkResponse(HanntoNetworkResponse response);

    /**
     * Subclasses can override this method to parse 'networkError' and return a more specific error.
     *
     * <p>The default implementation just returns the passed 'networkError'.
     *
     * @param volleyError the error retrieved from the network
     * @return an NetworkError augmented with additional information
     */
    protected HanntoError parseNetworkError(HanntoError volleyError) {
        return volleyError;
    }

    /**
     * Subclasses must implement this to perform delivery of the parsed response to their listeners.
     * The given response is guaranteed to be non-null; responses that fail to parse are not
     * delivered.
     *
     * @param response The parsed response returned by {@link
     *     #parseNetworkResponse(HanntoNetworkResponse)}
     */
    protected abstract void deliverResponse(T response);

    /**
     * Delivers error message to the ErrorListener that the Request was initialized with.
     *
     * @param error Error details
     */
    public void deliverError(HanntoError error) {
        HanntoResponse.ErrorListener listener;
        synchronized (mLock) {
            listener = mErrorListener;
        }
        if (listener != null) {
            listener.onErrorResponse(error);
        }
    }

    /**
     * {@link NetworkRequestCompleteListener} that will receive callbacks when the request returns
     * from the network.
     */
    /* package */ void setNetworkRequestCompleteListener(
            NetworkRequestCompleteListener requestCompleteListener) {
        synchronized (mLock) {
            mRequestCompleteListener = requestCompleteListener;
        }
    }

    /**
     * Notify NetworkRequestCompleteListener that a valid response has been received which can be
     * used for other, waiting requests.
     *
     * @param response received from the network
     */
    /* package */ void notifyListenerResponseReceived(HanntoResponse<?> response) {
        NetworkRequestCompleteListener listener;
        synchronized (mLock) {
            listener = mRequestCompleteListener;
        }
        if (listener != null) {
            listener.onResponseReceived(this, response);
        }
    }

    /**
     * Notify NetworkRequestCompleteListener that the network request did not result in a response
     * which can be used for other, waiting requests.
     */
    /* package */ void notifyListenerResponseNotUsable() {
        NetworkRequestCompleteListener listener;
        synchronized (mLock) {
            listener = mRequestCompleteListener;
        }
        if (listener != null) {
            listener.onNoUsableResponseReceived(this);
        }
    }

    /**
     * Our comparator sorts from high to low priority, and secondarily by sequence number to provide
     * FIFO ordering.
     */
    @Override
    public int compareTo(HanntoRequest<T> other) {
        Priority left = this.getPriority();
        Priority right = other.getPriority();

        // High-priority requests are "lesser" so they are sorted to the front.
        // Equal priorities are sorted by sequence number to provide FIFO ordering.
        return left == right ? this.mSequence - other.mSequence : right.ordinal() - left.ordinal();
    }

    @Override
    public String toString() {
//        String trafficStatsTag = "0x" + Integer.toHexString(getTrafficStatsTag());
        return (isCanceled() ? "[X] " : "[ ] ")
//                + getUrl()
//                + " "
//                + trafficStatsTag
//                + " "
                + getPriority()
                + " "
                + mSequence;
    }
}
