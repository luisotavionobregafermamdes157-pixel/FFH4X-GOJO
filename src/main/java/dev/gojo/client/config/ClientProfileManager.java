package dev.gojo.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.gojo.client.GojoClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ClientProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("gojo-client").resolve("player-data.json");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static JsonObject profiles = new JsonObject();
    private static String activeKey = "default";
    private static String activeName = "Player";

    private ClientProfileManager() {}

    /**
     * Loads the local profile for this Minecraft account.
     * Returns true only when this account has already completed the first login.
     */
    public static synchronized boolean initialize(String playerName) {
        activeName = playerName == null || playerName.isBlank() ? "Player" : playerName;
        activeKey = activeName.toLowerCase(Locale.ROOT);
        load();

        JsonObject profile = getActiveProfile();
        boolean alreadyAuthenticated = profile.has("authenticated")
                && profile.get("authenticated").getAsBoolean();
        if (alreadyAuthenticated) {
            profile.addProperty("loginCount", getLoginCount() + 1);
            profile.addProperty("lastLogin", LocalDateTime.now().format(DATE_FORMAT));
            profile.addProperty("playerName", activeName);
            save();
        }
        return alreadyAuthenticated;
    }

    public static synchronized void markAuthenticated() {
        JsonObject profile = getActiveProfile();
        profile.addProperty("authenticated", true);
        profile.addProperty("loginCount", getLoginCount() + 1);
        profile.addProperty("lastLogin", LocalDateTime.now().format(DATE_FORMAT));
        profile.addProperty("playerName", activeName);
        save();
    }

    public static synchronized int getLoginCount() {
        JsonObject profile = getActiveProfile();
        return profile.has("loginCount") ? Math.max(0, profile.get("loginCount").getAsInt()) : 0;
    }

    public static synchronized String getPlayerName() {
        return activeName;
    }

    public static synchronized String getLastLogin() {
        JsonObject profile = getActiveProfile();
        return profile.has("lastLogin") ? profile.get("lastLogin").getAsString() : "Never";
    }

    private static JsonObject getActiveProfile() {
        if (!profiles.has(activeKey) || !profiles.get(activeKey).isJsonObject()) {
            profiles.add(activeKey, new JsonObject());
        }
        return profiles.getAsJsonObject(activeKey);
    }

    private static void load() {
        if (!Files.exists(FILE)) {
            profiles = new JsonObject();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            profiles = root.has("profiles") && root.get("profiles").isJsonObject()
                    ? root.getAsJsonObject("profiles") : new JsonObject();
        } catch (Exception exception) {
            profiles = new JsonObject();
            GojoClient.LOGGER.error("Could not read GOJO CLIENT player data", exception);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            root.add("profiles", profiles);
            Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            GojoClient.LOGGER.error("Could not save GOJO CLIENT player data", exception);
        }
    }
}
