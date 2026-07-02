package com.mx.viewboard.client.compat.controlling;

import com.mx.viewboard.client.ControlsScreenBridge;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.lang.reflect.Field;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.blamejared.controlling.client.NewKeyBindsList$KeyEntry", remap = false)
public abstract class ControllingKeyEntryMixin {
    @Inject(method = "refreshEntry", at = @At("TAIL"))
    private void viewboard$applyConflictRules(CallbackInfo ci) {
        try {
            Object self = this;
            KeyMapping key = (KeyMapping) viewboard$getField(self, "key");
            Button changeButton = (Button) viewboard$getField(self, "btnChangeKeyBinding");
            Button resetButton = (Button) viewboard$getField(self, "btnResetKeyBinding");

            changeButton.setMessage(key.getTranslatedKeyMessage());
            resetButton.active = !key.isDefault();
            MutableComponent duplicates = Component.empty();

            for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
                if (com.mx.viewboard.client.keybind.ViewBoardConflictHooks.conflictsVisible(key, mapping)) {
                    if (!duplicates.getString().isEmpty()) {
                        duplicates.append(", ");
                    }
                    duplicates.append(mapping.getTranslatedKeyMessage());
                }
            }

            ViewBoardKeybindRules rules = ViewBoardKeybindRules.getInstance();
            rules.ensureLoaded();
            rules.syncRuntimeState();
            var states = rules.collectBindingStates();
            boolean hasCollision = ControlsScreenBridge.hasEffectiveConflict(key, states);
            viewboard$setField(self, "hasCollision", hasCollision);

            KeyBindsScreen screen = viewboard$screen(self);
            if (screen.selectedKey == key) {
                ControlsScreenBridge.applyButtonPresentation(screen, key, changeButton, hasCollision, states);
            } else {
                ControlsScreenBridge.applyButtonPresentation(screen, key, changeButton, hasCollision, states);
            }
        } catch (ReflectiveOperationException ignored) {
            // Controlling is optional; never let a compat reflection miss crash the controls screen.
        }
    }

    private static Object viewboard$getField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void viewboard$setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static KeyBindsScreen viewboard$screen(Object self) throws ReflectiveOperationException {
        Object list = viewboard$getField(self, "this$0");
        return (KeyBindsScreen) viewboard$getField(list, "controlsScreen");
    }
}
