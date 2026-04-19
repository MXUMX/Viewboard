package com.mx.viewboard.client.layout;

import com.mx.viewboard.client.keybind.KeyboardLayoutId;
import com.mx.viewboard.client.keybind.SerializedKey;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

public final class KeyboardLayout {
    private static final String SECTION_F13 = "f13";
    private static final String SECTION_FUNCTIONS = "functions";
    private static final String SECTION_MAIN = "main";
    private static final String SECTION_NAVIGATION = "navigation";
    private static final String SECTION_ARROWS = "arrows";
    private static final String SECTION_NUMPAD = "numpad";
    private static final String SECTION_MOUSE = "mouse";
    private static final String SECTION_CUSTOM = "custom";

    private static final List<SlotDefinition> BASE_SLOTS = createBaseSlots();

    private KeyboardLayout() {
    }

    public static KeyboardViewModel build(KeyboardLayoutId layoutId, List<SerializedKey> customKeys) {
        List<VisualKey> keys = new ArrayList<>();
        Set<SerializedKey> representedKeys = new LinkedHashSet<>();

        for (SlotDefinition slot : BASE_SLOTS) {
            ResolvedKey resolved = resolve(layoutId, slot);
            keys.add(new VisualKey(
                slot.id(),
                slot.sectionId(),
                resolved.key(),
                resolved.label(),
                slot.xUnits(),
                slot.yUnits(),
                slot.widthUnits(),
                1.0F,
                false
            ));
            representedKeys.add(resolved.key());
        }

        float customY = 10.0F;
        float customX = 0.0F;
        for (int index = 0; index < customKeys.size(); index++) {
            SerializedKey key = customKeys.get(index);
            float width = key.type() == InputConstants.Type.SCANCODE ? 1.8F : 1.4F;
            if (customX + width > 26.2F) {
                customX = 0.0F;
                customY += 1.2F;
            }

            keys.add(new VisualKey(
                "custom-" + index,
                SECTION_CUSTOM,
                key,
                key.displayLabel(),
                customX,
                customY,
                width,
                1.0F,
                true
            ));
            representedKeys.add(key);
            customX += width + 0.2F;
        }

        List<VisualSection> sections = createSections(keys);
        Bounds bounds = bounds(keys, sections);
        return new KeyboardViewModel(List.copyOf(sections), List.copyOf(keys), Set.copyOf(representedKeys), bounds.width(), bounds.height());
    }

    private static ResolvedKey resolve(KeyboardLayoutId layoutId, SlotDefinition slot) {
        KeyboardLayoutSpec.LayoutKey resolved = KeyboardLayoutSpec.resolve(
            layoutId,
            slot.id(),
            slot.defaultKey().asConfigString(),
            slot.defaultLabel()
        );
        return new ResolvedKey(SerializedKey.parse(resolved.serializedKey()), resolved.label());
    }

