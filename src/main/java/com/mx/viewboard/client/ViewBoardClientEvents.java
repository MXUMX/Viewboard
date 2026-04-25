package com.mx.viewboard.client;

import com.mx.viewboard.ViewBoardMod;
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

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        // Handle custom row buttons (Group and Ignore buttons).
        Map<String, RowButtons> byKey = CONTROLS_ROW_BUTTONS.get(keyBindsScreen);
        if (byKey != null) {
            for (RowButtons buttons : byKey.values()) {
                if (buttons.group().visible && buttons.group().mouseClicked(mouseX, mouseY, event.getButton())) {
                    event.setCanceled(true);
                    return;
                }
                if (buttons.ignore().visible && buttons.ignore().mouseClicked(mouseX, mouseY, event.getButton())) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        // Handle the main "Open ViewBoard" button.
        Button keyboardViewButton = KEYBOARD_VIEW_BUTTONS.get(keyBindsScreen);
        if (keyboardViewButton != null && keyboardViewButton.mouseClicked(mouseX, mouseY, event.getButton())) {
            event.setCanceled(true);
        }
    }

    private static void attachControlsRowButtons(ScreenEvent.Init.Post event, KeyBindsScreen screen) throws Exception {
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
                    continue; // category entry
                }
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

            // Don't add to listeners - we render manually and route clicks via onMouseClick.

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
        double scrollAmount = readDouble(list, new String[] {"getScrollAmount", "scrollAmount"}, new String[] {"scrollAmount"});
        int headerHeight = readInt(list, new String[] {"getHeaderHeight"}, new String[] {"headerHeight"});

        int index = 0;
        for (Object entry : list.children()) {
            try {
                // Compute the row's top/bottom exactly like vanilla's AbstractSelectionList#getRowTop.
                int rowTop = list.getY() + 4 - (int) scrollAmount + index * itemHeight + headerHeight;
                int rowBottom = rowTop + itemHeight;
                if (rowBottom < list.getY() || rowTop > list.getBottom()) {
                    index++;
                    continue;
                }

                Class<?> clazz = entry.getClass();
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
                        index++;
                        continue; // category entry
                    }
                }
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
                    // This row wasn't present during init; create buttons lazily so newly visible rows
                    // get buttons when the list is scrolled.
                    Button groupButton = Button.builder(Component.literal("G"), clicked ->
                        Minecraft.getInstance().setScreen(new GroupEditorScreen(screen, mapping)))
                        .bounds(0, 0, 20, 20)
                        .build();
                    groupButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("viewboard.controls.button.group")));

                    Button ignoreButton = Button.builder(Component.literal("I"), clicked -> {
                        RULES.setIgnored(mapping, !RULES.isIgnored(mapping));
                    }).bounds(0, 0, 20, 20).build();

                    buttons = new RowButtons(groupButton, ignoreButton);
                    byKey.put(mapping.getName(), buttons);
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

    private static double readDouble(Object target, String[] methodNames, String[] fieldNames) {
        // Prefer a public/protected no-arg method if present (names vary by Minecraft version).
        for (String methodName : methodNames) {
            try {
                var method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof Number n) {
                    return n.doubleValue();
                }
            } catch (Exception ignored) {
                // try next
            }
        }

        for (String fieldName : fieldNames) {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Number n) {
                    return n.doubleValue();
                }
            } catch (Exception ignored) {
                // try next
            }
        }

        return 0.0;
    }

    private static int readInt(Object target, String[] methodNames, String[] fieldNames) {
        for (String methodName : methodNames) {
            try {
                var method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof Number n) {
                    return n.intValue();
                }
            } catch (Exception ignored) {
                // try next
            }
        }

        for (String fieldName : fieldNames) {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Number n) {
                    return n.intValue();
                }
            } catch (Exception ignored) {
                // try next
            }
        }

        return 0;
    }

    private static Field findField(Class<?> startClass, String fieldName) {
        Class<?> current = startClass;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private record RowButtons(Button group, Button ignore) {
    }
}
