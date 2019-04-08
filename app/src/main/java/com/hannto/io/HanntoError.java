package com.hannto.io;

public class HanntoError extends Exception {
    public final HanntoNetworkResponse networkResponse;
    private long networkTimeMs;

    public HanntoError() {
        networkResponse = null;
    }

    public HanntoError(HanntoNetworkResponse response) {
        networkResponse = response;
    }

    public HanntoError(String exceptionMessage) {
        super(exceptionMessage);
        networkResponse = null;
    }

    public HanntoError(String exceptionMessage, Throwable reason) {
        super(exceptionMessage, reason);
        networkResponse = null;
    }

    public HanntoError(Throwable cause) {
        super(cause);
        networkResponse = null;
    }

    /* package */ void setNetworkTimeMs(long networkTimeMs) {
        this.networkTimeMs = networkTimeMs;
    }

    public long getNetworkTimeMs() {
        return networkTimeMs;
    }
}
