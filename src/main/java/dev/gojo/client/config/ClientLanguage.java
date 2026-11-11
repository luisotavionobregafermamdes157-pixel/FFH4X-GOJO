package dev.gojo.client.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientLanguage {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("gojo-client-language.txt");
    private static boolean portuguese = true;

    private ClientLanguage() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                portuguese = Files.readString(FILE).trim().equalsIgnoreCase("pt");
            } else {
                save();
            }
        } catch (IOException ignored) {
            portuguese = true;
        }
    }

    public static boolean isPortuguese() {
        return portuguese;
    }

    public static void toggle() {
        portuguese = !portuguese;
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, portuguese ? "pt" : "en");
        } catch (IOException ignored) {
        }
    }
}
