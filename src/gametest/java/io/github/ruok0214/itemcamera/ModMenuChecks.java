package io.github.ruok0214.itemcamera;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.util.mod.fabric.FabricIconHandler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

@SuppressWarnings("UnstableApiUsage")
final class ModMenuChecks {
    private static final String MOD_ID = "dropped_item_camera";
    private static final String SUMMARY = "modmenu.summaryTranslation." + MOD_ID;
    private static final String DESCRIPTION = "modmenu.descriptionTranslation." + MOD_ID;

    static void run(ClientGameTestContext context, String language, String screenshotSuffix) {
        context.runOnClient(mc -> {
            mc.options.languageCode = language;
            mc.getLanguageManager().setSelected(language);
            mc.reloadResourcePacks();
        });
        String expectedSummary = language.equals("ko_kr")
                ? "떨어진 아이템을 안정적인 카메라로 따라갑니다."
                : "Follow a dropped item with a steady camera.";
        context.waitFor(mc -> mc.gui.overlay() == null
                && Language.getInstance().getOrDefault(SUMMARY).equals(expectedSummary));
        var parent = context.computeOnClient(mc -> mc.gui.screen());
        boolean previousTranslation = ModMenuConfig.TRANSLATE_DESCRIPTIONS.getValue();
        try {
            context.runOnClient(mc -> {
                ModMenuConfig.TRANSLATE_DESCRIPTIONS.setValue(true);
                mc.gui.setScreen(ModMenuApi.createModsScreen(parent));
            });
            context.waitForScreen(ModsScreen.class);
            context.runOnClient(mc -> {
                var screen = (ModsScreen) mc.gui.screen();
                screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
                        .findFirst().orElseThrow().setValue("Dropped Item Camera");
                var list = screen.children().stream().filter(ModListWidget.class::isInstance)
                        .map(ModListWidget.class::cast).findFirst().orElseThrow();
                list.select(list.children().stream().filter(entry -> entry.getMod().getId().equals(MOD_ID))
                        .findFirst().orElseThrow());
                var mod = screen.getSelectedEntry().getMod();
                var metadata = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
                check(mod.getVersion().equals(metadata.getVersion().getFriendlyString()), "Release version");
                check(mod.getTranslatedSummary().equals(expectedSummary), "Translated summary");
                check(mod.getTranslatedDescription().equals(Component.translatable(DESCRIPTION).getString()),
                        "Translated description");
                check(mod.getTranslatedDescription().contains("F5"), "Usage instructions");
                check(metadata.getLicense().contains("MIT"), "MIT license");
                check(metadata.getContact().get("issues").orElseThrow()
                        .equals("https://github.com/RUOK0214/dropped_item_camera/issues"), "Issue link");
                var container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
                try (var icons = new FabricIconHandler()) {
                    check(icons.createIcon(container, metadata.getIconPath(64).orElseThrow()) != null,
                            "Packaged icon decodes");
                }
            });
            context.waitTicks(4);
            context.takeScreenshot("modmenu-" + screenshotSuffix);
            context.runOnClient(mc -> {
                ModMenuConfig.TRANSLATE_DESCRIPTIONS.setValue(false);
                var mod = ((ModsScreen) mc.gui.screen()).getSelectedEntry().getMod();
                check(mod.getTranslatedDescription().equals(mod.getDescription()), "English fallback");
                mc.gui.screen().onClose();
                check(mc.gui.screen() == parent, "Return to previous screen");
            });
        } finally {
            context.runOnClient(mc -> ModMenuConfig.TRANSLATE_DESCRIPTIONS.setValue(previousTranslation));
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
