package com.mx.viewboard.client.mixin;

import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {
    @Shadow
    @Final
    private KeyBindsScreen keyBindsScreen;

    @Inject(method = "getRowWidth", at = @At("HEAD"), cancellable = true)
    private void viewboard$getRowWidth(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.viewboard$rowWidth());
    }

    @Inject(method = "getScrollbarPosition", at = @At("HEAD"), cancellable = true)
    private void viewboard$getScrollbarPosition(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.min(this.keyBindsScreen.width - 6, this.viewboard$rowLeft() + this.viewboard$rowWidth() + 6));
    }

    private int viewboard$rowWidth() {
        return ViewBoardKeybindRules.getInstance().controlsListWidthMode().rowWidth(this.keyBindsScreen.width);
    }

    private int viewboard$rowLeft() {
        return Math.max(0, (this.keyBindsScreen.width - this.viewboard$rowWidth()) / 2);
    }
}
