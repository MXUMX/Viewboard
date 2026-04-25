package com.mx.viewboard.client;

import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.network.chat.Component;

public final class ViewBoardClientEvents {

    private static final Map<KeyBindsScreen, Button> KEYBOARD_VIEW_BUTTONS = new WeakHashMap<>();
    private static final Map<KeyBindsScreen, Map<String, RowButtons>> CONTROLS_ROW_BUTTONS = new WeakHashMap<>();
    private static final ViewBoardKeybindRules RULES = ViewBoardKeybindRules.getInstance();
    private static boolean registered;

    private ViewBoardClientEvents() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> onScreenInit(screen));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null || client.screen != null) {
                RULES.syncRuntimeState();
            }
        });
    }

    private static void onScreenInit(Screen screen) {
        if (!(screen instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Button button = Button.builder(
            Component.translatable("viewboard.button.open"),
            clicked -> minecraft.setScreen(new KeyboardViewScreen(keyBindsScreen))
        ).bounds(keyBindsScreen.width - 106, keyBindsScreen.height - 27, 100, 20).build();

        Screens.getButtons(keyBindsScreen).add(button);
        KEYBOARD_VIEW_BUTTONS.put(keyBindsScreen, button);

        try {
            attachControlsRowButtons(keyBindsScreen);
        } catch (Exception ignored) {
            // Avoid impacting vanilla screen if reflection fails.
        }

        ScreenEvents.beforeRender(keyBindsScreen).register((renderedScreen, guiGraphics, mouseX, mouseY, partialTick) -> {
            Button keyboardButton = KEYBOARD_VIEW_BUTTONS.get(keyBindsScreen);
            if (keyboardButton != null) {
                keyboardButton.setPosition(keyBindsScreen.width - 106, keyBindsScreen.height - 27);
            }

            hideControlsRowButtons(keyBindsScreen);
            ControlsScreenBridge.decorate(keyBindsScreen);
        });

        ScreenEvents.afterRender(keyBindsScreen).register((renderedScreen, guiGraphics, mouseX, mouseY, partialTick) -> {
            Map<String, RowButtons> byKey = CONTROLS_ROW_BUTTONS.get(keyBindsScreen);
            if (byKey == null || byKey.isEmpty()) {
                return;
            }

            positionControlsRowButtons(keyBindsScreen);

            for (RowButtons buttons : byKey.values()) {
                if (buttons.group().visible) {
                    buttons.group().render(guiGraphics, mouseX, mouseY, partialTick);
                }
                if (buttons.ignore().visible) {
                    buttons.ignore().render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }
        });
    }

    private static void hideControlsRowButtons(KeyBindsScreen screen) {
        Map<String, RowButtons> byKey = CONTROLS_ROW_BUTTONS.get(screen);
        if (byKey == null || byKey.isEmpty()) {
            return;
        }

        for (RowButtons buttons : byKey.values()) {
            buttons.group().visible = false;
            buttons.group().active = false;
            buttons.ignore().visible = false;
            buttons.ignore().active = false;
        }
    }

    private static void attachControlsRowButtons(KeyBindsScreen screen) throws Exception {
        Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
        listField.setAccessible(true);
        Object rawList = listField.get(screen);
        if (!(rawList instanceof KeyBindsList list)) {
            return;
        }

        RULES.ensureLoaded();

        Map<String, RowButtons> byKey = new java.util.LinkedHashMap<>();

        for (Object entry : list.children()) {
            Class<?> clazz = entry.getClass();
            Field mappingField;
            try {
                mappingField = clazz.getDeclaredField("key"); // 1.21.1
            } catch (NoSuchFieldException e) {
                continue; // category entry
            }
            mappingField.setAccessible(true);
            Object rawMapping = mappingField.get(entry);
            if (!(rawMapping instanceof net.minecraft.client.KeyMapping mapping)) {
                continue;
            }

            if (byKey.containsKey(mapping.getName())) {
                continue;
            }

            Button groupButton = Button.builder(Component.literal("G"), clicked ->
                Minecraft.getInstance().setScreen(new GroupEditorScreen(screen, mapping)))
                .bounds(0, 0, 20, 20)
                .build();
            groupButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("viewboard.controls.button.group")));

            Button ignoreButton = Button.builder(Component.literal("I"), clicked -> {
                RULES.setIgnored(mapping, !RULES.isIgnored(mapping));
            }).bounds(0, 0, 20, 20).build();
            // tooltip + message are refreshed each frame in positionControlsRowButtons

            Screens.getButtons(screen).add(groupButton);
            Screens.getButtons(screen).add(ignoreButton);

            byKey.put(mapping.getName(), new RowButtons(groupButton, ignoreButton));
        }

        CONTROLS_ROW_BUTTONS.put(screen, byKey);
    }

    private static void positionControlsRowButtons(KeyBindsScreen screen) {
        Map<String, RowButtons> byKey = CONTROLS_ROW_BUTTONS.get(screen);
        if (byKey == null || byKey.isEmpty()) {
            return;
        }

        KeyBindsList list;
        try {
            Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            listField.setAccessible(true);
            Object rawList = listField.get(screen);
            if (!(rawList instanceof KeyBindsList tmp)) {
                return;
            }
            list = tmp;
        } catch (Exception ignored) {
            return;
        }

        RULES.ensureLoaded();

        // Hide everything by default; we'll re-enable visible rows.
        for (RowButtons buttons : byKey.values()) {
            buttons.group().visible = false;
            buttons.group().active = false;
            buttons.ignore().visible = false;
            buttons.ignore().active = false;
        }

        // Vanilla 1.21.1: scrollBarX() == getRowRight() + 6 + 2
        int scrollBarX = list.getRowRight() + 8;
        int itemHeight = 20;
        int headerHeight = 0;
        double scrollAmount = 0.0;

        // Prefer public API if present, but fall back to reflection if needed.
        try {
            var getScrollAmount = list.getClass().getMethod("getScrollAmount");
            Object value = getScrollAmount.invoke(list);
            if (value instanceof Number n) {
                scrollAmount = n.doubleValue();
            }
        } catch (Exception ignored) {
            try {
                var scrollAmountField = list.getClass().getSuperclass().getDeclaredField("scrollAmount");
                scrollAmountField.setAccessible(true);
                scrollAmount = scrollAmountField.getDouble(list);
            } catch (Exception ignored2) {
                // best-effort; 0.0 is fine
            }
        }

        try {
            var headerHeightField = list.getClass().getSuperclass().getDeclaredField("headerHeight");
            headerHeightField.setAccessible(true);
            headerHeight = headerHeightField.getInt(list);
        } catch (Exception ignored) {
            headerHeight = 0;
        }

        int index = 0;
        for (Object entry : list.children()) {
            try {
                // Compute the row's top/bottom exactly like vanilla's AbstractSelectionList#getRowTop.
                int listTop = getListTop(list);
                int rowTop = listTop + 4 - (int) scrollAmount + index * itemHeight + headerHeight;
                int rowBottom = rowTop + itemHeight;
                if (rowBottom < listTop || rowTop > getListBottom(list)) {
                    index++;
                    continue;
                }

                Class<?> clazz = entry.getClass();
                Field mappingField = clazz.getDeclaredField("key");
                mappingField.setAccessible(true);
                Object rawMapping = mappingField.get(entry);
                if (!(rawMapping instanceof net.minecraft.client.KeyMapping mapping)) {
                    index++;
                    continue;
                }

                Field changeButtonField = clazz.getDeclaredField("changeButton");
                changeButtonField.setAccessible(true);
                Button changeButton = (Button) changeButtonField.get(entry);

                Field resetButtonField = clazz.getDeclaredField("resetButton");
                resetButtonField.setAccessible(true);
                Button resetButton = (Button) resetButtonField.get(entry);

                RowButtons buttons = byKey.get(mapping.getName());
                if (buttons == null) {
                    index++;
                    continue;
                }

                int iconW = 20;
                int gap = 2;

                // Prefer vanilla's own button Y for perfect alignment (it is set during KeyEntry.renderContent()).
                int y = changeButton.getY();
                if (y <= 0) {
                    y = rowTop;
                }
                // Vanilla math:
                // i = scrollBarX - resetW - 10
                // j = getContentY() - 2
                // k = i - 5 - changeW
                int resetW = resetButton.getWidth();
                int changeW = changeButton.getWidth();
                int i = scrollBarX - resetW - 10;
                int changeX = i - 5 - changeW;

                int ignoreX = changeX - gap - iconW;
                int groupX = ignoreX - gap - iconW;

                // Keep icons from overlapping the key name area (per-row width, not global max).
                // KeyBindsList entries are anchored to list.getRowLeft(), so contentX is stable.
                int contentX = list.getRowLeft() + 2;
                int nameRight = contentX + 120; // conservative fallback if reflection fails
                try {
                    Field nameField = clazz.getDeclaredField("name");
                    nameField.setAccessible(true);
                    Object rawName = nameField.get(entry);
                    if (rawName instanceof net.minecraft.network.chat.Component nameComponent) {
                        nameRight = contentX + Minecraft.getInstance().font.width(nameComponent);
                    }
                } catch (Exception ignored) {
                    // keep fallback
                }

                if (groupX < nameRight + 4 || ignoreX + iconW > changeX - 1) {
                    continue;
                }

                buttons.group().setPosition(groupX, y);
                buttons.group().visible = true;
                buttons.group().active = true;

                buttons.ignore().setPosition(ignoreX, y);
                buttons.ignore().visible = true;
                buttons.ignore().active = true;

                boolean ignored = RULES.isIgnored(mapping);
                buttons.ignore().setMessage(Component.literal(ignored ? "!" : "I"));
                buttons.ignore().setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    ignored
                        ? Component.translatable("viewboard.controls.button.ignore_on")
                        : Component.translatable("viewboard.controls.button.ignore_off")
                ));
            } catch (Exception ignored) {
                // Keep rendering even if one entry changed shape.
            }

            index++;
        }
    }

    private static int getListTop(KeyBindsList list) {
        return getListBound(list, "y0");
    }

    private static int getListBottom(KeyBindsList list) {
        return getListBound(list, "y1");
    }

    private static int getListBound(KeyBindsList list, String fieldName) {
        Class<?> current = list.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.getInt(list);
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        return 0;
    }

    private record RowButtons(Button group, Button ignore) {
    }
}
