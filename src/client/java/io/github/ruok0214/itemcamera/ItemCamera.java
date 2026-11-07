package io.github.ruok0214.itemcamera;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class ItemCamera implements ClientModInitializer {
    private static final double RANGE = 32;
    private static ItemEntity target;
    private CameraType previousView;
    private KeyMapping toggle;

    @Override
    public void onInitializeClient() {
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dropped_item_camera", "main"));
        toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dropped_item_camera.toggle",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public static boolean follows(Entity entity) {
        return target != null && target == entity;
    }

    private void tick(Minecraft mc) {
        if (target != null && (mc.getCameraEntity() != target || target.isRemoved() || target.level() != mc.level
                || mc.player == null || !mc.player.isAlive())) {
            stop(mc);
        }
        while (toggle.consumeClick()) {
            if (mc.player == null || mc.level == null || mc.gui.screen() != null || !mc.player.isAlive()) continue;
            if (target != null) {
                stop(mc);
            } else if (mc.getCameraEntity() == mc.player) {
                ItemEntity selected = findItem(mc);
                if (selected == null) {
                    mc.player.sendOverlayMessage(Component.translatable("dropped_item_camera.missing"));
                    continue;
                }
                previousView = mc.options.getCameraType();
                target = selected;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.setCameraEntity(target);
                mc.player.sendOverlayMessage(Component.translatable("dropped_item_camera.started",
                        target.getItem().getHoverName(), mc.options.keyTogglePerspective.getTranslatedKeyMessage(),
                        toggle.getTranslatedKeyMessage()));
            }
        }
    }

    private void stop(Minecraft mc) {
        if (mc.getCameraEntity() == target) mc.setCameraEntity(mc.player);
        target = null;
        mc.options.setCameraType(previousView);
        if (mc.player != null) mc.player.sendOverlayMessage(Component.translatable("dropped_item_camera.stopped"));
    }

    private ItemEntity findItem(Minecraft mc) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = start.add(mc.player.getViewVector(1).scale(RANGE));
        var block = mc.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        Vec3 visibleEnd = block.getLocation();
        double closest = Double.POSITIVE_INFINITY;
        ItemEntity result = null;
        for (var item : mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(start, visibleEnd).inflate(0.35))) {
            var bounds = item.getBoundingBox().inflate(0.35);
            var hit = bounds.contains(start) ? start : bounds.clip(start, visibleEnd).orElse(null);
            if (hit == null || item.isRemoved()) continue;
            double distance = start.distanceToSqr(hit);
            if (distance < closest && mc.player.hasLineOfSight(item)) {
                closest = distance;
                result = item;
            }
        }
        return result;
    }
}
