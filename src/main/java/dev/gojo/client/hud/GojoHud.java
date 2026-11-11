package dev.gojo.client.hud;

import dev.gojo.client.config.ClientLanguage;
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

    private static String tr(String portuguese, String english) { return ClientLanguage.isPortuguese() ? portuguese : english; }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        int x = 8;
        int y = 8;
        y = line(context, "GOJO CLIENT", x, y, 0xFF82B9FF);
        if (enabled("FPS")) y = line(context, tr("QUADROS  ", "FPS  ") + client.getCurrentFps(), x, y, 0xFFE8E8F4);
        if (enabled("HUD Coordinates") || enabled("Coordinates")) {
            double px = client.player.getX();
            double py = client.player.getY();
            double pz = client.player.getZ();
            y = line(context, String.format(tr("XYZ  %.1f  %.1f  %.1f", "XYZ  %.1f  %.1f  %.1f"), px, py, pz), x, y, 0xFFE8E8F4);
        }
        if (enabled("HUD Speed") || enabled("Speed")) {
            double speed = client.player.getVelocity().horizontalLength() * 20.0;
            y = line(context, String.format(tr("VELOCIDADE  %.2f m/s", "SPEED  %.2f m/s"), speed), x, y, 0xFFB9A0FF);
        }
        if (enabled("Ping")) {
            PlayerListEntry entry = client.getNetworkHandler() == null ? null
                    : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            y = line(context, tr("LATÊNCIA  ", "PING  ") + (entry == null ? "--" : entry.getLatency() + " ms"), x, y, 0xFFE8E8F4);
        }
        if (enabled("Connection Monitor")) {
            PlayerListEntry entry = client.getNetworkHandler() == null ? null
                    : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            String connection = client.getCurrentServerEntry() == null ? "LOCAL" : "MULTIJOGADOR";
            String latency = entry == null ? "--" : entry.getLatency() + " ms";
            y = line(context, tr("CONEXÃO  ", "CONNECTION  ") + (client.getCurrentServerEntry() == null ? tr("LOCAL", "LOCAL") : tr("MULTIJOGADOR", "MULTIPLAYER")) + tr("  LATÊNCIA ", "  PING ") + latency, x, y, 0xFF9EDBFF);
        }
        if (enabled("Clock")) y = line(context, tr("HORA  ", "TIME  ") + LocalTime.now().format(CLOCK), x, y, 0xFFE8E8F4);
        if (enabled("Durability")) {
            var stack = client.player.getMainHandStack();
            if (!stack.isEmpty() && stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                y = line(context, tr("DURABILIDADE  ", "DURABILITY  ") + remaining + "/" + stack.getMaxDamage(), x, y, 0xFFFFD18B);
            }
        }
        if (enabled("Player Info")) {
            y = line(context, tr("JOGADOR  ", "PLAYER  ") + client.player.getName().getString()
                    + tr("  VIDA ", "  HP ") + String.format("%.1f", client.player.getHealth())
                    + tr("  FOME ", "  FOOD ") + client.player.getHungerManager().getFoodLevel(),
                    x, y, 0xFF9EDBFF);
        }
        if (enabled("Environment Info")) {
            String biome = client.world.getBiome(client.player.getBlockPos()).getKey()
                    .map(key -> key.getValue().toString()).orElse("unknown");
            y = line(context, tr("BIOMA  ", "BIOME  ") + trim(biome, 34)
                    + tr("  DIMENSÃO ", "  DIM ") + client.world.getRegistryKey().getValue(), x, y, 0xFF9EDBFF);
        }
        if (enabled("Entity Viewer")) {
            int players = 0;
            int mobs = 0;
            for (var entity : client.world.getEntities()) {
                if (entity == client.player || !entity.isAlive()) continue;
                if (entity instanceof net.minecraft.entity.player.PlayerEntity) players++;
                else if (entity instanceof net.minecraft.entity.LivingEntity) mobs++;
            }
            y = line(context, tr("ENTIDADES  JOGADORES ", "ENTITIES  PLAYERS ") + players + tr("  CRIATURAS ", "  MOBS ") + mobs, x, y, 0xFFB9A0FF);
        }
        if (enabled("Combat Info")) {
            String target = client.crosshairTarget instanceof EntityHitResult hit
                    ? trim(hit.getEntity().getName().getString(), 22)  : "nenhum";
            y = line(context, tr("COMBATE  ALVO ", "COMBAT  TARGET ") + target + "  VIDA " + String.format("%.1f", client.player.getHealth()),
                    x, y, 0xFFFFA7E8);
        }
        if (enabled("Block Indicator") && client.crosshairTarget instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            String block = client.world.getBlockState(blockHit.getBlockPos()).getBlock().getName().getString();
            y = line(context, tr("OLHANDO PARA  ", "LOOKING AT  ") + trim(block, 30), x, y, 0xFFFFD18B);
        }
        if (enabled("Server Info")) {
            String server = client.getCurrentServerEntry() == null ? "Um jogador" : client.getCurrentServerEntry().address;
            y = line(context, tr("SERVIDOR  ", "SERVER  ") + trim(server, 36), x, y, 0xFFB4B4C8);
        }
        if (enabled("Chest ESP")) y = renderBlockEntityEsp(context, client, y, true);
        if (enabled("Furnace ESP")) y = renderBlockEntityEsp(context, client, y, false);
        if (enabled("Block Info") && client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            line(context, tr("BLOCO  ", "BLOCK  ") + trim(client.world.getBlockState(pos).getBlock().getName().getString(), 28)
                    + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), x, y, 0xFFB4B4C8);
        }
        if (enabled("Target Indicator") && client.crosshairTarget instanceof EntityHitResult hit) {
            line(context, tr("ALVO  ", "TARGET  ") + trim(hit.getEntity().getName().getString(), 32), 8, context.getScaledWindowHeight() - 24, 0xFFFFA7E8);
        }
        if (enabled("Combat Visuals")) renderCombatVisuals(context, client);
        if (enabled("Tracers")) renderTracers(context, client);
        if (enabled("Reach Indicator") && client.crosshairTarget != null && client.crosshairTarget.getType() != HitResult.Type.MISS) {
            double reach = client.player.getEyePos().distanceTo(client.crosshairTarget.getPos());
            line(context, String.format(tr("ALCANCE  %.2f m", "REACH  %.2f m"), reach), 8, context.getScaledWindowHeight() - 38, 0xFF82B9FF);
        }
    }

    private static void renderCombatVisuals(DrawContext context, MinecraftClient client) {
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;
        var target = hit.getEntity();
        String name = trim(target.getName().getString(), 28);
        String health = target instanceof net.minecraft.entity.LivingEntity living
                ? String.format("%.1f HP", living.getHealth()) : "ENTIDADE";
        String distance = String.format("%.1f m", client.player.distanceTo(target));
        int centerX = context.getScaledWindowWidth() / 2;
        int top = context.getScaledWindowHeight() - 68;
        int left = centerX - 82;
        context.fill(left, top, left + 164, top + 48, 0xDD10101B);
        context.fill(left, top, left + 164, top + 2, 0xFF9A63FF);
        context.drawText(MinecraftClient.getInstance().textRenderer, name, left + 9, top + 8, 0xFFFFFFFF, true);
        context.drawText(MinecraftClient.getInstance().textRenderer, health, left + 9, top + 25, 0xFFFFA7E8, true);
        context.drawText(MinecraftClient.getInstance().textRenderer, distance, left + 112, top + 25, 0xFF9EDBFF, true);
    }


    private static void renderTracers(DrawContext context, MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        var player = client.player;
        List<net.minecraft.entity.LivingEntity> targets = new ArrayList<>(
                client.world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                        player.getBoundingBox().expand(48.0),
                        entity -> entity != player && entity.isAlive() && !entity.isInvisible()
                                && player.squaredDistanceTo(entity) <= 2304.0));
        targets.sort(Comparator.comparingDouble(player::squaredDistanceTo));

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int maxTargets = Math.min(12, targets.size());
        for (int i = 0; i < maxTargets; i++) {
            var target = targets.get(i);
            var delta = target.getEyePos().subtract(player.getEyePos());
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (horizontal < 0.001) continue;
            float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
            float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
            float yawDelta = net.minecraft.util.math.MathHelper.wrapDegrees(targetYaw - player.getYaw());
            float pitchDelta = targetPitch - player.getPitch();
            if (Math.abs(yawDelta) > 65.0F || Math.abs(pitchDelta) > 45.0F) continue;

            int endX = centerX + Math.round(yawDelta / 65.0F * Math.max(20, width / 2 - 18));
            int endY = centerY + Math.round(pitchDelta / 45.0F * Math.max(20, height / 2 - 18));
            int color = target instanceof net.minecraft.entity.player.PlayerEntity
                    ? 0xFFFF78C8 : 0xFF72B7FF;
            drawHudLine(context, centerX, centerY, endX, endY, color);
        }
    }

    private static void drawHudLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(1, Math.min(64, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int step = 0; step <= steps; step++) {
            float progress = step / (float) steps;
            int x = Math.round(x0 + (x1 - x0) * progress);
            int y = Math.round(y0 + (y1 - y0) * progress);
            context.fill(x, y, x + 2, y + 2, color);
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
        String title = chests ? tr("ESP DE BAÚS", "CHEST ESP") : tr("ESP DE FORNALHAS", "FURNACE ESP");
        int color = chests ? 0xFFFFC66D : 0xFFFF8A65;
        y = line(context, title + "  " + found.size(), 8, y, color);
        int shown = Math.min(5, found.size());
        for (int i = 0; i < shown; i++) {
            BlockPos pos = found.get(i).getPos();
            double distance = Math.sqrt(client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            y = line(context, String.format("%s  %d %d %d  %.0fm",
                    chests ? tr("BAÚ", "CHEST") : tr("FORNALHA", "FURNACE"), pos.getX(), pos.getY(), pos.getZ(), distance),
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
