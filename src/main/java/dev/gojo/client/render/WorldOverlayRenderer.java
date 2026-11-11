package dev.gojo.client.render;

import dev.gojo.client.module.GojoModule;
import dev.gojo.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WorldOverlayRenderer {
    private static final List<BlockPos> ORE_CACHE = new ArrayList<>();
    private static long lastOreScanNanos;
    private static final int MAX_ORE_MARKERS = 96;

    private WorldOverlayRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldOverlayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        boolean playerBoxes = enabled("Player Box ESP");
        boolean xray = enabled("X-Ray");
        if (!playerBoxes && !xray) return;

        var matrices = context.matrices();
        var consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        var camera = client.gameRenderer.getCamera().getCameraPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        var lines = consumers.getBuffer(RenderLayers.LINES);

        if (playerBoxes) {
            PlayerEntity self = client.player;
            for (PlayerEntity target : client.world.getPlayers()) {
                if (target == self || !target.isAlive() || target.isSpectator()
                        || self.squaredDistanceTo(target) > 4096.0) continue;
                int color = target.isTeammate(self) ? 0xFF65D6A5 : 0xFFFF70B8;
                VertexRendering.drawOutline(matrices, lines,
                        VoxelShapes.cuboid(target.getBoundingBox()),
                        0.0, 0.0, 0.0, color, 2.0F);
            }
        }

        if (xray) {
            refreshOreCache(client);
            for (BlockPos pos : ORE_CACHE) {
                BlockState state = client.world.getBlockState(pos);
                if (state.isAir()) continue;
                int color = oreColor(state);
                VertexRendering.drawOutline(matrices, lines, VoxelShapes.fullCube(),
                        pos.getX(), pos.getY(), pos.getZ(), color, 2.0F);
            }
        }

        matrices.pop();
    }

    private static void refreshOreCache(MinecraftClient client) {
        long now = System.nanoTime();
        if (now - lastOreScanNanos < 500_000_000L) return;
        lastOreScanNanos = now;
        ORE_CACHE.clear();

        BlockPos center = client.player.getBlockPos();
        List<BlockPos> found = new ArrayList<>();
        for (int x = center.getX() - 12; x <= center.getX() + 12; x++) {
            for (int y = center.getY() - 8; y <= center.getY() + 8; y++) {
                for (int z = center.getZ() - 12; z <= center.getZ() + 12; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isValuableOre(client.world.getBlockState(pos))) found.add(pos.toImmutable());
                }
            }
        }
        found.sort(Comparator.comparingDouble(pos -> client.player.squaredDistanceTo(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        ORE_CACHE.addAll(found.subList(0, Math.min(MAX_ORE_MARKERS, found.size())));
    }

    private static boolean isValuableOre(BlockState state) {
        return state.isOf(Blocks.COAL_ORE) || state.isOf(Blocks.DEEPSLATE_COAL_ORE)
                || state.isOf(Blocks.IRON_ORE) || state.isOf(Blocks.DEEPSLATE_IRON_ORE)
                || state.isOf(Blocks.COPPER_ORE) || state.isOf(Blocks.DEEPSLATE_COPPER_ORE)
                || state.isOf(Blocks.GOLD_ORE) || state.isOf(Blocks.DEEPSLATE_GOLD_ORE)
                || state.isOf(Blocks.REDSTONE_ORE) || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.isOf(Blocks.LAPIS_ORE) || state.isOf(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.isOf(Blocks.EMERALD_ORE) || state.isOf(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.isOf(Blocks.NETHER_QUARTZ_ORE) || state.isOf(Blocks.NETHER_GOLD_ORE)
                || state.isOf(Blocks.ANCIENT_DEBRIS);
    }

    private static int oreColor(BlockState state) {
        if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) return 0xFF55E8FF;
        if (state.isOf(Blocks.EMERALD_ORE) || state.isOf(Blocks.DEEPSLATE_EMERALD_ORE)) return 0xFF62F28A;
        if (state.isOf(Blocks.GOLD_ORE) || state.isOf(Blocks.DEEPSLATE_GOLD_ORE)
                || state.isOf(Blocks.NETHER_GOLD_ORE)) return 0xFFFFD45C;
        if (state.isOf(Blocks.ANCIENT_DEBRIS)) return 0xFFFF8B63;
        if (state.isOf(Blocks.REDSTONE_ORE) || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)) return 0xFFFF5555;
        if (state.isOf(Blocks.LAPIS_ORE) || state.isOf(Blocks.DEEPSLATE_LAPIS_ORE)) return 0xFF668CFF;
        if (state.isOf(Blocks.NETHER_QUARTZ_ORE)) return 0xFFFFE5D2;
        return 0xFFB7C3D8;
    }

    private static boolean enabled(String name) {
        GojoModule module = ModuleManager.find(name);
        return module != null && module.enabled();
    }
}