    private static List<SlotDefinition> createBaseSlots() {
        List<SlotDefinition> slots = new ArrayList<>();

        addRow(slots, SECTION_F13, 4.8F, 0.0F, new Object[][] {
            {"f13", GLFW.GLFW_KEY_F13, "F13", 1.0F},
            {"f14", GLFW.GLFW_KEY_F14, "F14", 1.0F},
            {"f15", GLFW.GLFW_KEY_F15, "F15", 1.0F},
            {"f16", GLFW.GLFW_KEY_F16, "F16", 1.0F},
            {"f17", GLFW.GLFW_KEY_F17, "F17", 1.0F},
            {"f18", GLFW.GLFW_KEY_F18, "F18", 1.0F},
            {"f19", GLFW.GLFW_KEY_F19, "F19", 1.0F},
            {"f20", GLFW.GLFW_KEY_F20, "F20", 1.0F},
            {"f21", GLFW.GLFW_KEY_F21, "F21", 1.0F},
            {"f22", GLFW.GLFW_KEY_F22, "F22", 1.0F},
            {"f23", GLFW.GLFW_KEY_F23, "F23", 1.0F},
            {"f24", GLFW.GLFW_KEY_F24, "F24", 1.0F},
            {"f25", GLFW.GLFW_KEY_F25, "F25", 1.0F}
        });

        addRow(slots, SECTION_FUNCTIONS, 0.0F, 1.4F, new Object[][] {
            {"escape", GLFW.GLFW_KEY_ESCAPE, "Esc", 1.0F}
        });
        addRow(slots, SECTION_FUNCTIONS, 2.6F, 1.4F, new Object[][] {
            {"f1", GLFW.GLFW_KEY_F1, "F1", 1.0F},
            {"f2", GLFW.GLFW_KEY_F2, "F2", 1.0F},
            {"f3", GLFW.GLFW_KEY_F3, "F3", 1.0F},
            {"f4", GLFW.GLFW_KEY_F4, "F4", 1.0F}
        });
        addRow(slots, SECTION_FUNCTIONS, 7.6F, 1.4F, new Object[][] {
            {"f5", GLFW.GLFW_KEY_F5, "F5", 1.0F},
            {"f6", GLFW.GLFW_KEY_F6, "F6", 1.0F},
            {"f7", GLFW.GLFW_KEY_F7, "F7", 1.0F},
            {"f8", GLFW.GLFW_KEY_F8, "F8", 1.0F}
        });
        addRow(slots, SECTION_FUNCTIONS, 12.6F, 1.4F, new Object[][] {
            {"f9", GLFW.GLFW_KEY_F9, "F9", 1.0F},
            {"f10", GLFW.GLFW_KEY_F10, "F10", 1.0F},
            {"f11", GLFW.GLFW_KEY_F11, "F11", 1.0F},
            {"f12", GLFW.GLFW_KEY_F12, "F12", 1.0F}
        });
        addRow(slots, SECTION_FUNCTIONS, 18.0F, 1.4F, new Object[][] {
            {"printscreen", GLFW.GLFW_KEY_PRINT_SCREEN, "PrtSc", 1.1F},
            {"scrolllock", GLFW.GLFW_KEY_SCROLL_LOCK, "ScrLk", 1.1F},
            {"pause", GLFW.GLFW_KEY_PAUSE, "Pause", 1.1F}
        });

        addRow(slots, SECTION_MAIN, 0.0F, 2.6F, new Object[][] {
            {"grave", GLFW.GLFW_KEY_GRAVE_ACCENT, "`", 1.0F},
            {"digit1", GLFW.GLFW_KEY_1, "1", 1.0F},
            {"digit2", GLFW.GLFW_KEY_2, "2", 1.0F},
            {"digit3", GLFW.GLFW_KEY_3, "3", 1.0F},
            {"digit4", GLFW.GLFW_KEY_4, "4", 1.0F},
            {"digit5", GLFW.GLFW_KEY_5, "5", 1.0F},
            {"digit6", GLFW.GLFW_KEY_6, "6", 1.0F},
            {"digit7", GLFW.GLFW_KEY_7, "7", 1.0F},
            {"digit8", GLFW.GLFW_KEY_8, "8", 1.0F},
            {"digit9", GLFW.GLFW_KEY_9, "9", 1.0F},
            {"digit0", GLFW.GLFW_KEY_0, "0", 1.0F},
            {"minus", GLFW.GLFW_KEY_MINUS, "-", 1.0F},
            {"equals", GLFW.GLFW_KEY_EQUAL, "=", 1.0F},
            {"backspace", GLFW.GLFW_KEY_BACKSPACE, "Back", 2.2F}
        });
        addRow(slots, SECTION_NAVIGATION, 17.8F, 2.6F, new Object[][] {
            {"insert", GLFW.GLFW_KEY_INSERT, "Ins", 1.0F},
            {"home", GLFW.GLFW_KEY_HOME, "Home", 1.0F},
            {"pageup", GLFW.GLFW_KEY_PAGE_UP, "PgUp", 1.0F}
        });
        addRow(slots, SECTION_NUMPAD, 21.8F, 2.6F, new Object[][] {
            {"numlock", GLFW.GLFW_KEY_NUM_LOCK, "Num", 1.0F},
            {"kpdivide", GLFW.GLFW_KEY_KP_DIVIDE, "K/", 1.0F},
            {"kpmultiply", GLFW.GLFW_KEY_KP_MULTIPLY, "K*", 1.0F},
            {"kpsubtract", GLFW.GLFW_KEY_KP_SUBTRACT, "K-", 1.0F}
        });

        addRow(slots, SECTION_MAIN, 0.0F, 3.8F, new Object[][] {
            {"tab", GLFW.GLFW_KEY_TAB, "Tab", 1.5F},
            {"q", GLFW.GLFW_KEY_Q, "Q", 1.0F},
            {"w", GLFW.GLFW_KEY_W, "W", 1.0F},
            {"e", GLFW.GLFW_KEY_E, "E", 1.0F},
            {"r", GLFW.GLFW_KEY_R, "R", 1.0F},
            {"t", GLFW.GLFW_KEY_T, "T", 1.0F},
            {"y", GLFW.GLFW_KEY_Y, "Y", 1.0F},
            {"u", GLFW.GLFW_KEY_U, "U", 1.0F},
            {"i", GLFW.GLFW_KEY_I, "I", 1.0F},
            {"o", GLFW.GLFW_KEY_O, "O", 1.0F},
            {"p", GLFW.GLFW_KEY_P, "P", 1.0F},
            {"leftbracket", GLFW.GLFW_KEY_LEFT_BRACKET, "[", 1.0F},
            {"rightbracket", GLFW.GLFW_KEY_RIGHT_BRACKET, "]", 1.0F},
            {"backslash", GLFW.GLFW_KEY_BACKSLASH, "\\", 1.7F}
        });
        addRow(slots, SECTION_NAVIGATION, 17.8F, 3.8F, new Object[][] {
            {"delete", GLFW.GLFW_KEY_DELETE, "Del", 1.0F},
            {"end", GLFW.GLFW_KEY_END, "End", 1.0F},
            {"pagedown", GLFW.GLFW_KEY_PAGE_DOWN, "PgDn", 1.0F}
        });
        addRow(slots, SECTION_NUMPAD, 21.8F, 3.8F, new Object[][] {
            {"kp7", GLFW.GLFW_KEY_KP_7, "K7", 1.0F},
            {"kp8", GLFW.GLFW_KEY_KP_8, "K8", 1.0F},
            {"kp9", GLFW.GLFW_KEY_KP_9, "K9", 1.0F},
            {"kpadd", GLFW.GLFW_KEY_KP_ADD, "K+", 1.0F}
        });

        addRow(slots, SECTION_MAIN, 0.0F, 5.0F, new Object[][] {
            {"capslock", GLFW.GLFW_KEY_CAPS_LOCK, "Caps", 1.8F},
            {"a", GLFW.GLFW_KEY_A, "A", 1.0F},
            {"s", GLFW.GLFW_KEY_S, "S", 1.0F},
            {"d", GLFW.GLFW_KEY_D, "D", 1.0F},
            {"f", GLFW.GLFW_KEY_F, "F", 1.0F},
            {"g", GLFW.GLFW_KEY_G, "G", 1.0F},
            {"h", GLFW.GLFW_KEY_H, "H", 1.0F},
            {"j", GLFW.GLFW_KEY_J, "J", 1.0F},
            {"k", GLFW.GLFW_KEY_K, "K", 1.0F},
            {"l", GLFW.GLFW_KEY_L, "L", 1.0F},
            {"semicolon", GLFW.GLFW_KEY_SEMICOLON, ";", 1.0F},
            {"apostrophe", GLFW.GLFW_KEY_APOSTROPHE, "'", 1.0F},
            {"enter", GLFW.GLFW_KEY_ENTER, "Enter", 2.6F}
        });
        addRow(slots, SECTION_NUMPAD, 21.8F, 5.0F, new Object[][] {
            {"kp4", GLFW.GLFW_KEY_KP_4, "K4", 1.0F},
            {"kp5", GLFW.GLFW_KEY_KP_5, "K5", 1.0F},
            {"kp6", GLFW.GLFW_KEY_KP_6, "K6", 1.0F},
            {"kpequal", GLFW.GLFW_KEY_KP_EQUAL, "K=", 1.0F}
        });

        addRow(slots, SECTION_MAIN, 0.0F, 6.2F, new Object[][] {
            {"leftshift", GLFW.GLFW_KEY_LEFT_SHIFT, "LShift", 2.2F},
            {"world1", GLFW.GLFW_KEY_WORLD_1, "W1", 1.0F},
            {"z", GLFW.GLFW_KEY_Z, "Z", 1.0F},
            {"x", GLFW.GLFW_KEY_X, "X", 1.0F},
            {"c", GLFW.GLFW_KEY_C, "C", 1.0F},
            {"v", GLFW.GLFW_KEY_V, "V", 1.0F},
            {"b", GLFW.GLFW_KEY_B, "B", 1.0F},
            {"n", GLFW.GLFW_KEY_N, "N", 1.0F},
            {"m", GLFW.GLFW_KEY_M, "M", 1.0F},
            {"comma", GLFW.GLFW_KEY_COMMA, ",", 1.0F},
            {"period", GLFW.GLFW_KEY_PERIOD, ".", 1.0F},
            {"slash", GLFW.GLFW_KEY_SLASH, "/", 1.0F},
            {"rightshift", GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift", 2.2F}
        });
        addRow(slots, SECTION_ARROWS, 19.0F, 6.2F, new Object[][] {
            {"up", GLFW.GLFW_KEY_UP, "Up", 1.0F}
        });
        addRow(slots, SECTION_NUMPAD, 21.8F, 6.2F, new Object[][] {
            {"kp1", GLFW.GLFW_KEY_KP_1, "K1", 1.0F},
            {"kp2", GLFW.GLFW_KEY_KP_2, "K2", 1.0F},
            {"kp3", GLFW.GLFW_KEY_KP_3, "K3", 1.0F},
            {"kpenter", GLFW.GLFW_KEY_KP_ENTER, "KEnt", 1.0F}
        });

        addRow(slots, SECTION_MAIN, 0.0F, 7.4F, new Object[][] {
            {"leftcontrol", GLFW.GLFW_KEY_LEFT_CONTROL, "LCtrl", 1.4F},
            {"leftsuper", GLFW.GLFW_KEY_LEFT_SUPER, "LSuper", 1.2F},
            {"leftalt", GLFW.GLFW_KEY_LEFT_ALT, "LAlt", 1.2F},
            {"space", GLFW.GLFW_KEY_SPACE, "Space", 5.9F},
            {"rightalt", GLFW.GLFW_KEY_RIGHT_ALT, "RAlt", 1.2F},
            {"world2", GLFW.GLFW_KEY_WORLD_2, "W2", 1.0F},
            {"rightsuper", GLFW.GLFW_KEY_RIGHT_SUPER, "RSuper", 1.2F},
            {"menu", GLFW.GLFW_KEY_MENU, "Menu", 1.2F},
            {"rightcontrol", GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl", 1.4F}
        });
        addRow(slots, SECTION_ARROWS, 17.8F, 7.4F, new Object[][] {
            {"left", GLFW.GLFW_KEY_LEFT, "Left", 1.0F},
            {"down", GLFW.GLFW_KEY_DOWN, "Down", 1.0F},
            {"right", GLFW.GLFW_KEY_RIGHT, "Right", 1.0F}
        });
        addRow(slots, SECTION_NUMPAD, 21.8F, 7.4F, new Object[][] {
            {"kp0", GLFW.GLFW_KEY_KP_0, "K0", 2.2F},
            {"kpdecimal", GLFW.GLFW_KEY_KP_DECIMAL, "K.", 1.0F}
        });

        addRow(slots, SECTION_MOUSE, 18.0F, 8.8F, new Object[][] {
            {"mouse-left", -100, "LButton", 1.6F},
            {"mouse-right", -99, "RButton", 1.6F},
            {"mouse-middle", -98, "MButton", 1.6F},
            {"mouse-x1", -97, "XButton1", 1.8F},
            {"mouse-x2", -96, "XButton2", 1.8F}
        });

        return List.copyOf(slots);
    }

    private static List<VisualSection> createSections(List<VisualKey> keys) {
        Map<String, SectionBounds> boundsBySection = new LinkedHashMap<>();
        for (VisualKey key : keys) {
            boundsBySection.computeIfAbsent(key.sectionId(), unused -> new SectionBounds())
                .include(key.xUnits(), key.yUnits(), key.widthUnits(), key.heightUnits());
        }

        List<VisualSection> sections = new ArrayList<>();
        for (Map.Entry<String, SectionBounds> entry : boundsBySection.entrySet()) {
            SectionBounds bounds = entry.getValue();
            sections.add(new VisualSection(
                entry.getKey(),
                bounds.minX - 0.25F,
                bounds.minY - 0.25F,
                bounds.width() + 0.5F,
                bounds.height() + 0.5F
            ));
        }
        return sections;
    }

    private static Bounds bounds(List<VisualKey> keys, List<VisualSection> sections) {
        float maxX = 0.0F;
        float maxY = 0.0F;
        for (VisualKey key : keys) {
            maxX = Math.max(maxX, key.xUnits() + key.widthUnits());
            maxY = Math.max(maxY, key.yUnits() + key.heightUnits());
        }
        for (VisualSection section : sections) {
            maxX = Math.max(maxX, section.xUnits() + section.widthUnits());
            maxY = Math.max(maxY, section.yUnits() + section.heightUnits());
        }
        return new Bounds(maxX + 0.5F, maxY + 0.5F);
    }

    private static void addRow(List<SlotDefinition> slots, String sectionId, float startX, float y, Object[][] row) {
        float x = startX;
        for (Object[] key : row) {
            String slotId = (String) key[0];
            int value = (int) key[1];
            String label = (String) key[2];
            float width = (float) key[3];
            InputConstants.Type type = value <= -96 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
            int keyValue = value <= -96 ? Math.abs(value + 100) : value;
            slots.add(new SlotDefinition(slotId, sectionId, new SerializedKey(type, keyValue), label, x, y, width));
            x += width + 0.2F;
        }
    }

    private record SlotDefinition(
        String id,
        String sectionId,
        SerializedKey defaultKey,
        String defaultLabel,
        float xUnits,
        float yUnits,
        float widthUnits
    ) {
    }

    private record ResolvedKey(SerializedKey key, String label) {
    }

    private record Bounds(float width, float height) {
    }

    private static final class SectionBounds {
        private float minX = Float.MAX_VALUE;
        private float minY = Float.MAX_VALUE;
        private float maxX = Float.MIN_VALUE;
        private float maxY = Float.MIN_VALUE;

        private void include(float x, float y, float width, float height) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.maxX = Math.max(this.maxX, x + width);
            this.maxY = Math.max(this.maxY, y + height);
        }

        private float width() {
            return this.maxX - this.minX;
        }

        private float height() {
            return this.maxY - this.minY;
        }
    }
}
