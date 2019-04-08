package com.hannto.io;

import android.os.Handler;

import java.util.concurrent.Executor;

public class HanntoExecutorDelivery implements HanntoResponseDelivery {
    /** Used for posting responses, typically to the main thread. */
    private final Executor mResponsePoster;

    /**
     * Creates a new response delivery interface.
     *
     * @param handler {@link Handler} to post responses on
     */
    public HanntoExecutorDelivery(final Handler handler) {
        // Make an Executor that just wraps the handler.
        mResponsePoster =
                new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        handler.post(command);
                    }
                };
    }

    /**
     * Creates a new response delivery interface, mockable version for testing.
     *
     * @param executor For running delivery tasks
     */
    public HanntoExecutorDelivery(Executor executor) {
        mResponsePoster = executor;
    }

    @Override
    public void postResponse(HanntoRequest<?> request, HanntoResponse<?> response) {
        postResponse(request, response, null);
    }

    @Override
    public void postResponse(HanntoRequest<?> request, HanntoResponse<?> response, Runnable runnable) {
        request.markDelivered();
//        request.addMarker("post-response");
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, runnable));
    }

    @Override
    public void postError(HanntoRequest<?> request, HanntoError error) {
//        request.addMarker("post-error");
        HanntoResponse<?> response = HanntoResponse.error(error);
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, null));
    }

    /** A Runnable used for delivering network responses to a listener on the main thread. */
    @SuppressWarnings("rawtypes")
    private static class ResponseDeliveryRunnable implements Runnable {
        private final HanntoRequest mRequest;
        private final HanntoResponse mResponse;
        private final Runnable mRunnable;

        public ResponseDeliveryRunnable(HanntoRequest request, HanntoResponse response, Runnable runnable) {
            mRequest = request;
            mResponse = response;
            mRunnable = runnable;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            // NOTE: If cancel() is called off the thread that we're currently running in (by
            // default, the main thread), we cannot guarantee that deliverResponse()/deliverError()
            // won't be called, since it may be canceled after we check isCanceled() but before we
            // deliver the response. Apps concerned about this guarantee must either call cancel()
            // from the same thread or implement their own guarantee about not invoking their
            // listener after cancel() has been called.

            // If this request has canceled, finish it and don't deliver.
            if (mRequest.isCanceled()) {
                mRequest.finish("canceled-at-delivery");
                return;
            }

            // Deliver a normal response or error, depending.
            if (mResponse.isSuccess()) {
                mRequest.deliverResponse(mResponse.result);
            } else {
                mRequest.deliverError(mResponse.error);
            }

            // If this is an intermediate response, add a marker, otherwise we're done
            // and the request can be finished.
            if (mResponse.intermediate) {
//                mRequest.addMarker("intermediate-response");
            } else {
                mRequest.finish("done");
            }

            // If we have been provided a post-delivery runnable, run it.
            if (mRunnable != null) {
                mRunnable.run();
            }
        }
    }
}
