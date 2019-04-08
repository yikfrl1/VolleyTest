package com.hannto.io.toolbox;

import com.hannto.io.HanntoRequest;
import com.hannto.volleytest.MainActivity;

import java.io.IOException;

public class HanntoBasicStack implements HanntoStack {
    @Override
    public RawResponse executeRequest(HanntoRequest<?> request) throws IOException {
        if(request.getData().equals(MainActivity.FIRST_REQUEST)){
            return new RawResponse(MainActivity.FIRST_RESPONSE);
        }else if (request.getData().equals(MainActivity.SECOND_REQUEST)){
            return new RawResponse(MainActivity.SECOND_RESPONSE);
        }else if (request.getData().equals(MainActivity.THIRD_REQUEST)){
            return new RawResponse(MainActivity.THIRD_RESPONSE);
        }else{
            return new RawResponse(MainActivity.OTHER_RESPONSE);
        }
    }
}
