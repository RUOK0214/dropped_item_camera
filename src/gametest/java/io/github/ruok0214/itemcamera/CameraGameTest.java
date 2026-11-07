package io.github.ruok0214.itemcamera;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
public final class CameraGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext c) {
        c.getInput().resizeWindow(1100, 780);
        ModMenuChecks.run(c, "ko_kr", "ko");
        ModMenuChecks.run(c, "en_us", "en");
        try (var world = c.worldBuilder().create()) {
            var server = world.getServer();
            world.getConnection().waitForChunksDownload();
            server.runCommand("fill -4 99 -4 4 99 10 stone");
            server.runCommand("tp @a 0.5 100 5.5 180 16");
            server.runCommand("summon item 0.5 100 0.5 {Item:{id:'minecraft:diamond',count:1},PickupDelay:32767s}");
            c.waitFor(mc -> !mc.level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(10)).isEmpty());
            c.waitTicks(10);
            c.getInput().pressKey(GLFW.GLFW_KEY_V);
            c.waitFor(mc -> mc.getCameraEntity() instanceof ItemEntity);
            checkView(c, CameraType.THIRD_PERSON_BACK);
            c.waitTicks(5);
            c.takeScreenshot("follow-back");
            c.getInput().pressKey(GLFW.GLFW_KEY_F5);
            checkView(c, CameraType.THIRD_PERSON_FRONT);
            c.getInput().pressKey(GLFW.GLFW_KEY_F5);
            checkView(c, CameraType.FIRST_PERSON);
            server.runCommand("tp @e[type=item,limit=1,sort=nearest] 2.5 100 0.5");
            c.waitFor(mc -> Math.abs(mc.gameRenderer.mainCamera().position().x - 2.5) < 0.1);
            c.getInput().pressKey(GLFW.GLFW_KEY_V);
            c.waitFor(mc -> mc.getCameraEntity() == mc.player);
            server.runCommand("tp @e[type=item,limit=1,sort=nearest] 0.5 100 0.5");
            c.waitTicks(12);
            c.getInput().pressKey(GLFW.GLFW_KEY_V);
            c.waitFor(mc -> mc.getCameraEntity() instanceof ItemEntity);
            server.runCommand("kill @e[type=item]");
            c.waitFor(mc -> mc.getCameraEntity() == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON);
        }
    }

    private static void checkView(ClientGameTestContext c, CameraType expected) {
        c.waitFor(mc -> mc.options.getCameraType() == expected);
        c.runOnClient(mc -> {
            var camera = mc.gameRenderer.mainCamera();
            camera.update(DeltaTracker.ONE);
            float expectedYaw = mc.player.getViewYRot(1) + (expected.isMirrored() ? 180 : 0);
            float expectedPitch = mc.player.getViewXRot(1) * (expected.isMirrored() ? -1 : 1);
            if (Math.abs(Mth.wrapDegrees(camera.yRot() - expectedYaw)) > 0.1
                    || Math.abs(camera.xRot() - expectedPitch) > 0.1
                    || camera.isDetached() == expected.isFirstPerson()) {
                throw new AssertionError("Incorrect item camera angles or perspective in " + expected);
            }
        });
    }
}
