package dev.gojo.client.module;

import dev.gojo.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<GojoModule> MODULES = new ArrayList<>();
    private static boolean registered;
    private ModuleManager() {}

    public static void registerDefaults() {
        if (registered) return;
        registered = true;
        add("Reach Indicator", "Displays distance to the crosshair target.", ModuleCategory.COMBAT, true);
        add("Target Indicator", "Shows the entity currently under the crosshair.", ModuleCategory.COMBAT, true);
        add("Combat Info", "Displays compact combat context in the HUD.", ModuleCategory.COMBAT, true);
        add("Combat Visuals", "Visual combat overlays; no attack automation.", ModuleCategory.COMBAT, false);
        add("Auto Sprint", "Sprints while moving forward, when allowed by the player state.", ModuleCategory.MOVEMENT, true);
        add("Movement Control", "Movement status display and local controls.", ModuleCategory.MOVEMENT, false);
        add("Speed", "Displays horizontal movement speed.", ModuleCategory.MOVEMENT, true);
        add("Fullbright", "Client-side brightness override.", ModuleCategory.RENDER, false);
        add("Entity ESP", "Entity outlines for permitted/private worlds.", ModuleCategory.RENDER, false);
        add("Tracers", "Entity direction lines for permitted/private worlds.", ModuleCategory.RENDER, false);
        add("Nametags", "Enhanced entity labels for permitted/private worlds.", ModuleCategory.RENDER, false);
        add("Block Indicator", "Displays the block under the crosshair.", ModuleCategory.RENDER, true);
        add("Lighting", "Client-side lighting presentation controls.", ModuleCategory.RENDER, false);
        add("Auto Tool", "Selects a suitable hotbar tool for the targeted block.", ModuleCategory.PLAYER, false);
        add("Auto Respawn", "Automatically respawns after death.", ModuleCategory.PLAYER, true);
        add("Coordinates", "Displays current XYZ coordinates.", ModuleCategory.PLAYER, true);
        add("Player Info", "Displays player and dimension information.", ModuleCategory.PLAYER, true);
        add("Inventory Manager", "Inventory organization helpers.", ModuleCategory.PLAYER, false);
        add("Block Info", "Displays targeted block state and position.", ModuleCategory.WORLD, true);
        add("Structure Finder", "Visual structure discovery; no server scanning.", ModuleCategory.WORLD, false);
        add("Entity Viewer", "Displays nearby entity counts.", ModuleCategory.WORLD, true);
        add("Environment Info", "Displays biome and dimension context.", ModuleCategory.WORLD, true);
        add("FPS", "Displays current frames per second.", ModuleCategory.HUD, true);
        add("Ping", "Displays connection latency when available.", ModuleCategory.HUD, true);
        add("HUD Coordinates", "Coordinates overlay.", ModuleCategory.HUD, true);
        add("HUD Speed", "Movement speed overlay.", ModuleCategory.HUD, true);
        add("Durability", "Displays equipped item durability.", ModuleCategory.HUD, true);
        add("Clock", "Displays local system time.", ModuleCategory.HUD, true);
        add("Server Info", "Displays server and connection details.", ModuleCategory.HUD, true);
        ConfigManager.apply(MODULES);
    }

    private static void add(String name, String description, ModuleCategory category, boolean implemented) {
        GojoModule module = new GojoModule(name, description, category, implemented);
        if (implemented && (name.equals("FPS") || name.equals("HUD Coordinates") || name.equals("Clock"))) {
            module.setEnabled(true);
        }
        MODULES.add(module);
    }

    public static List<GojoModule> all() { return Collections.unmodifiableList(MODULES); }
    public static GojoModule find(String name) { return MODULES.stream().filter(m -> m.name().equals(name)).findFirst().orElse(null); }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        GojoModule sprint = find("Auto Sprint");
        if (sprint != null && sprint.enabled() && client.options.forwardKey.isPressed()
                && !player.isSneaking() && player.getHungerManager().getFoodLevel() > 6) {
            player.setSprinting(true);
        }
        GojoModule respawn = find("Auto Respawn");
        if (respawn != null && respawn.enabled() && player.isDead()) player.requestRespawn();
    }
}
