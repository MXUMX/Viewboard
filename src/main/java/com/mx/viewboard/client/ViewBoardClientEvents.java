package com.mx.viewboard.client;

import com.mx.viewboard.ViewBoardMod;
import com.mx.viewboard.client.keybind.ControlsListWidthMode;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ViewBoardMod.MOD_ID, value = Dist.CLIENT)
public final class ViewBoardClientEvents {

    private static final Map<KeyBindsScreen, Button> KEYBOARD_VIEW_BUTTONS = new WeakHashMap<>();
    private static final Map<KeyBindsScreen, Button> WIDTH_BUTTONS = new WeakHashMap<>();
    private static final Map<KeyBindsScreen, List<RowAction>> CONTROLS_ROW_ACTIONS = new WeakHashMap<>();
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

        CONTROLS_ROW_ACTIONS.put(keyBindsScreen, new ArrayList<>());
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

        // Position after vanilla has rendered the visible rows (so change/reset button Y is correct).
        List<RowAction> actions = positionControlsRowActions(keyBindsScreen);
        ClipBounds clipBounds = controlsListClipBounds(keyBindsScreen);
        if (clipBounds == null) {
            return;
        }

        Component hoveredTooltip = null;
        event.getGuiGraphics().enableScissor(clipBounds.left(), clipBounds.top(), clipBounds.right(), clipBounds.bottom());
        for (RowAction action : actions) {
            renderRowAction(event.getGuiGraphics(), action, event.getMouseX(), event.getMouseY());
            if (clipBounds.contains(event.getMouseX(), event.getMouseY()) && action.contains(event.getMouseX(), event.getMouseY())) {
                hoveredTooltip = action.tooltip();
            }
        }
        event.getGuiGraphics().disableScissor();

