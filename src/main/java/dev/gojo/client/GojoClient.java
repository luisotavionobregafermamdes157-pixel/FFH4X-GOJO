package dev.gojo.client;

import dev.gojo.client.config.ConfigManager;
import dev.gojo.client.gui.GojoScreen;
import dev.gojo.client.gui.LoginScreen;
import dev.gojo.client.hud.GojoHud;
import dev.gojo.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GojoClient implements ClientModInitializer {
    public static final String MOD_ID = "gojo-client";
    public static final Logger LOGGER = LoggerFactory.getLogger("GOJO CLIENT");
    private static KeyBinding openMenu;
    private static boolean authenticated;
    private static final String CLIENT_PASSWORD = "gojocraft";

    public static boolean authenticate(String password) {
        if (CLIENT_PASSWORD.equals(password)) {
            authenticated = true;
            return true;
        }
        return false;
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        ModuleManager.registerDefaults();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gojo-client.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!authenticated) {
                if (!(client.currentScreen instanceof LoginScreen)) client.setScreen(new LoginScreen());
                return;
            }
            while (openMenu.wasPressed()) {
                // Toggle the menu even from the title or pause screen.
                if (client.currentScreen instanceof GojoScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new GojoScreen());
                }
            }
            ModuleManager.tick(client);
        });

        GojoHud.register();
        LOGGER.info("GOJO CLIENT {} initialized", "1.0.0");
    }
}
