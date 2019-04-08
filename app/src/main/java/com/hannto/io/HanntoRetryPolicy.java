package com.hannto.io;

public interface HanntoRetryPolicy {
    /** Returns the current timeout (used for logging). */
    int getCurrentTimeout();

    /** Returns the current retry count (used for logging). */
    int getCurrentRetryCount();

    /**
     * Prepares for the next retry by applying a backoff to the timeout.
     *
     * @param error The error code of the last attempt.
     * @throws HanntoError In the event that the retry could not be performed (for example if we ran
     *     out of attempts), the passed in error is thrown.
     */
    void retry(HanntoError error) throws HanntoError;
}
