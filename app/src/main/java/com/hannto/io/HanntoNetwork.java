package com.hannto.io;

public interface HanntoNetwork {

    /**
     * Performs the specified request.
     *
     * @param request Request to process
     * @return A {@link HanntoNetworkResponse} with data and caching metadata; will never be null
     * @throws HanntoError on errors
     */
    HanntoNetworkResponse performRequest(HanntoRequest<?> request) throws HanntoError;

}
