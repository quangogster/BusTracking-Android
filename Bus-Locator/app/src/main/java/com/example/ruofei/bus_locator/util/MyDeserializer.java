package com.example.ruofei.bus_locator.util;

import com.example.ruofei.bus_locator.TrackedBus;
import com.example.ruofei.bus_locator.pojo.BusTracker;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.json.JSONArray;

import java.lang.reflect.Type;
import java.util.List;

public class MyDeserializer<T> implements JsonDeserializer<T>
{
    @Override
    public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc)
            throws JsonParseException
    {
        // Get the "content" element from the parsed JSON
//        JsonElement content = je.getAsJsonObject().get("content");
        JsonArray content = je.getAsJsonArray();

        // Deserialize it. You use a new instance of Gson to avoid infinite recursion
        // to this deserializer
//        return new Gson().fromJson(content, type);
        return new Gson().fromJson(content, type);
    }
}
