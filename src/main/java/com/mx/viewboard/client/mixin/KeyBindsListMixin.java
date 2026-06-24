package com.mx.viewboard.client.mixin;

import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
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
        cir.setReturnValue(ViewBoardKeybindRules.getInstance().controlsListWidthMode().rowWidth(this.keyBindsScreen.width));
    }
}
