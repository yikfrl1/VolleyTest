package com.hannto.io;

public class HanntoResponse<T> {

    /** Callback interface for delivering parsed responses. */
    public interface Listener<T> {
        /** Called when a response is received. */
        void onResponse(T response);
    }

    /** Callback interface for delivering error responses. */
    public interface ErrorListener {
        /**
         * Callback method that an error has been occurred with the provided error code and optional
         * user-readable message.
         */
        void onErrorResponse(HanntoError error);
    }

    /** Returns a successful response containing the parsed result. */
    public static <T> HanntoResponse<T> success(T result) {
        return new HanntoResponse<>(result);
    }

    /**
     * Returns a failed response containing the given error code and an optional localized message
     * displayed to the user.
     */
    public static <T> HanntoResponse<T> error(HanntoError error) {
        return new HanntoResponse<>(error);
    }

    /** Parsed response, or null in the case of error. */
    public final T result;

//    /** Cache metadata for this response, or null in the case of error. */
//    public final Cache.Entry cacheEntry;

    /** Detailed error information if <code>errorCode != OK</code>. */
    public final HanntoError error;

    /** True if this response was a soft-expired one and a second one MAY be coming. */
    public boolean intermediate = false;

    /** Returns whether this response is considered successful. */
    public boolean isSuccess() {
        return error == null;
    }

    private HanntoResponse(T result) {
        this.result = result;
//        this.cacheEntry = cacheEntry;
        this.error = null;
    }

    private HanntoResponse(HanntoError error) {
        this.result = null;
//        this.cacheEntry = null;
        this.error = error;
    }

}
