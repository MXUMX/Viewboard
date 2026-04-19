package com.mx.viewboard.client.keybind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ViewBoardConfigCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ViewBoardConfigCodec() {
    }

    public static String toJson(ViewBoardClientConfig config) {
        return GSON.toJson(config);
    }

    public static ViewBoardClientConfig fromJson(String rawJson) {
        ViewBoardClientConfig config = GSON.fromJson(rawJson, ViewBoardClientConfig.class);
        return config != null ? config : new ViewBoardClientConfig();
    }
}
