package com.mx.viewboard.client;

import com.mx.viewboard.ViewBoardMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ViewBoardMod.MOD_ID, value = Dist.CLIENT)
public final class ViewBoardClientEvents {
    private ViewBoardClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        event.addListener(Button.builder(Component.translatable("viewboard.button.open"), button ->
                minecraft.setScreen(new KeyboardViewScreen(keyBindsScreen)))
            .bounds(screenWidth - 106, screenHeight - 27, 100, 20)
            .build());
    }
}
