package com.mx.viewboard.client.keybind;

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

public final class ViewBoardConfigStore {
    private static final String FILE_NAME = "viewboard-client.json";

    private ViewBoardConfigStore() {
    }

    public static ViewBoardClientConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return new ViewBoardClientConfig();
        }

        try {
            return ViewBoardConfigCodec.fromJson(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | JsonParseException exception) {
            return new ViewBoardClientConfig();
        }
    }

    public static void save(ViewBoardClientConfig config) {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(ViewBoardConfigCodec.toJson(config));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save ViewBoard config", exception);
        }
    }

    public static String toJson(ViewBoardClientConfig config) {
        return ViewBoardConfigCodec.toJson(config);
    }

    public static ViewBoardClientConfig fromJson(String rawJson) {
        return ViewBoardConfigCodec.fromJson(rawJson);
    }

    private static Path configPath() {
        Path gameDirectory = Minecraft.getInstance().gameDirectory.toPath();
        return gameDirectory.resolve("config").resolve(FILE_NAME);
    }
}
