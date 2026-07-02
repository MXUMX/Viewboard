package com.mx.viewboard.client;

import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import com.mx.viewboard.client.keybind.ViewBoardConflictHooks;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ControlsScreenBridge {
    private static final ViewBoardKeybindRules RULES = ViewBoardKeybindRules.getInstance();

    private ControlsScreenBridge() {}

    public static void decorate(KeyBindsScreen screen) {
        RULES.ensureLoaded();
        RULES.syncRuntimeState();
        List<ViewBoardKeybindRules.KeyBindingState> states = RULES.collectBindingStates();

        try {
            Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            listField.setAccessible(true);
            Object rawList = listField.get(screen);

            if (!(rawList instanceof KeyBindsList keyBindsList)) {
                return;
            }

            for (Object entry : keyBindsList.children()) {
                decorateEntry(screen, entry, states);
            }

        } catch (Exception e) {
            // Never crash UI.
            // Rendering hooks call this frequently, so avoid noisy stacktraces.
        }
    }

    private static void decorateEntry(KeyBindsScreen screen, Object entry, List<ViewBoardKeybindRules.KeyBindingState> states) {
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

            Button changeButton = buttonField(entry, "changeButton", "btnChangeKeyBinding");

            // --- collision logic ---
            boolean collision = hasEffectiveConflict(mapping, states);

            // --- hasCollision (optional field) ---
            try {
                Field hasCollisionField = clazz.getDeclaredField("hasCollision");
                hasCollisionField.setAccessible(true);
                hasCollisionField.setBoolean(entry, collision);
            } catch (NoSuchFieldException ignored) {
                // field removed/renamed in newer versions → ignore safely
            }

            applyButtonPresentation(screen, mapping, changeButton, collision, states);

        } catch (Exception e) {
            // Never crash UI; avoid noisy logs for render-time reflection.
        }
    }

    public static boolean hasEffectiveConflict(KeyMapping mapping, List<ViewBoardKeybindRules.KeyBindingState> states) {
        if (mapping.isUnbound()) {
            return false;
        }
        if (ViewBoardConflictHooks.isIgnored(mapping)) {
            return false;
        }

        ViewBoardKeybindRules.KeyBindingState state = states.stream()
            .filter(candidate -> Objects.equals(candidate.keybindId(), mapping.getName()))
            .findFirst()
            .orElse(null);

        if (state == null) {
            return false;
        }

        for (ViewBoardKeybindRules.KeyBindingState other : states) {
            if (Objects.equals(other.keybindId(), state.keybindId()) || other.ignored()) {
                continue;
            }

            if ((!Objects.equals(other.groupId(), state.groupId()) || state.groupId() == null)
                && ViewBoardConflictHooks.conflictsVisible(mapping, other.mapping())) {
                return true;
            }
        }

        return false;
    }

    public static void applyButtonPresentation(KeyBindsScreen screen, KeyMapping mapping, Button changeButton, boolean collision, List<ViewBoardKeybindRules.KeyBindingState> states) {
        changeButton.setTooltip(Tooltip.create(createTooltip(mapping, collision, states)));

        if (screen.selectedKey == mapping) {
            changeButton.setMessage(Component.literal("> ")
                .append(formatKeyMessage(mapping, collision).copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                .append(" <")
                .withStyle(ChatFormatting.YELLOW));
            return;
        }

        changeButton.setMessage(formatKeyMessage(mapping, collision));
    }

    public static Component formatKeyMessage(KeyMapping mapping, boolean collision) {
        Component baseMessage = mapping.getTranslatedKeyMessage();
        if (!collision) {
            return baseMessage;
        }

        return Component.literal("[")
            .append(baseMessage.copy().withStyle(ChatFormatting.WHITE))
            .append("]")
            .withStyle(ChatFormatting.RED);
    }

    public static Component createTooltip(KeyMapping mapping, boolean collision, List<ViewBoardKeybindRules.KeyBindingState> states) {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.translatable(mapping.getName()));

        if (ViewBoardConflictHooks.isIgnored(mapping)) {
            lines.add(Component.translatable("viewboard.tooltip.ignored"));
        }

        RULES.groupFor(mapping).ifPresent(group ->
            lines.add(Component.translatable("viewboard.tooltip.group",
                Component.literal(group.name())))
        );

        if (collision) {
            List<Component> conflicts = new ArrayList<>();
            ViewBoardKeybindRules.KeyBindingState current = states.stream()
                .filter(state -> Objects.equals(state.keybindId(), mapping.getName()))
                .findFirst()
                .orElse(null);

            for (ViewBoardKeybindRules.KeyBindingState state : states) {
                if (current != null
                    && current.groupId() != null
                    && Objects.equals(current.groupId(), state.groupId())) {
                    continue;
                }

                if (!Objects.equals(state.keybindId(), mapping.getName())
                    && !state.ignored()
                    && ViewBoardConflictHooks.conflictsVisible(mapping, state.mapping())) {
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

    public static KeyMapping mappingField(Object entry) throws ReflectiveOperationException {
        Field field = field(entry.getClass(), "key", "keyMapping");
        field.setAccessible(true);
        Object value = field.get(entry);
        return value instanceof KeyMapping mapping ? mapping : null;
    }

    public static Button buttonField(Object entry, String... names) throws ReflectiveOperationException {
        Field field = field(entry.getClass(), names);
        field.setAccessible(true);
        return (Button) field.get(entry);
    }

    public static Field field(Class<?> type, String... names) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            for (String name : names) {
                try {
                    return current.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                    // try next alias/superclass
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException(String.join("/", names));
    }
}