        if (hoveredTooltip != null) {
            event.getGuiGraphics().setComponentTooltipForNextFrame(
                Minecraft.getInstance().font,
                java.util.List.of(hoveredTooltip),
                event.getMouseX(),
                event.getMouseY()
            );
            event.getGuiGraphics().renderDeferredElements();
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        if (dispatchRowActionClick(keyBindsScreen, event.getMouseButtonEvent())) {
            event.setCanceled(true);
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

    private static boolean dispatchRowActionClick(KeyBindsScreen screen, MouseButtonEvent event) {
        ClipBounds clipBounds = controlsListClipBounds(screen);
        if (clipBounds == null || !clipBounds.contains(event.x(), event.y())) {
            return false;
        }

        for (RowAction action : positionControlsRowActions(screen)) {
            if (action.contains(event.x(), event.y())) {
                performRowAction(screen, action);
                return true;
            }
        }

        return false;
    }

    private static List<RowAction> positionControlsRowActions(KeyBindsScreen screen) {
        Object list;
        try {
            Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            listField.setAccessible(true);
            Object rawList = listField.get(screen);
            if (rawList == null) {
                return List.of();
            }
            list = rawList;
        } catch (Exception ignored) {
            return List.of();
        }

        RULES.ensureLoaded();
        List<RowAction> actions = CONTROLS_ROW_ACTIONS.computeIfAbsent(screen, unused -> new ArrayList<>());
        actions.clear();

        int listY = intMethod(list, "getY", 0);
        int listBottom = intMethod(list, "getBottom", screen.height);
        int listLeft = intMethod(list, "getRowLeft", 0);
        int index = 0;
        for (Object entry : listChildren(list)) {
            try {
                Class<?> clazz = entry.getClass();
                net.minecraft.client.KeyMapping mapping = ControlsScreenBridge.mappingField(entry);
                if (mapping == null) {
                    index++;
                    continue;
                }

                Button changeButton = ControlsScreenBridge.buttonField(entry, "changeButton", "btnChangeKeyBinding");
                Button resetButton = ControlsScreenBridge.buttonField(entry, "resetButton", "btnResetKeyBinding");
                int rowTop = intMethod(list, "getRowTop", index, Integer.MIN_VALUE);
                int rowBottom = intMethod(list, "getRowBottom", index, Integer.MIN_VALUE);
                if (rowTop == Integer.MIN_VALUE || rowBottom == Integer.MIN_VALUE) {
                    rowTop = intMethod(entry, "getY", Integer.MIN_VALUE);
                    int rowHeight = intMethod(entry, "getHeight", Math.max(changeButton.getHeight(), resetButton.getHeight()));
                    if (rowTop == Integer.MIN_VALUE) {
                        rowTop = changeButton.getY();
                    }
                    if (rowHeight <= 0) {
                        rowHeight = 20;
                    }
                    rowBottom = rowTop + rowHeight;
                }
                if (rowTop <= 0 || rowBottom < listY || rowTop > listBottom) {
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
                int changeX = changeButton.getX();

                int ignoreX = changeX - gap - iconW;
                int groupX = ignoreX - gap - iconW;

                // Keep icons from overlapping the key name area (per-row width, not global max).
                // KeyBindsList entries are anchored to list.getRowLeft(), so contentX is stable.
                int contentX = listLeft + 2;
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

                boolean ignored = RULES.isIgnored(mapping);
                actions.add(new RowAction(mapping, RowActionType.GROUP, groupX, y, iconW, iconW, Component.literal("G"), Component.translatable("viewboard.controls.button.group")));
                actions.add(new RowAction(
                    mapping,
                    RowActionType.IGNORE,
                    ignoreX,
                    y,
                    iconW,
                    iconW,
                    Component.literal(ignored ? "!" : "I"),
                    ignored
                        ? Component.translatable("viewboard.controls.button.ignore_on")
                        : Component.translatable("viewboard.controls.button.ignore_off")
                ));
            } catch (Exception ignored) {
                // Keep rendering even if one entry changed shape.
            }

            index++;
        }
        return actions;
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
                list.updateSizeAndPosition(list.getWidth(), list.getHeight(), list.getX(), list.getY());
                list.resetMappingAndUpdateButtons();
            }
        } catch (Exception ignored) {
            // Best-effort UI refresh only.
        }
    }

    private static ClipBounds controlsListClipBounds(KeyBindsScreen screen) {
        try {
            Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            listField.setAccessible(true);
            Object list = listField.get(screen);
            if (list == null) {
                return null;
            }

            int left = intMethod(list, "getX", 0);
            int top = intMethod(list, "getY", 0);
            int right = intMethod(list, "getRight", screen.width);
            int bottom = intMethod(list, "getBottom", screen.height);
            if (right <= left || bottom <= top) {
                return null;
            }
            return new ClipBounds(left, top, right, bottom);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void renderRowAction(GuiGraphics graphics, RowAction action, int mouseX, int mouseY) {
        boolean hovered = action.contains(mouseX, mouseY);
        int fill = hovered ? 0xFF4A5568 : 0xFF242A33;
        int border = hovered ? 0xFFFFFFFF : 0xFF7A8493;
        graphics.fill(action.x(), action.y(), action.x() + action.width(), action.y() + action.height(), fill);
        graphics.fill(action.x(), action.y(), action.x() + action.width(), action.y() + 1, border);
        graphics.fill(action.x(), action.y() + action.height() - 1, action.x() + action.width(), action.y() + action.height(), border);
        graphics.fill(action.x(), action.y(), action.x() + 1, action.y() + action.height(), border);
        graphics.fill(action.x() + action.width() - 1, action.y(), action.x() + action.width(), action.y() + action.height(), border);

        int textX = action.x() + (action.width() - Minecraft.getInstance().font.width(action.label())) / 2;
        int textY = action.y() + 6;
        graphics.drawString(Minecraft.getInstance().font, action.label(), textX, textY, 0xFFFFFFFF, false);
    }

    private static void performRowAction(KeyBindsScreen screen, RowAction action) {
        switch (action.type()) {
            case GROUP -> Minecraft.getInstance().setScreen(new GroupEditorScreen(screen, action.mapping()));
            case IGNORE -> {
                RULES.setIgnored(action.mapping(), !RULES.isIgnored(action.mapping()));
                refreshControlsListLayout(screen);
            }
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

    private static int intMethod(Object target, String name, int argument, int fallback) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                var method = current.getDeclaredMethod(name, int.class);
                method.setAccessible(true);
                Object value = method.invoke(target, argument);
                return value instanceof Number number ? number.intValue() : fallback;
            } catch (ReflectiveOperationException ignored) {
                current = current.getSuperclass();
            }
        }
        return fallback;
    }

    private enum RowActionType {
        GROUP,
        IGNORE
    }

    private record RowAction(KeyMapping mapping, RowActionType type, int x, int y, int width, int height, Component label, Component tooltip) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record ClipBounds(int left, int top, int right, int bottom) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.left && mouseX < this.right && mouseY >= this.top && mouseY < this.bottom;
        }
    }
}
