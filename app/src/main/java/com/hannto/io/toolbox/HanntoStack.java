package com.hannto.io.toolbox;

import com.hannto.io.HanntoRequest;

import java.io.IOException;

public interface HanntoStack {

    RawResponse executeRequest(HanntoRequest<?> request) throws IOException;

}
