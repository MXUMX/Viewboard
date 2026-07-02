package com.mx.viewboard.client.mixin;

import com.mx.viewboard.client.ViewBoardClientEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin extends Screen {
    protected KeyBindsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void viewboard$initControls(CallbackInfo ci) {
        ViewBoardClientEvents.ensureControlsButtons((KeyBindsScreen) (Object) this, this::viewboard$addButton);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void viewboard$beforeRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ViewBoardClientEvents.beforeControlsRender((KeyBindsScreen) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void viewboard$afterRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ViewBoardClientEvents.afterControlsRender((KeyBindsScreen) (Object) this, graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void viewboard$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (ViewBoardClientEvents.dispatchRowActionClick((KeyBindsScreen) (Object) this, mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    private void viewboard$addButton(Button button) {
        this.addRenderableWidget(button);
    }
}
