package com.mx.viewboard.client;

import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ControlsScreenBridge {
    private static final ViewBoardKeybindRules RULES = ViewBoardKeybindRules.getInstance();

    private ControlsScreenBridge() {}

    public static void decorate(KeyBindsScreen screen) {
        RULES.ensureLoaded();
        RULES.syncRuntimeState();

        try {
            Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            listField.setAccessible(true);
            Object rawList = listField.get(screen);

            if (!(rawList instanceof KeyBindsList keyBindsList)) {
                return;
            }

            for (Object entry : keyBindsList.children()) {
                decorateEntry(screen, entry);
            }

        } catch (Exception e) {
            // Never crash UI.
            // Rendering hooks call this frequently, so avoid noisy stacktraces.
        }
    }

    private static void decorateEntry(KeyBindsScreen screen, Object entry) {
        try {
            Class<?> clazz = entry.getClass();

            // --- KeyMapping (FIXED FIELD NAME) ---
            Field mappingField = null;
            try {
                mappingField = clazz.getDeclaredField("key"); // 1.21.1
            } catch (NoSuchFieldException ignored) {
                // fall through
            }
            if (mappingField == null) {
                try {
                    mappingField = clazz.getDeclaredField("keyMapping"); // other versions
                } catch (NoSuchFieldException ignored) {
                    return; // not a key entry (probably category header)
                }
            }

            mappingField.setAccessible(true);
            Object rawMapping = mappingField.get(entry);
            if (!(rawMapping instanceof KeyMapping mapping)) {
                return;
            }

            // --- changeButton ---
            Field changeButtonField = clazz.getDeclaredField("changeButton");
            changeButtonField.setAccessible(true);
            Button changeButton = (Button) changeButtonField.get(entry);

            // --- collision logic ---
            boolean collision = hasEffectiveConflict(mapping);

            // --- hasCollision (optional field) ---
            try {
                Field hasCollisionField = clazz.getDeclaredField("hasCollision");
                hasCollisionField.setAccessible(true);
                hasCollisionField.setBoolean(entry, collision);
            } catch (NoSuchFieldException ignored) {
                // field removed/renamed in newer versions → ignore safely
            }

            // --- tooltip ---
            changeButton.setTooltip(Tooltip.create(createTooltip(mapping, collision)));

            if (screen.selectedKey == mapping) {
                return;
            }

            Component baseMessage = mapping.getTranslatedKeyMessage();

            if (collision) {
                changeButton.setMessage(Component.literal("[")
                    .append(baseMessage.copy().withStyle(ChatFormatting.WHITE))
                    .append("]")
                    .withStyle(ChatFormatting.RED));
            } else {
                changeButton.setMessage(baseMessage);
            }

        } catch (Exception e) {
            // Never crash UI; avoid noisy logs for render-time reflection.
        }
    }

    private static boolean hasEffectiveConflict(KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        if (RULES.isIgnored(mapping)) {
            return false;
        }

        ViewBoardKeybindRules.KeyBindingState state = RULES.collectBindingStates().stream()
            .filter(candidate -> Objects.equals(candidate.keybindId(), mapping.getName()))
            .findFirst()
            .orElse(null);

        if (state == null) {
            return false;
        }

        for (ViewBoardKeybindRules.KeyBindingState other : RULES.usageFor(mapping.getKey()).states()) {
            if (Objects.equals(other.keybindId(), state.keybindId()) || other.ignored()) {
                continue;
            }

            if (!Objects.equals(other.groupId(), state.groupId()) || state.groupId() == null) {
                return true;
            }
        }

        return false;
    }

    private static Component createTooltip(KeyMapping mapping, boolean collision) {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.translatable(mapping.getName()));

        if (RULES.isIgnored(mapping)) {
            lines.add(Component.translatable("viewboard.tooltip.ignored"));
        }

        RULES.groupFor(mapping).ifPresent(group ->
            lines.add(Component.translatable("viewboard.tooltip.group",
                Component.literal(group.name())))
        );

        if (collision) {
            List<Component> conflicts = new ArrayList<>();

            for (ViewBoardKeybindRules.KeyBindingState state :
                    RULES.usageFor(mapping.getKey()).states()) {

                if (!Objects.equals(state.keybindId(), mapping.getName()) && !state.ignored()) {
                    conflicts.add(Component.translatable(state.mapping().getName()));
                }
            }

            if (!conflicts.isEmpty()) {
                MutableComponent joined = Component.empty();

                for (int i = 0; i < conflicts.size(); i++) {
                    if (i > 0) joined.append(", ");
                    joined.append(conflicts.get(i));
                }

                lines.add(Component.translatable(
                    "controls.keybinds.duplicateKeybinds", joined));
            }

        } else {
            lines.add(Component.translatable("viewboard.controls.no_conflict"));
        }

        MutableComponent tooltip = Component.empty();

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) tooltip.append("\n");
            tooltip.append(lines.get(i));
        }

        return tooltip;
    }
}
