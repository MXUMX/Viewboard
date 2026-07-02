package com.mx.viewboard.client.mixin;

import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {
    @Inject(method = "getRowWidth", at = @At("HEAD"), cancellable = true)
    private void viewboard$getRowWidth(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.viewboard$rowWidth());
    }

    @Inject(method = "getScrollbarPosition", at = @At("HEAD"), cancellable = true)
    private void viewboard$getScrollbarPosition(CallbackInfoReturnable<Integer> cir) {
        int screenWidth = this.viewboard$screenWidth();
        cir.setReturnValue(Math.min(screenWidth - 6, this.viewboard$rowLeft() + this.viewboard$rowWidth() + 6));
    }

    private int viewboard$rowWidth() {
        return ViewBoardKeybindRules.getInstance().controlsListWidthMode().rowWidth(this.viewboard$screenWidth());
    }

    private int viewboard$rowLeft() {
        return Math.max(0, (this.viewboard$screenWidth() - this.viewboard$rowWidth()) / 2);
    }

    private int viewboard$screenWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null ? minecraft.getWindow().getGuiScaledWidth() : minecraft.screen.width;
    }
}
