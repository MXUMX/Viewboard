package com.mx.viewboard.client;

import com.mx.viewboard.ViewBoardMod;
import com.mx.viewboard.client.keybind.ControlsListWidthMode;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ViewBoardMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ViewBoardClientEvents {

    private static final Map<KeyBindsScreen, Button> KEYBOARD_VIEW_BUTTONS = new WeakHashMap<>();
    private static final Map<KeyBindsScreen, Button> WIDTH_BUTTONS = new WeakHashMap<>();
    private static final Map<KeyBindsScreen, Map<String, RowButtons>> CONTROLS_ROW_BUTTONS = new WeakHashMap<>();
    private static final ViewBoardKeybindRules RULES = ViewBoardKeybindRules.getInstance();

    private ViewBoardClientEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Button button = Button.builder(
            Component.translatable("viewboard.button.open"),
            clicked -> minecraft.setScreen(new KeyboardViewScreen(keyBindsScreen))
        ).bounds(keyBindsScreen.width - 106, keyBindsScreen.height - 27, 100, 20).build();

        event.addListener(button);
        KEYBOARD_VIEW_BUTTONS.put(keyBindsScreen, button);

        Button widthButton = Button.builder(widthButtonMessage(), clicked -> {
            RULES.cycleControlsListWidthMode();
            clicked.setMessage(widthButtonMessage());
            refreshControlsListLayout(keyBindsScreen);
        }).bounds(keyBindsScreen.width - 126, 6, 120, 20).build();
        event.addListener(widthButton);
        WIDTH_BUTTONS.put(keyBindsScreen, widthButton);

        try {
            attachControlsRowButtons(event, keyBindsScreen);
        } catch (Exception ignored) {
            // Avoid impacting vanilla screen if reflection fails.
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        // Keep the keyboard-view button pinned to bottom-right, even on resize.
        Button button = KEYBOARD_VIEW_BUTTONS.get(keyBindsScreen);
        if (button != null) {
            button.setPosition(keyBindsScreen.width - 106, keyBindsScreen.height - 27);
        }
        Button widthButton = WIDTH_BUTTONS.get(keyBindsScreen);
        if (widthButton != null) {
            widthButton.setPosition(keyBindsScreen.width - 126, 6);
            widthButton.setMessage(widthButtonMessage());
        }

        // Patch vanilla duplicate warnings + tooltip indicators using ViewBoard's effective rules.
        ControlsScreenBridge.decorate(keyBindsScreen);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        Map<String, RowButtons> byKey = CONTROLS_ROW_BUTTONS.get(keyBindsScreen);
        if (byKey == null || byKey.isEmpty()) {
            return;
        }

        // Position after vanilla has rendered the visible rows (so change/reset button Y is correct).
        positionControlsRowButtons(keyBindsScreen);

        for (RowButtons buttons : byKey.values()) {
            if (buttons.group().visible) {
                buttons.group().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }
            if (buttons.ignore().visible) {
                buttons.ignore().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null || mc.screen != null) {
            com.mx.viewboard.client.keybind.ViewBoardKeybindRules
                .getInstance()
                .syncRuntimeState();
        }
    }

    private static void attachControlsRowButtons(ScreenEvent.Init.Post event, KeyBindsScreen screen) throws Exception {
        Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
        listField.setAccessible(true);
        Object list = listField.get(screen);
        if (list == null) {
            return;
        }

        RULES.ensureLoaded();

        Map<String, RowButtons> byKey = new java.util.LinkedHashMap<>();

        for (Object entry : listChildren(list)) {
            Class<?> clazz = entry.getClass();
            KeyMapping mapping;
            try {
                mapping = ControlsScreenBridge.mappingField(entry);
            } catch (ReflectiveOperationException ignored) {
                continue;
            }
            if (mapping == null) {
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
                refreshControlsListLayout(screen);
            }).bounds(0, 0, 20, 20).build();
            // tooltip + message are refreshed each frame in positionControlsRowButtons

            event.addListener(groupButton);
            event.addListener(ignoreButton);

            byKey.put(mapping.getName(), new RowButtons(groupButton, ignoreButton));
        }

        CONTROLS_ROW_BUTTONS.put(screen, byKey);
    }

    private static void positionControlsRowButtons(KeyBindsScreen screen) {
        Map<String, RowButtons> byKey = CONTROLS_ROW_BUTTONS.get(screen);
        if (byKey == null || byKey.isEmpty()) {
            return;
        }

        Object list;
        try {
            Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            listField.setAccessible(true);
            Object rawList = listField.get(screen);
            if (rawList == null) {
                return;
            }
            list = rawList;
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
        int scrollBarX = intMethod(list, "getRowRight", screen.width - 28) + 8;
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
        for (Object entry : listChildren(list)) {
            try {
                // Compute the row's top/bottom exactly like vanilla's AbstractSelectionList#getRowTop.
                int listY = intMethod(list, "getY", 0);
                int rowTop = listY + 4 - (int) scrollAmount + index * itemHeight + headerHeight;
                int rowBottom = rowTop + itemHeight;
                if (rowBottom < listY || rowTop > intMethod(list, "getBottom", screen.height)) {
                    index++;
                    continue;
                }

                Class<?> clazz = entry.getClass();
                net.minecraft.client.KeyMapping mapping = ControlsScreenBridge.mappingField(entry);
                if (mapping == null) {
                    index++;
                    continue;
                }

                Button changeButton = ControlsScreenBridge.buttonField(entry, "changeButton", "btnChangeKeyBinding");
                Button resetButton = ControlsScreenBridge.buttonField(entry, "resetButton", "btnResetKeyBinding");

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
                int contentX = intMethod(list, "getRowLeft", 0) + 2;
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

    private static Component widthButtonMessage() {
        ControlsListWidthMode mode = RULES.controlsListWidthMode();
        return Component.translatable("viewboard.controls.width", Component.translatable(mode.translationKey()));
    }

    private static void refreshControlsListLayout(KeyBindsScreen screen) {
        try {
            Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            listField.setAccessible(true);
            Object rawList = listField.get(screen);
            if (rawList instanceof KeyBindsList list) {
                list.updateSizeAndPosition(list.getWidth(), list.getHeight(), list.getY());
                list.resetMappingAndUpdateButtons();
            }
        } catch (Exception ignored) {
            // Best-effort UI refresh only.
        }
    }

    private static java.util.List<?> listChildren(Object list) {
        try {
            Object value = list.getClass().getMethod("children").invoke(list);
            return value instanceof java.util.List<?> children ? children : java.util.List.of();
        } catch (ReflectiveOperationException ignored) {
            return java.util.List.of();
        }
    }

    private static int intMethod(Object target, String name, int fallback) {
        try {
            Object value = target.getClass().getMethod(name).invoke(target);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private record RowButtons(Button group, Button ignore) {
    }
}
