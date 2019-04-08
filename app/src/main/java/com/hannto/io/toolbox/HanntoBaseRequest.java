package com.hannto.io.toolbox;

import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;

import com.hannto.io.HanntoNetworkResponse;
import com.hannto.io.HanntoRequest;
import com.hannto.io.HanntoResponse;

public class HanntoBaseRequest extends HanntoRequest<byte[]> {

    /** Lock to guard mListener as it is cleared on cancel() and read on delivery. */
    private final Object mLock = new Object();

    @Nullable
    @GuardedBy("mLock")
    private HanntoResponse.Listener<byte[]> mListener;

    public HanntoBaseRequest(byte[] data, HanntoResponse.Listener<byte[]> listener, @Nullable HanntoResponse.ErrorListener errorListener) {
        super(data, errorListener);
        mListener = listener;
    }

    @Override
    public void cancel() {
        super.cancel();
        synchronized (mLock) {
            mListener = null;
        }
    }

    @Override
    protected void deliverResponse(byte[] response) {
        HanntoResponse.Listener<byte[]> listener;
        synchronized (mLock) {
            listener = mListener;
        }
        if (listener != null) {
            listener.onResponse(response);
        }
    }

    @Override
    protected HanntoResponse parseNetworkResponse(HanntoNetworkResponse response) {
        return HanntoResponse.success(response.data);
    }

}
