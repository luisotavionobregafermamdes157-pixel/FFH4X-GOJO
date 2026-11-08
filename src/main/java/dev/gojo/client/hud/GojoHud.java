package dev.gojo.client.hud;

import dev.gojo.client.module.GojoModule;
import dev.gojo.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GojoHud {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private GojoHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        int x = 8;
        int y = 8;
        y = line(context, "GOJO CLIENT", x, y, 0xFF82B9FF);
        if (enabled("FPS")) y = line(context, "FPS  " + client.getCurrentFps(), x, y, 0xFFE8E8F4);
        if (enabled("HUD Coordinates") || enabled("Coordinates")) {
            double px = client.player.getX();
            double py = client.player.getY();
            double pz = client.player.getZ();
            y = line(context, String.format("XYZ  %.1f  %.1f  %.1f", px, py, pz), x, y, 0xFFE8E8F4);
        }
        if (enabled("HUD Speed") || enabled("Speed")) {
            double speed = client.player.getVelocity().horizontalLength() * 20.0;
            y = line(context, String.format("SPEED  %.2f m/s", speed), x, y, 0xFFB9A0FF);
        }
        if (enabled("Ping")) {
            PlayerListEntry entry = client.getNetworkHandler() == null ? null
                    : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            y = line(context, "PING  " + (entry == null ? "--" : entry.getLatency() + " ms"), x, y, 0xFFE8E8F4);
        }
        if (enabled("Clock")) y = line(context, "TIME  " + LocalTime.now().format(CLOCK), x, y, 0xFFE8E8F4);
        if (enabled("Durability")) {
            var stack = client.player.getMainHandStack();
            if (!stack.isEmpty() && stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                y = line(context, "DURABILITY  " + remaining + "/" + stack.getMaxDamage(), x, y, 0xFFFFD18B);
            }
        }
        if (enabled("Server Info")) {
            String server = client.getCurrentServerEntry() == null ? "Singleplayer" : client.getCurrentServerEntry().address;
            y = line(context, "SERVER  " + trim(server, 36), x, y, 0xFFB4B4C8);
        }
        if (enabled("Chest ESP")) y = renderBlockEntityEsp(context, client, y, true);
        if (enabled("Furnace ESP")) y = renderBlockEntityEsp(context, client, y, false);
        if (enabled("Block Info") && client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            line(context, "BLOCK  " + trim(client.world.getBlockState(pos).getBlock().getName().getString(), 28)
                    + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), x, y, 0xFFB4B4C8);
        }
        if (enabled("Target Indicator") && client.crosshairTarget instanceof EntityHitResult hit) {
            line(context, "TARGET  " + trim(hit.getEntity().getName().getString(), 32), 8, context.getScaledWindowHeight() - 24, 0xFFFFA7E8);
        }
        if (enabled("Reach Indicator") && client.crosshairTarget != null && client.crosshairTarget.getType() != HitResult.Type.MISS) {
            double reach = client.player.getEyePos().distanceTo(client.crosshairTarget.getPos());
            line(context, String.format("REACH  %.2f m", reach), 8, context.getScaledWindowHeight() - 38, 0xFF82B9FF);
        }
    }

    private static int renderBlockEntityEsp(DrawContext context, MinecraftClient client, int y, boolean chests) {
        List<BlockEntity> found = new ArrayList<>();
        for (BlockEntity entity : client.world.getBlockEntities()) {
            BlockPos pos = entity.getPos();
            double distance = Math.sqrt(client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            if (distance > 64.0) continue;
            var block = entity.getCachedState().getBlock();
            boolean match = chests
                    ? block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST || block == Blocks.BARREL
                    : block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER;
            if (match) found.add(entity);
        }
        found.sort(Comparator.comparingDouble(entity -> client.player.squaredDistanceTo(
                entity.getPos().getX() + 0.5, entity.getPos().getY() + 0.5, entity.getPos().getZ() + 0.5)));
        String title = chests ? "CHEST ESP" : "FURNACE ESP";
        int color = chests ? 0xFFFFC66D : 0xFFFF8A65;
        y = line(context, title + "  " + found.size(), 8, y, color);
        int shown = Math.min(5, found.size());
        for (int i = 0; i < shown; i++) {
            BlockPos pos = found.get(i).getPos();
            double distance = Math.sqrt(client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            y = line(context, String.format("%s  %d %d %d  %.0fm",
                    chests ? "CHEST" : "FURNACE", pos.getX(), pos.getY(), pos.getZ(), distance),
                    8, y, 0xFFE8E8F4);
        }
        return y;
    }

    private static boolean enabled(String name) {
        GojoModule module = ModuleManager.find(name);
        return module != null && module.enabled();
    }

    private static int line(DrawContext context, String value, int x, int y, int color) {
        int width = MinecraftClient.getInstance().textRenderer.getWidth(value) + 12;
        context.fill(x - 4, y - 3, x + width, y + 12, 0x990D0D18);
        context.fill(x - 4, y - 3, x - 2, y + 12, color);
        context.drawText(MinecraftClient.getInstance().textRenderer, value, x, y, color, true);
        return y + 16;
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
