package com.thrifa.ruofei.bus_locator.api;

import com.thrifa.ruofei.bus_locator.pojo.BusInfo;
import com.thrifa.ruofei.bus_locator.pojo.BusStop;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by ruofei on 5/24/2016.
 */
public interface BusLocatorApi {
    @GET("GetBusStops/{name}")
    Call<List<BusStop>>getBusStop(@Path("name") String routeName);

    @GET("SubscribeBusCoordinate/{id}/{token}")
    Call<Void>subscribeBus(@Path("id") String busID,
                           @Path("token") String token);

    @GET("GetBusInfo/{id}")
    Call<BusInfo>getBusLocation(@Path("id") String busID);
}