package com.hannto.io;

public interface HanntoResponseDelivery {

    /** Parses a response from the network or cache and delivers it. */
    void postResponse(HanntoRequest<?> request, HanntoResponse<?> response);

    /**
     * Parses a response from the network or cache and delivers it. The provided Runnable will be
     * executed after delivery.
     */
    void postResponse(HanntoRequest<?> request, HanntoResponse<?> response, Runnable runnable);

    /** Posts an error for the given request. */
    void postError(HanntoRequest<?> request, HanntoError error);
}
