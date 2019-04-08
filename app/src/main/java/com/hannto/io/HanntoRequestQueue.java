package com.hannto.io;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class HanntoRequestQueue {

    /** Callback interface for completed requests. */
    public interface RequestFinishedListener<T> {
        /** Called when a request has finished processing. */
        void onRequestFinished(HanntoRequest<T> request);
    }

    /** Used for generating monotonically-increasing sequence numbers for requests. */
    private final AtomicInteger mSequenceGenerator = new AtomicInteger();

    /**
     * The set of all requests currently being processed by this RequestQueue. A Request will be in
     * this set if it is waiting in any queue or currently being processed by any dispatcher.
     */
    private final Set<HanntoRequest> mCurrentRequests = new HashSet<>();

//    /** The cache triage queue.
//     *  暂不使用缓存机制
//     */
//    private final PriorityBlockingQueue<HanntoRequest> mCacheQueue = new PriorityBlockingQueue<>();

    /** The queue of requests that are actually going out to the network. */
    private final PriorityBlockingQueue<HanntoRequest> mNetworkQueue = new PriorityBlockingQueue<>();

    /** Number of network request dispatcher threads to start. */
    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;

//    /** Cache interface for retrieving and storing responses. */
//    private final Cache mCache;

    /** Network interface for performing requests. */
    private final HanntoNetwork mNetwork;

    /** Response delivery mechanism. */
    private final HanntoResponseDelivery mDelivery;

    /** The network dispatchers. */
    private final HanntoNetworkDispatcher[] mDispatchers;

//    /** The cache dispatcher. */
//    private CacheDispatcher mCacheDispatcher;

    private final List<RequestFinishedListener> mFinishedListeners = new ArrayList<>();

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
//     * @param cache A Cache to use for persisting responses to disk
     * @param network A Network interface for performing HTTP requests
     * @param threadPoolSize Number of network dispatcher threads to create
     * @param delivery A ResponseDelivery interface for posting responses and errors
     */
    public HanntoRequestQueue(HanntoNetwork network, int threadPoolSize, HanntoResponseDelivery delivery) {
//        mCache = cache;
        mNetwork = network;
        mDispatchers = new HanntoNetworkDispatcher[threadPoolSize];
        mDelivery = delivery;
    }

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
//     * @param cache A Cache to use for persisting responses to disk
     * @param network A Network interface for performing HTTP requests
     * @param threadPoolSize Number of network dispatcher threads to create
     */
    public HanntoRequestQueue(HanntoNetwork network, int threadPoolSize) {
        this(
                network,
                threadPoolSize,
                new HanntoExecutorDelivery(new Handler(Looper.getMainLooper())));
    }

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
//     * @param cache A Cache to use for persisting responses to disk
     * @param network A Network interface for performing HTTP requests
     */
    public HanntoRequestQueue(HanntoNetwork network) {
        this(network, DEFAULT_NETWORK_THREAD_POOL_SIZE);
    }

    /** Starts the dispatchers in this queue. */
    public void start() {
        stop(); // Make sure any currently running dispatchers are stopped.
        // Create the cache dispatcher and start it.
//        mCacheDispatcher = new CacheDispatcher(mCacheQueue, mNetworkQueue, mCache, mDelivery);
//        mCacheDispatcher.start();

        // Create network dispatchers (and corresponding threads) up to the pool size.
        for (int i = 0; i < mDispatchers.length; i++) {
            HanntoNetworkDispatcher networkDispatcher =
                    new HanntoNetworkDispatcher(mNetworkQueue, mNetwork, mDelivery);
            mDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }
    }

    /** Stops the cache and network dispatchers. */
    public void stop() {
//        if (mCacheDispatcher != null) {
//            mCacheDispatcher.quit();
//        }
        for (final HanntoNetworkDispatcher mDispatcher : mDispatchers) {
            if (mDispatcher != null) {
                mDispatcher.quit();
            }
        }
    }

    /** Gets a sequence number. */
    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    /**
     * A simple predicate or filter interface for Requests, for use by {@link
     * HanntoRequestQueue#cancelAll(RequestFilter)}.
     */
    public interface RequestFilter {
        boolean apply(HanntoRequest<?> request);
    }

    /**
     * Cancels all requests in this queue for which the given filter applies.
     *
     * @param filter The filtering function to use
     */
    public void cancelAll(RequestFilter filter) {
        synchronized (mCurrentRequests) {
            for (HanntoRequest<?> request : mCurrentRequests) {
                if (filter.apply(request)) {
                    request.cancel();
                }
            }
        }
    }

    /**
     * Cancels all requests in this queue with the given tag. Tag must be non-null and equality is
     * by identity.
     */
    public void cancelAll(final Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancelAll with a null tag");
        }
        cancelAll(
                new RequestFilter() {
                    @Override
                    public boolean apply(HanntoRequest<?> request) {
                        return request.getTag() == tag;
                    }
                });
    }

    /**
     * Adds a Request to the dispatch queue.
     *
     * @param request The request to service
     * @return The passed-in request
     */
    public <T> HanntoRequest<T> add(HanntoRequest<T> request) {
        // Tag the request as belonging to this queue and add it to the set of current requests.
        request.setRequestQueue(this);
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }

        // Process requests in the order they are added.
        request.setSequence(getSequenceNumber());
//        request.addMarker("add-to-queue");

        // If the request is uncacheable, skip the cache queue and go straight to the network.
//        if (!request.shouldCache()) {
            mNetworkQueue.add(request);
            return request;
//        }
//        mCacheQueue.add(request);
//        return request;
    }


    /**
     * Called from {@link HanntoRequest#finish(String)}, indicating that processing of the given request
     * has finished.
     */
    @SuppressWarnings("unchecked") // see above note on RequestFinishedListener
     void finish(HanntoRequest request) {
        // Remove from the set of requests currently being processed.
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }
        synchronized (mFinishedListeners) {
            for (RequestFinishedListener listener : mFinishedListeners) {
                listener.onRequestFinished(request);
            }
        }
    }

    public <T> void addRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.add(listener);
        }
    }

    /** Remove a RequestFinishedListener. Has no effect if listener was not previously added. */
    public <T> void removeRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.remove(listener);
        }
    }
}
