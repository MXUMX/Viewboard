package com.mx.viewboard.client.mixin;

import com.mx.viewboard.client.ControlsScreenBridge;
import com.mx.viewboard.client.GroupEditorScreen;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyEntryPresentationMixin {
    @Shadow
    @Final
    private KeyMapping key;

    @Shadow
    @Final
    private Button changeButton;

    @Shadow
    private boolean hasCollision;

    @Unique
    private Button viewboard$groupButton;

    @Unique
    private Button viewboard$ignoreButton;

    @Inject(method = "refreshEntry", at = @At("TAIL"))
    private void viewboard$refreshEntry(CallbackInfo ci) {
        this.viewboard$applyPresentation();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void viewboard$renderHead(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        this.viewboard$applyPresentation();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void viewboard$renderTail(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        this.viewboard$ensureButtons();
        this.viewboard$positionButtons(top);
        this.viewboard$groupButton.render(graphics, mouseX, mouseY, partialTick);
        this.viewboard$ignoreButton.render(graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void viewboard$children(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
        this.viewboard$ensureButtons();
        List<GuiEventListener> children = new ArrayList<>(cir.getReturnValue());
        children.add(this.viewboard$groupButton);
        children.add(this.viewboard$ignoreButton);
        cir.setReturnValue(children);
    }

    @Inject(method = "narratables", at = @At("RETURN"), cancellable = true)
    private void viewboard$narratables(CallbackInfoReturnable<List<? extends NarratableEntry>> cir) {
        this.viewboard$ensureButtons();
        List<NarratableEntry> narratables = new ArrayList<>(cir.getReturnValue());
        narratables.add(this.viewboard$groupButton);
        narratables.add(this.viewboard$ignoreButton);
        cir.setReturnValue(narratables);
    }

    private void viewboard$applyPresentation() {
        if (!(Minecraft.getInstance().screen instanceof KeyBindsScreen screen)) {
            return;
        }
        ViewBoardKeybindRules rules = ViewBoardKeybindRules.getInstance();
        rules.ensureLoaded();
        rules.syncRuntimeState();
        List<ViewBoardKeybindRules.KeyBindingState> states = rules.collectBindingStates();
        boolean collision = ControlsScreenBridge.hasEffectiveConflict(this.key, states);
        this.hasCollision = collision;
        ControlsScreenBridge.applyButtonPresentation(screen, this.key, this.changeButton, collision, states);
        if (this.viewboard$ignoreButton != null) {
            boolean ignored = rules.isIgnored(this.key);
            this.viewboard$ignoreButton.setMessage(Component.literal(ignored ? "!" : "I"));
            this.viewboard$ignoreButton.setTooltip(Tooltip.create(ignored
                ? Component.translatable("viewboard.controls.button.ignore_on")
                : Component.translatable("viewboard.controls.button.ignore_off")));
        }
    }

    @Unique
    private void viewboard$ensureButtons() {
        if (this.viewboard$groupButton == null) {
            this.viewboard$groupButton = Button.builder(Component.literal("G"), button -> {
                if (Minecraft.getInstance().screen instanceof KeyBindsScreen screen) {
                    Minecraft.getInstance().setScreen(new GroupEditorScreen(screen, this.key));
                }
            }).bounds(0, 0, 20, 20).tooltip(Tooltip.create(Component.translatable("viewboard.controls.button.group"))).build();
        }
        if (this.viewboard$ignoreButton == null) {
            this.viewboard$ignoreButton = Button.builder(Component.literal("I"), button -> {
                ViewBoardKeybindRules rules = ViewBoardKeybindRules.getInstance();
                rules.setIgnored(this.key, !rules.isIgnored(this.key));
                this.viewboard$applyPresentation();
            }).bounds(0, 0, 20, 20).build();
        }
    }

    @Unique
    private void viewboard$positionButtons(int top) {
        int gap = 2;
        int ignoreX = this.changeButton.getX() - gap - this.viewboard$ignoreButton.getWidth();
        int groupX = ignoreX - gap - this.viewboard$groupButton.getWidth();
        this.viewboard$groupButton.setX(groupX);
        this.viewboard$groupButton.setY(top);
        this.viewboard$ignoreButton.setX(ignoreX);
        this.viewboard$ignoreButton.setY(top);
    }
}
