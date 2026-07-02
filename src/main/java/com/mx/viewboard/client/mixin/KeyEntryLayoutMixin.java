package com.mx.viewboard.client.mixin;

import com.mx.viewboard.client.keybind.ControlsListWidthMode;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyEntryLayoutMixin {
    @Shadow
    @Final
    private Button changeButton;

    @Shadow
    @Final
    private Button resetButton;

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"), index = 2)
    private int viewboard$clampKeyNameX(int x) {
        return Math.max(8, x);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setX(I)V", ordinal = 0))
    private int viewboard$moveResetButton(int x) {
        return this.viewboard$rightEdge() - this.resetButton.getWidth();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setX(I)V", ordinal = 1))
    private int viewboard$moveChangeButton(int x) {
        return this.viewboard$rightEdge() - this.resetButton.getWidth() - 10 - this.changeButton.getWidth();
    }

    private int viewboard$rightEdge() {
        if (Minecraft.getInstance().screen == null) {
            return this.resetButton.getX() + this.resetButton.getWidth();
        }
        int screenWidth = Minecraft.getInstance().screen.width;
        ControlsListWidthMode mode = ViewBoardKeybindRules.getInstance().controlsListWidthMode();
        int rowWidth = mode.rowWidth(screenWidth);
        int rowLeft = Math.max(0, (screenWidth - rowWidth) / 2);
        return Math.min(screenWidth - 18, rowLeft + rowWidth - 8);
    }
}
