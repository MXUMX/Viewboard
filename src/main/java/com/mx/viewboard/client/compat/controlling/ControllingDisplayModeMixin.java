package com.mx.viewboard.client.compat.controlling;

import com.mx.viewboard.client.keybind.ViewBoardConflictHooks;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.blamejared.controlling.api.DisplayMode", remap = false)
public abstract class ControllingDisplayModeMixin {
    @Inject(method = "getPredicate", at = @At("RETURN"), cancellable = true)
    private void viewboard$wrapConflictingPredicate(CallbackInfoReturnable<Predicate<KeyBindsList.Entry>> cir) {
        if (!"CONFLICTING".equals(String.valueOf(this))) {
            return;
        }

        Predicate<KeyBindsList.Entry> original = cir.getReturnValue();
        cir.setReturnValue(entry -> original.test(entry) && viewboard$hasEffectiveConflict(entry));
    }

    private static boolean viewboard$hasEffectiveConflict(KeyBindsList.Entry entry) {
        try {
            Object key = entry.getClass().getMethod("getKey").invoke(entry);
            return key instanceof KeyMapping mapping && ViewBoardConflictHooks.hasEffectiveConflict(mapping);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
