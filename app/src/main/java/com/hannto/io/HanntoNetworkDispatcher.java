package com.hannto.io;

import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.BlockingQueue;

public class HanntoNetworkDispatcher extends Thread {

    /** The queue of requests to service. */
    private final BlockingQueue<HanntoRequest> mQueue;
    /** The network interface for processing requests. */
    private final HanntoNetwork mNetwork;
//    /** The cache to write to. */
//    private final Cache mCache;
    /** For posting responses and errors. */
    private final HanntoResponseDelivery mDelivery;
    /** Used for telling us to die. */
    private volatile boolean mQuit = false;

    /**
     * Creates a new network dispatcher thread. You must call {@link #start()} in order to begin
     * processing.
     *
     * @param queue Queue of incoming requests for triage
     * @param network Network interface to use for performing requests
//     * @param cache Cache interface to use for writing responses to cache
     * @param delivery Delivery interface to use for posting responses
     */
    public HanntoNetworkDispatcher(
            BlockingQueue<HanntoRequest> queue,
            HanntoNetwork network,
//            Cache cache,
            HanntoResponseDelivery delivery) {
        mQueue = queue;
        mNetwork = network;
//        mCache = cache;
        mDelivery = delivery;
    }

    /**
     * Forces this dispatcher to quit immediately. If any requests are still in the queue, they are
     * not guaranteed to be processed.
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }

//    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
//    private void addTrafficStatsTag(HanntoRequest request) {
//        // Tag the request (if API >= 14)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
//        }
//    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                processRequest();
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    Thread.currentThread().interrupt();
                    return;
                }
//                VolleyLog.e(
//                        "Ignoring spurious interrupt of NetworkDispatcher thread; "
//                                + "use quit() to terminate it");
            }
        }
    }

    private void processRequest() throws InterruptedException {
        // Take a request from the queue.
        HanntoRequest request = mQueue.take();
        processRequest(request);
    }

    @VisibleForTesting
    void processRequest(HanntoRequest request) {
        long startTimeMs = SystemClock.elapsedRealtime();
        try {
//            request.addMarker("network-queue-take");

            // If the request was cancelled already, do not perform the
            // network request.
            if (request.isCanceled()) {
                request.finish("network-discard-cancelled");
                request.notifyListenerResponseNotUsable();
                return;
            }

//            addTrafficStatsTag(request);

            // Perform the network request.
            HanntoNetworkResponse networkResponse = mNetwork.performRequest(request);
//            request.addMarker("network-http-complete");

            // If the server returned 304 AND we delivered a response already,
            // we're done -- don't deliver a second identical response.
//            if (networkResponse.notModified && request.hasHadResponseDelivered()) {
//                request.finish("not-modified");
//                request.notifyListenerResponseNotUsable();
//                return;
//            }

            // Parse the response here on the worker thread.
            HanntoResponse response = request.parseNetworkResponse(networkResponse);
//            request.addMarker("network-parse-complete");

            // Write to cache if applicable.
            // TODO: Only update cache metadata instead of entire record for 304s.
//            if (request.shouldCache() && response.cacheEntry != null) {
//                mCache.put(request.getCacheKey(), response.cacheEntry);
//                request.addMarker("network-cache-written");
//            }

            // Post the response back.
            request.markDelivered();
            mDelivery.postResponse(request, response);
            request.notifyListenerResponseReceived(response);
        } catch (HanntoError hanntoError) {
            hanntoError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
            parseAndDeliverNetworkError(request, hanntoError);
            request.notifyListenerResponseNotUsable();
        } catch (Exception e) {
//            VolleyLog.e(e, "Unhandled exception %s", e.toString());
            HanntoError volleyError = new HanntoError(e);
            volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
            mDelivery.postError(request, volleyError);
            request.notifyListenerResponseNotUsable();
        }
    }

    private void parseAndDeliverNetworkError(HanntoRequest<?> request, HanntoError error) {
        error = request.parseNetworkError(error);
        mDelivery.postError(request, error);
    }
}
