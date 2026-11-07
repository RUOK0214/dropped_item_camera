package io.github.ruok0214.itemcamera.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.ruok0214.itemcamera.ItemCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private Entity entity;

    // Keep vanilla position interpolation, F5 mirroring and wall collision.
    // Only the base viewing angles come from the player's mouse.
    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float itemCameraYaw(float original, float partialTick) {
        var player = Minecraft.getInstance().player;
        return player != null && ItemCamera.follows(entity) ? player.getViewYRot(partialTick) : original;
    }

    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float itemCameraPitch(float original, float partialTick) {
        var player = Minecraft.getInstance().player;
        return player != null && ItemCamera.follows(entity) ? player.getViewXRot(partialTick) : original;
    }
}
