package com.hannto.io.toolbox;

public class RawResponse {

    private final byte[] mData;

    public RawResponse(byte[] mData) {
        this.mData = mData;
    }

    public byte[] getmData() {
        return mData;
    }
}
