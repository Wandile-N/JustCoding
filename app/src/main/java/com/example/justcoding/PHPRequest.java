package com.example.justcoding;

import android.app.Activity;
import android.content.ContentValues;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PHPRequest {
    OkHttpClient client = new OkHttpClient();
    String url;


    public PHPRequest(String prefix){
        url = prefix ;
    }

    public void doRequest(Activity act, String file, ContentValues params, RequestHandler rh){
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : params.keySet()){
            builder.add(key, params.getAsString(key));
        }

        Request request = new Request.Builder()
                .url(url+file+".php")
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("Error onFailure: ", e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Checking for the error using isSuccessful
                if (!response.isSuccessful()){
                    throw new IOException("Unexpected code onResponse: "+response);
                }

                // Read data from the worker thread
                final String responseData = response.body().string();

                //Run view-related code back on the main thread
                act.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        rh.processResponse(responseData);
                    }
                });
            }
        });

    }
}
