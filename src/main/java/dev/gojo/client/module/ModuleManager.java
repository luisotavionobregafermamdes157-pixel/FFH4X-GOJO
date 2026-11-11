package dev.gojo.client.module;

import dev.gojo.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModuleManager {
    private static final List<GojoModule> MODULES = new ArrayList<>();
    private static final Map<String, GojoModule> BY_NAME = new LinkedHashMap<>();
    private static final Map<Entity, Boolean> PREVIOUS_GLOW = new HashMap<>();
    private static final Set<Entity> MODIFIED_HITBOXES = new HashSet<>();
    private static boolean registered;
    private static long nextAutoClickNanos;
    private static long nextMobKillAuraNanos;
    private static long nextPlayerKillAuraNanos;
    private static double previousGamma = -1.0;
    private static int autoEatOriginalSlot = -1;
    private static boolean autoEatStarted;
    private ModuleManager() {}

    public static void registerDefaults() {
        if (registered) return;
        registered = true;
        add("Reach Indicator", "Displays distance to the crosshair target.", ModuleCategory.COMBAT, true);
        add("Target Indicator", "Shows the entity currently under the crosshair.", ModuleCategory.COMBAT, true);
        add("Combat Info", "Displays compact combat context in the HUD.", ModuleCategory.COMBAT, true);
        add("KillAura", "Automatically attacks visible players in a short range.", ModuleCategory.COMBAT, true);
        add("Mob KillAura", "Automatically attacks visible hostile and passive mobs in range.", ModuleCategory.COMBAT, true);
        add("Hitbox", "Expands nearby player and mob hitboxes (client-side targeting).", ModuleCategory.COMBAT, true);
        add("Auto Click", "Repeats the attack input while the left mouse button is held (1-20 CPS).", ModuleCategory.COMBAT, true);
        add("Aimbot", "Smoothly aims at visible players inside the adjustable FOV.", ModuleCategory.COMBAT, true);
        add("Combat Visuals", "Shows a target card with health and distance.", ModuleCategory.COMBAT, true);
        add("Auto Sprint", "Sprints while moving forward, when allowed by the player state.", ModuleCategory.MOVEMENT, true);
        add("Auto Eat", "Uses food from your hotbar when hunger is low.", ModuleCategory.PLAYER, true);
        add("NoFall", "Resets local fall distance while enabled.", ModuleCategory.MOVEMENT, true);
        add("NoSlow", "Applies a local movement compensation while using items; server rules still apply.", ModuleCategory.MOVEMENT, true);
        add("Step Assist", "Automatically jumps when moving into a low obstacle.", ModuleCategory.MOVEMENT, true);
        add("Movement Control", "Movement status display and local controls.", ModuleCategory.MOVEMENT, false);
        add("Speed", "Adjusts horizontal movement speed (1-100).", ModuleCategory.MOVEMENT, true);
        add("Scaffold", "Places blocks beneath you while bridging, when a valid support face exists.", ModuleCategory.MOVEMENT, true);
        add("Fullbright", "Sets client gamma to maximum brightness.", ModuleCategory.RENDER, true);
        add("Entity ESP", "Highlights nearby living entities through walls.", ModuleCategory.RENDER, true);
        add("Player ESP", "Highlights nearby player entities through walls.", ModuleCategory.RENDER, true);
        add("Player Box ESP", "Draws a 3D outline around nearby player hitboxes.", ModuleCategory.RENDER, true);
        add("X-Ray", "Highlights valuable ore blocks in loaded chunks with an on-screen toggle.", ModuleCategory.RENDER, true);
        add("Chest ESP", "Shows nearby chests, trapped chests, ender chests and barrels with distance and coordinates.", ModuleCategory.RENDER, true);
        add("Furnace ESP", "Shows nearby furnaces, blast furnaces and smokers with distance and coordinates.", ModuleCategory.RENDER, true);
        add("Tracers", "Draws on-screen direction lines to nearby living entities.", ModuleCategory.RENDER, true);
        add("Nametags", "Enhanced entity labels for permitted/private worlds.", ModuleCategory.RENDER, false);
        add("Block Indicator", "Displays the block under the crosshair.", ModuleCategory.RENDER, true);
        add("Lighting", "Client-side lighting presentation controls.", ModuleCategory.RENDER, false);
        add("Auto Tool", "Selects the fastest tool in your hotbar for the targeted block.", ModuleCategory.PLAYER, true);
        add("Auto Respawn", "Automatically respawns after death.", ModuleCategory.PLAYER, true);
        add("Coordinates", "Displays current XYZ coordinates.", ModuleCategory.PLAYER, true);
        add("Player Info", "Displays player and dimension information.", ModuleCategory.PLAYER, true);
        add("Inventory Manager", "Refills the selected hotbar slot from the main inventory when it becomes empty.", ModuleCategory.PLAYER, true);
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
        add("Connection Monitor", "Shows whether the client is in singleplayer or multiplayer and displays current ping.", ModuleCategory.HUD, true);
        add("Server Compatibility", "Disables automated combat and movement modules while connected to a multiplayer server.", ModuleCategory.PLAYER, true);
        ConfigManager.apply(MODULES);
    }

    private static void add(String name, String description, ModuleCategory category, boolean implemented) {
        GojoModule module = new GojoModule(name, description, category, implemented);
        if (implemented && (name.equals("FPS") || name.equals("HUD Coordinates") || name.equals("Clock") || name.equals("Connection Monitor"))) {
            module.setEnabled(true);
        }
        MODULES.add(module);
        BY_NAME.put(name, module);
    }

    public static List<GojoModule> all() { return Collections.unmodifiableList(MODULES); }
    public static GojoModule find(String name) { return BY_NAME.get(name); }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            restoreGlow();
            restoreHitboxes();
            restoreFullbright(client);
            return;
        }

        tickServerCompatibility(client);

        GojoModule sprint = find("Auto Sprint");
        if (sprint != null && sprint.enabled() && client.options.forwardKey.isPressed()
                && !player.isSneaking() && player.getHungerManager().getFoodLevel() > 6) {
            player.setSprinting(true);
        }

        GojoModule speed = find("Speed");
        if (speed != null && speed.enabled() && player.isOnGround()) {
            Vec3d velocity = player.getVelocity();
            Vec3d horizontal = new Vec3d(velocity.x, 0.0, velocity.z);
            if (horizontal.lengthSquared() > 0.0004) {
                double targetSpeed = 0.10 + (speed.value() - 1) * 0.0015;
                Vec3d adjusted = horizontal.normalize().multiply(targetSpeed);
                player.setVelocity(adjusted.x, velocity.y, adjusted.z);
            }
        }

        GojoModule respawn = find("Auto Respawn");
        if (respawn != null && respawn.enabled() && player.isDead()) player.requestRespawn();

        tickFullbright(client);
        tickNoFall(player);
        tickNoSlow(player);
        tickStepAssist(player);
        tickAutoTool(client, player);
        tickAutoEat(client, player);
        tickInventoryManager(client, player);
        tickEsp(client, player);
        tickAimbot(client, player);
        tickAutoClick(client);
        tickKillAura(client, player, "KillAura", true);
        tickKillAura(client, player, "Mob KillAura", false);
        tickHitboxes(client, player);
        tickScaffold(client, player);
    }

    
    private static void tickServerCompatibility(MinecraftClient client) {
        GojoModule compatibility = find("Server Compatibility");
        if (compatibility == null || !compatibility.enabled() || client.getCurrentServerEntry() == null) return;

        String[] restrictedModules = {
                "KillAura", "Mob KillAura", "Hitbox", "Aimbot", "Auto Click",
                "Speed", "NoFall", "NoSlow", "Step Assist", "Scaffold"
        };
        boolean changed = false;
        for (String name : restrictedModules) {
            GojoModule module = find(name);
            if (module != null && module.enabled()) {
                module.setEnabled(false);
                changed = true;
            }
        }
        if (changed) {
            nextAutoClickNanos = 0L;
            nextPlayerKillAuraNanos = 0L;
            nextMobKillAuraNanos = 0L;
            ConfigManager.save(MODULES);
            net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                    dev.gojo.client.GojoClient.LOGGER.info("Server Compatibility disabled automated combat/movement modules on a multiplayer server."));
        }
    }

    private static void tickFullbright(MinecraftClient client) {
        GojoModule fullbright = find("Fullbright");
        if (fullbright != null && fullbright.enabled()) {
            if (previousGamma < 0.0) previousGamma = client.options.getGamma().getValue();
            if (client.options.getGamma().getValue() < 1.0) {
                client.options.getGamma().setValue(1.0);
            }
        } else {
            restoreFullbright(client);
        }
    }

    private static void restoreFullbright(MinecraftClient client) {
        if (previousGamma >= 0.0) {
            client.options.getGamma().setValue(previousGamma);
            previousGamma = -1.0;
        }
    }

    private static void tickNoFall(ClientPlayerEntity player) {
        GojoModule noFall = find("NoFall");
        if (noFall != null && noFall.enabled() && player.fallDistance > 2.5F) {
            player.fallDistance = 0.0F;
        }
    }

    private static void tickNoSlow(ClientPlayerEntity player) {
        GojoModule noSlow = find("NoSlow");
        if (noSlow == null || !noSlow.enabled() || !player.isUsingItem()) return;
        Vec3d velocity = player.getVelocity();
        if (velocity.horizontalLengthSquared() > 0.000001) {
            player.setVelocity(velocity.x * 1.15, velocity.y, velocity.z * 1.15);
        }
    }

    private static void tickStepAssist(ClientPlayerEntity player) {
        GojoModule step = find("Step Assist");
        if (step == null || !step.enabled() || !player.horizontalCollision
                || !player.isOnGround() || player.isSneaking()
                || player.getVelocity().horizontalLengthSquared() < 0.0025) return;
        player.jump();
    }

    private static void tickAutoTool(MinecraftClient client, ClientPlayerEntity player) {
        GojoModule autoTool = find("Auto Tool");
        if (autoTool == null || !autoTool.enabled() || client.currentScreen != null
                || !client.options.attackKey.isPressed()
                || !(client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return;

        var state = client.world.getBlockState(hit.getBlockPos());
        if (state.isAir() || state.getHardness(client.world, hit.getBlockPos()) < 0.0F) return;

        int bestSlot = player.getInventory().getSelectedSlot();
        float bestSpeed = player.getInventory().getStack(bestSlot).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        player.getInventory().setSelectedSlot(bestSlot);
    }

    private static void tickAutoEat(MinecraftClient client, ClientPlayerEntity player) {
        GojoModule autoEat = find("Auto Eat");
        if (autoEat == null || !autoEat.enabled() || client.currentScreen != null
                || client.interactionManager == null) {
            if (autoEatOriginalSlot >= 0 && !player.isUsingItem()) {
                player.getInventory().setSelectedSlot(autoEatOriginalSlot);
                autoEatOriginalSlot = -1;
                autoEatStarted = false;
            }
            return;
        }

        if (autoEatStarted) {
            if (!player.isUsingItem()) {
                player.getInventory().setSelectedSlot(autoEatOriginalSlot);
                autoEatOriginalSlot = -1;
                autoEatStarted = false;
            }
            return;
        }
        if (player.getHungerManager().getFoodLevel() > 14 || player.isUsingItem()) return;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty() || stack.get(DataComponentTypes.FOOD) == null) continue;
            autoEatOriginalSlot = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(slot);
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            autoEatStarted = true;
            return;
        }
    }

    private static void tickInventoryManager(MinecraftClient client, ClientPlayerEntity player) {
        GojoModule manager = find("Inventory Manager");
        if (manager == null || !manager.enabled() || client.currentScreen != null
                || client.interactionManager == null) return;
        int selected = player.getInventory().getSelectedSlot();
        if (!player.getInventory().getStack(selected).isEmpty()) return;
        for (int slot = 9; slot < 36; slot++) {
            if (!player.getInventory().getStack(slot).isEmpty()) {
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                        slot, selected, SlotActionType.SWAP, player);
                return;
            }
        }
    }

    private static void tickEsp(MinecraftClient client, ClientPlayerEntity player) {
        GojoModule entityEsp = find("Entity ESP");
        GojoModule playerEsp = find("Player ESP");
        boolean showEntities = entityEsp != null && entityEsp.enabled();
        boolean showPlayers = playerEsp != null && playerEsp.enabled();
        if (!showEntities && !showPlayers) {
            restoreGlow();
            return;
        }

        Set<Entity> visibleNow = new HashSet<>();
        for (Entity entity : client.world.getEntities()) {
            if (entity == player || !entity.isAlive() || player.squaredDistanceTo(entity) > 4096.0) continue;
            boolean shouldHighlight = showEntities && entity instanceof LivingEntity
                    || showPlayers && entity instanceof PlayerEntity;
            if (!shouldHighlight) continue;
            PREVIOUS_GLOW.putIfAbsent(entity, entity.isGlowing());
            entity.setGlowing(true);
            visibleNow.add(entity);
        }

        Iterator<Map.Entry<Entity, Boolean>> iterator = PREVIOUS_GLOW.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Boolean> entry = iterator.next();
            if (!visibleNow.contains(entry.getKey())) {
                entry.getKey().setGlowing(entry.getValue());
                iterator.remove();
            }
        }
    }

    private static void restoreGlow() {
        for (Map.Entry<Entity, Boolean> entry : PREVIOUS_GLOW.entrySet()) {
            entry.getKey().setGlowing(entry.getValue());
        }
        PREVIOUS_GLOW.clear();
    }

    private static void tickAimbot(MinecraftClient client, ClientPlayerEntity player) {
        GojoModule aim = find("Aimbot");
        if (aim == null || !aim.enabled() || client.currentScreen != null) return;

        double range = 32.0;
        double halfFov = Math.toRadians(aim.value() / 2.0);
        double minDot = Math.cos(halfFov);
        Vec3d look = player.getRotationVec(1.0F);
        PlayerEntity target = null;
        double closest = range * range;

        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate == player || !candidate.isAlive() || candidate.isSpectator()
                    || candidate.isTeammate(player) || !player.canSee(candidate)) continue;
            double distance = player.squaredDistanceTo(candidate);
            if (distance >= closest) continue;
            Vec3d direction = candidate.getEyePos().subtract(player.getEyePos()).normalize();
            if (look.dotProduct(direction) < minDot) continue;
            closest = distance;
            target = candidate;
        }

        if (target == null) return;
        Vec3d delta = target.getEyePos().subtract(player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        float smooth = 0.35F;
        player.setYaw(player.getYaw() + MathHelper.wrapDegrees(targetYaw - player.getYaw()) * smooth);
        player.setPitch(player.getPitch() + (targetPitch - player.getPitch()) * smooth);
    }

    private static void tickAutoClick(MinecraftClient client) {
        GojoModule autoClick = find("Auto Click");
        if (autoClick == null || !autoClick.enabled() || client.currentScreen != null
                || client.interactionManager == null || !client.options.attackKey.isPressed()) {
            nextAutoClickNanos = 0L;
            return;
        }
        long now = System.nanoTime();
        long interval = 1_000_000_000L / Math.max(1, Math.min(20, autoClick.value()));
        if (nextAutoClickNanos == 0L || now >= nextAutoClickNanos) {
            HitResult target = client.crosshairTarget;
            if (target instanceof EntityHitResult entityHit) {
                client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                client.player.swingHand(Hand.MAIN_HAND);
            } else if (target instanceof BlockHitResult blockHit) {
                client.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                client.player.swingHand(Hand.MAIN_HAND);
            } else {
                client.player.swingHand(Hand.MAIN_HAND);
            }
            nextAutoClickNanos = now + interval;
        }
    }

    private static void tickKillAura(MinecraftClient client, ClientPlayerEntity player,
                                     String moduleName, boolean playersOnly) {
        GojoModule aura = find(moduleName);
        if (aura == null || !aura.enabled() || client.interactionManager == null
                || client.currentScreen != null) return;
        long now = System.nanoTime();
        long interval = 1_000_000_000L / Math.max(1, Math.min(20, aura.value()));
        long nextAttack = playersOnly ? nextPlayerKillAuraNanos : nextMobKillAuraNanos;
        if (now < nextAttack || player.getAttackCooldownProgress(0.5F) < 1.0F) return;

        double range = 4.2;
        double minDot = Math.cos(Math.toRadians(55.0));
        Vec3d look = player.getRotationVec(1.0F);
        LivingEntity target = null;
        double closest = range * range;

        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class, player.getBoundingBox().expand(range),
                candidate -> candidate != player && candidate.isAlive() && !candidate.isTeammate(player)
                        && (candidate instanceof PlayerEntity) == playersOnly)) {
            double distance = player.squaredDistanceTo(entity);
            if (distance >= closest || !player.canSee(entity)) continue;
            Vec3d direction = entity.getEyePos().subtract(player.getEyePos()).normalize();
            if (look.dotProduct(direction) < minDot) continue;
            closest = distance;
            target = entity;
        }

        if (target != null) {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            long next = System.nanoTime() + interval;
            if (playersOnly) nextPlayerKillAuraNanos = next;
            else nextMobKillAuraNanos = next;
        }
    }

    private static Box vanillaHitbox(Entity entity) {
        double halfWidth = entity.getWidth() * 0.5;
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        return new Box(x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + entity.getHeight(), z + halfWidth);
    }

    private static void tickHitboxes(MinecraftClient client, ClientPlayerEntity player) {
        GojoModule hitbox = find("Hitbox");
        if (hitbox == null || !hitbox.enabled()) {
            restoreHitboxes();
            return;
        }

        Set<Entity> nearby = new HashSet<>();
        // Keep the expansion bounded: the slider controls a modest local targeting adjustment.
        double expansion = Math.max(0.005, Math.min(0.50, hitbox.value() * 0.005));
        Box searchArea = player.getBoundingBox().expand(64.0);

        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class, searchArea,
                candidate -> candidate != player && candidate.isAlive())) {
            Box base = vanillaHitbox(entity);
            entity.setBoundingBox(base.expand(expansion, expansion * 0.75, expansion));
            MODIFIED_HITBOXES.add(entity);
            nearby.add(entity);
        }

        Iterator<Entity> iterator = MODIFIED_HITBOXES.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (!nearby.contains(entity)) {
                entity.setBoundingBox(vanillaHitbox(entity));
                iterator.remove();
            }
        }
    }

    private static void restoreHitboxes() {
        for (Entity entity : MODIFIED_HITBOXES) {
            entity.setBoundingBox(vanillaHitbox(entity));
        }
        MODIFIED_HITBOXES.clear();
    }

    private static void tickScaffold(MinecraftClient client, ClientPlayerEntity player) {
        GojoModule scaffold = find("Scaffold");
        if (scaffold == null || !scaffold.enabled() || client.interactionManager == null
                || client.currentScreen != null || player.isSneaking()
                || player.getVelocity().horizontalLengthSquared() < 0.001) return;

        BlockPos target = BlockPos.ofFloored(player.getX(), player.getY() - 1.0, player.getZ());
        if (!client.world.getBlockState(target).isAir()) return;

        int selectedSlot = player.getInventory().getSelectedSlot();
        boolean hasBlock = player.getInventory().getStack(selectedSlot).getItem() instanceof BlockItem;
        if (!hasBlock) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack.getItem() instanceof BlockItem) {
                    player.getInventory().setSelectedSlot(slot);
                    hasBlock = true;
                    break;
                }
            }
        }
        if (!hasBlock) return;

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos support = target.offset(direction);
            if (client.world.getBlockState(support).isAir()) continue;
            Direction face = direction.getOpposite();
            Vec3d hitPos = Vec3d.ofCenter(support).add(
                    face.getOffsetX() * 0.5,
                    face.getOffsetY() * 0.5,
                    face.getOffsetZ() * 0.5
            );
            BlockHitResult hit = new BlockHitResult(hitPos, face, support, false);
            if (client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit).isAccepted()) {
                player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }
}
