package com.mx.viewboard.client.keybind;

import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class ViewBoardConflictHooks {
    private static final ViewBoardKeybindRules RULES = ViewBoardKeybindRules.getInstance();

    private ViewBoardConflictHooks() {
    }

    public static boolean isIgnored(KeyMapping mapping) {
        RULES.ensureLoaded();
        return RULES.isIgnored(mapping);
    }

    public static boolean conflictsVisible(KeyMapping left, KeyMapping right) {
        if (left == null || right == null || left == right || left.isUnbound() || right.isUnbound()) {
            return false;
        }
        if (isIgnored(left) || isIgnored(right)) {
            return false;
        }

        String leftGroup = RULES.groupFor(left).map(KeybindGroupConfig::id).orElse(null);
        String rightGroup = RULES.groupFor(right).map(KeybindGroupConfig::id).orElse(null);
        if (leftGroup != null && Objects.equals(leftGroup, rightGroup)) {
            return false;
        }

        return left.same(right);
    }

    public static boolean hasEffectiveConflict(KeyMapping mapping) {
        if (mapping == null || mapping.isUnbound() || isIgnored(mapping)) {
            return false;
        }

        RULES.syncRuntimeState();
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (conflictsVisible(mapping, other)) {
                return true;
            }
        }
        return false;
    }
}
