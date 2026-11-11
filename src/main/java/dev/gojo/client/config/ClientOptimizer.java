package dev.gojo.client.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Applies a conservative client-side performance preset.
 * These settings can be changed again from Minecraft's Video Settings.
 */
public final class ClientOptimizer {
    private ClientOptimizer() {}

    public static void apply() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        var options = client.options;
        options.applyGraphicsMode(GraphicsMode.FAST);
        options.getParticles().setValue(ParticlesMode.MINIMAL);
        options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        options.getEntityShadows().setValue(false);
        options.getBiomeBlendRadius().setValue(0);
        options.getViewDistance().setValue(Math.min(options.getViewDistance().getValue(), 8));
        options.getSimulationDistance().setValue(Math.min(options.getSimulationDistance().getValue(), 5));
        options.getEntityDistanceScaling().setValue(Math.min(options.getEntityDistanceScaling().getValue(), 0.75));
        options.write();
    }
}
