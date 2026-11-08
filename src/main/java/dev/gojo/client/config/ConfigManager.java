package dev.gojo.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.gojo.client.GojoClient;
import dev.gojo.client.module.GojoModule;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("gojo-client");
    private static final Path CONFIG = DIRECTORY.resolve("config.json");
    private static JsonObject cached = new JsonObject();
    private static JsonObject cachedSettings = new JsonObject();
    private ConfigManager() {}

    public static void load() {
        if (!Files.exists(CONFIG)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(CONFIG, StandardCharsets.UTF_8)).getAsJsonObject();
            cached = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            cachedSettings = root.has("settings") ? root.getAsJsonObject("settings") : new JsonObject();
        } catch (Exception exception) {
            GojoClient.LOGGER.error("Could not read GOJO CLIENT configuration; defaults will be used", exception);
        }
    }

    public static void apply(List<GojoModule> modules) {
        for (GojoModule module : modules) {
            if (cached.has(module.name()) && cached.get(module.name()).isJsonPrimitive()) {
                module.setEnabled(cached.get(module.name()).getAsBoolean());
            }
            if (cachedSettings.has(module.name()) && cachedSettings.get(module.name()).isJsonPrimitive()) {
                module.setValue(cachedSettings.get(module.name()).getAsInt());
            }
        }
    }

    public static void save(List<GojoModule> modules) {
        try {
            Files.createDirectories(DIRECTORY);
            JsonObject root = new JsonObject();
            root.addProperty("profile", "default");
            JsonObject state = new JsonObject();
            JsonObject settings = new JsonObject();
            for (GojoModule module : modules) {
                state.addProperty(module.name(), module.enabled());
                settings.addProperty(module.name(), module.value());
            }
            root.add("modules", state);
            root.add("settings", settings);
            Files.writeString(CONFIG, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            GojoClient.LOGGER.error("Could not save GOJO CLIENT configuration", exception);
        }
    }

    public static void saveProfile(String profile, List<GojoModule> modules) {
        try {
            Files.createDirectories(DIRECTORY);
            JsonObject root = new JsonObject();
            root.addProperty("profile", profile.replaceAll("[^a-zA-Z0-9_-]", "_"));
            JsonObject state = new JsonObject();
            JsonObject settings = new JsonObject();
            for (GojoModule module : modules) {
                state.addProperty(module.name(), module.enabled());
                settings.addProperty(module.name(), module.value());
            }
            root.add("modules", state);
            root.add("settings", settings);
            Path target = DIRECTORY.resolve(root.get("profile").getAsString() + ".json");
            Files.writeString(target, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            GojoClient.LOGGER.error("Could not save GOJO CLIENT profile", exception);
        }
    }
}
