package com.example.sivaram.cranes.Common;

import com.example.sivaram.cranes.Remote.IGoogleApi;
import com.example.sivaram.cranes.Remote.RetrofitClient;

public class Common {
    public static final String baseURL="https://maps.googleapis.com";
    public static IGoogleApi getGoogleAPI()
    {
        return RetrofitClient.getClient(baseURL).create(IGoogleApi.class);
    }
}
