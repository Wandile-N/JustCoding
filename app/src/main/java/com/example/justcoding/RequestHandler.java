package com.example.justcoding;

import java.util.Map;

public abstract interface RequestHandler {
    public void processResponse(String response);

    Map<String, String> getParams();
}
