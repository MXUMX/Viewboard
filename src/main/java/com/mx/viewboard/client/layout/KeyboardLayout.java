package com.mx.viewboard.client.layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public final class KeyboardLayout {
    private static final String SECTION_F13 = "f13";
    private static final String SECTION_FUNCTIONS = "functions";
    private static final String SECTION_MAIN = "main";
    private static final String SECTION_NAVIGATION = "navigation";
    private static final String SECTION_ARROWS = "arrows";
    private static final String SECTION_NUMPAD = "numpad";
    private static final List<KeyboardKey> KEYS = createKeys();
    private static final List<KeyboardSection> SECTIONS = createSections(KEYS);
    private static final float TOTAL_WIDTH_UNITS = 26.2F;
    private static final float TOTAL_HEIGHT_UNITS = 9.0F;

    private KeyboardLayout() {
    }

    public static List<KeyboardKey> keys() {
        return KEYS;
    }

    public static float totalWidthUnits() {
        return TOTAL_WIDTH_UNITS;
    }

    public static float totalHeightUnits() {
        return TOTAL_HEIGHT_UNITS;
    }

    public static List<KeyboardSection> sections() {
        return SECTIONS;
    }

    private static List<KeyboardKey> createKeys() {
        List<KeyboardKey> keys = new ArrayList<>();

        addRow(keys, SECTION_F13, 4.8F, 0.0F, new Object[][] {
            {GLFW.GLFW_KEY_F13, "F13", 1.0F},
            {GLFW.GLFW_KEY_F14, "F14", 1.0F},
            {GLFW.GLFW_KEY_F15, "F15", 1.0F},
            {GLFW.GLFW_KEY_F16, "F16", 1.0F},
            {GLFW.GLFW_KEY_F17, "F17", 1.0F},
            {GLFW.GLFW_KEY_F18, "F18", 1.0F},
            {GLFW.GLFW_KEY_F19, "F19", 1.0F},
            {GLFW.GLFW_KEY_F20, "F20", 1.0F},
            {GLFW.GLFW_KEY_F21, "F21", 1.0F},
            {GLFW.GLFW_KEY_F22, "F22", 1.0F},
            {GLFW.GLFW_KEY_F23, "F23", 1.0F},
            {GLFW.GLFW_KEY_F24, "F24", 1.0F},
            {GLFW.GLFW_KEY_F25, "F25", 1.0F}
        });

        addRow(keys, SECTION_FUNCTIONS, 0.0F, 1.4F, new Object[][] {
            {GLFW.GLFW_KEY_ESCAPE, "Esc", 1.0F},
        });
        addRow(keys, SECTION_FUNCTIONS, 2.6F, 1.4F, new Object[][] {
            {GLFW.GLFW_KEY_F1, "F1", 1.0F},
            {GLFW.GLFW_KEY_F2, "F2", 1.0F},
            {GLFW.GLFW_KEY_F3, "F3", 1.0F},
            {GLFW.GLFW_KEY_F4, "F4", 1.0F}
        });
        addRow(keys, SECTION_FUNCTIONS, 7.6F, 1.4F, new Object[][] {
            {GLFW.GLFW_KEY_F5, "F5", 1.0F},
            {GLFW.GLFW_KEY_F6, "F6", 1.0F},
            {GLFW.GLFW_KEY_F7, "F7", 1.0F},
            {GLFW.GLFW_KEY_F8, "F8", 1.0F}
        });
        addRow(keys, SECTION_FUNCTIONS, 12.6F, 1.4F, new Object[][] {
            {GLFW.GLFW_KEY_F9, "F9", 1.0F},
            {GLFW.GLFW_KEY_F10, "F10", 1.0F},
            {GLFW.GLFW_KEY_F11, "F11", 1.0F},
            {GLFW.GLFW_KEY_F12, "F12", 1.0F}
        });
        addRow(keys, SECTION_FUNCTIONS, 18.0F, 1.4F, new Object[][] {
            {GLFW.GLFW_KEY_PRINT_SCREEN, "PrtSc", 1.1F},
            {GLFW.GLFW_KEY_SCROLL_LOCK, "ScrLk", 1.1F},
            {GLFW.GLFW_KEY_PAUSE, "Pause", 1.1F}
        });

        addRow(keys, SECTION_MAIN, 0.0F, 2.6F, new Object[][] {
            {GLFW.GLFW_KEY_GRAVE_ACCENT, "`", 1.0F},
            {GLFW.GLFW_KEY_1, "1", 1.0F},
            {GLFW.GLFW_KEY_2, "2", 1.0F},
            {GLFW.GLFW_KEY_3, "3", 1.0F},
            {GLFW.GLFW_KEY_4, "4", 1.0F},
            {GLFW.GLFW_KEY_5, "5", 1.0F},
            {GLFW.GLFW_KEY_6, "6", 1.0F},
            {GLFW.GLFW_KEY_7, "7", 1.0F},
            {GLFW.GLFW_KEY_8, "8", 1.0F},
            {GLFW.GLFW_KEY_9, "9", 1.0F},
            {GLFW.GLFW_KEY_0, "0", 1.0F},
            {GLFW.GLFW_KEY_MINUS, "-", 1.0F},
            {GLFW.GLFW_KEY_EQUAL, "=", 1.0F},
            {GLFW.GLFW_KEY_BACKSPACE, "Back", 2.2F}
        });
        addRow(keys, SECTION_NAVIGATION, 17.8F, 2.6F, new Object[][] {
            {GLFW.GLFW_KEY_INSERT, "Ins", 1.0F},
            {GLFW.GLFW_KEY_HOME, "Home", 1.0F},
            {GLFW.GLFW_KEY_PAGE_UP, "PgUp", 1.0F}
        });
        addRow(keys, SECTION_NUMPAD, 21.8F, 2.6F, new Object[][] {
            {GLFW.GLFW_KEY_NUM_LOCK, "Num", 1.0F},
            {GLFW.GLFW_KEY_KP_DIVIDE, "K/", 1.0F},
            {GLFW.GLFW_KEY_KP_MULTIPLY, "K*", 1.0F},
            {GLFW.GLFW_KEY_KP_SUBTRACT, "K-", 1.0F}
        });

        addRow(keys, SECTION_MAIN, 0.0F, 3.8F, new Object[][] {
            {GLFW.GLFW_KEY_TAB, "Tab", 1.5F},
            {GLFW.GLFW_KEY_Q, "Q", 1.0F},
            {GLFW.GLFW_KEY_W, "W", 1.0F},
            {GLFW.GLFW_KEY_E, "E", 1.0F},
            {GLFW.GLFW_KEY_R, "R", 1.0F},
            {GLFW.GLFW_KEY_T, "T", 1.0F},
            {GLFW.GLFW_KEY_Y, "Y", 1.0F},
            {GLFW.GLFW_KEY_U, "U", 1.0F},
            {GLFW.GLFW_KEY_I, "I", 1.0F},
            {GLFW.GLFW_KEY_O, "O", 1.0F},
            {GLFW.GLFW_KEY_P, "P", 1.0F},
            {GLFW.GLFW_KEY_LEFT_BRACKET, "[", 1.0F},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, "]", 1.0F},
            {GLFW.GLFW_KEY_BACKSLASH, "\\", 1.7F}
        });
        addRow(keys, SECTION_NAVIGATION, 17.8F, 3.8F, new Object[][] {
            {GLFW.GLFW_KEY_DELETE, "Del", 1.0F},
            {GLFW.GLFW_KEY_END, "End", 1.0F},
            {GLFW.GLFW_KEY_PAGE_DOWN, "PgDn", 1.0F}
        });
        addRow(keys, SECTION_NUMPAD, 21.8F, 3.8F, new Object[][] {
            {GLFW.GLFW_KEY_KP_7, "K7", 1.0F},
            {GLFW.GLFW_KEY_KP_8, "K8", 1.0F},
            {GLFW.GLFW_KEY_KP_9, "K9", 1.0F},
            {GLFW.GLFW_KEY_KP_ADD, "K+", 1.0F}
        });

        addRow(keys, SECTION_MAIN, 0.0F, 5.0F, new Object[][] {
            {GLFW.GLFW_KEY_CAPS_LOCK, "Caps", 1.8F},
            {GLFW.GLFW_KEY_A, "A", 1.0F},
            {GLFW.GLFW_KEY_S, "S", 1.0F},
            {GLFW.GLFW_KEY_D, "D", 1.0F},
            {GLFW.GLFW_KEY_F, "F", 1.0F},
            {GLFW.GLFW_KEY_G, "G", 1.0F},
            {GLFW.GLFW_KEY_H, "H", 1.0F},
            {GLFW.GLFW_KEY_J, "J", 1.0F},
            {GLFW.GLFW_KEY_K, "K", 1.0F},
            {GLFW.GLFW_KEY_L, "L", 1.0F},
            {GLFW.GLFW_KEY_SEMICOLON, ";", 1.0F},
            {GLFW.GLFW_KEY_APOSTROPHE, "'", 1.0F},
            {GLFW.GLFW_KEY_ENTER, "Enter", 2.6F}
        });
        addRow(keys, SECTION_NUMPAD, 21.8F, 5.0F, new Object[][] {
            {GLFW.GLFW_KEY_KP_4, "K4", 1.0F},
            {GLFW.GLFW_KEY_KP_5, "K5", 1.0F},
            {GLFW.GLFW_KEY_KP_6, "K6", 1.0F},
            {GLFW.GLFW_KEY_KP_EQUAL, "K=", 1.0F}
        });

        addRow(keys, SECTION_MAIN, 0.0F, 6.2F, new Object[][] {
            {GLFW.GLFW_KEY_LEFT_SHIFT, "LShift", 2.2F},
            {GLFW.GLFW_KEY_WORLD_1, "W1", 1.0F},
            {GLFW.GLFW_KEY_Z, "Z", 1.0F},
            {GLFW.GLFW_KEY_X, "X", 1.0F},
            {GLFW.GLFW_KEY_C, "C", 1.0F},
            {GLFW.GLFW_KEY_V, "V", 1.0F},
            {GLFW.GLFW_KEY_B, "B", 1.0F},
            {GLFW.GLFW_KEY_N, "N", 1.0F},
            {GLFW.GLFW_KEY_M, "M", 1.0F},
            {GLFW.GLFW_KEY_COMMA, ",", 1.0F},
            {GLFW.GLFW_KEY_PERIOD, ".", 1.0F},
            {GLFW.GLFW_KEY_SLASH, "/", 1.0F},
            {GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift", 2.2F}
        });
        addRow(keys, SECTION_ARROWS, 19.0F, 6.2F, new Object[][] {
            {GLFW.GLFW_KEY_UP, "Up", 1.0F}
        });
        addRow(keys, SECTION_NUMPAD, 21.8F, 6.2F, new Object[][] {
            {GLFW.GLFW_KEY_KP_1, "K1", 1.0F},
            {GLFW.GLFW_KEY_KP_2, "K2", 1.0F},
            {GLFW.GLFW_KEY_KP_3, "K3", 1.0F},
            {GLFW.GLFW_KEY_KP_ENTER, "KEnt", 1.0F}
        });

        addRow(keys, SECTION_MAIN, 0.0F, 7.4F, new Object[][] {
            {GLFW.GLFW_KEY_LEFT_CONTROL, "LCtrl", 1.4F},
            {GLFW.GLFW_KEY_LEFT_SUPER, "LSuper", 1.2F},
            {GLFW.GLFW_KEY_LEFT_ALT, "LAlt", 1.2F},
            {GLFW.GLFW_KEY_SPACE, "Space", 5.9F},
            {GLFW.GLFW_KEY_RIGHT_ALT, "RAlt", 1.2F},
            {GLFW.GLFW_KEY_WORLD_2, "W2", 1.0F},
            {GLFW.GLFW_KEY_RIGHT_SUPER, "RSuper", 1.2F},
            {GLFW.GLFW_KEY_MENU, "Menu", 1.2F},
            {GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl", 1.4F}
        });
        addRow(keys, SECTION_ARROWS, 17.8F, 7.4F, new Object[][] {
            {GLFW.GLFW_KEY_LEFT, "Left", 1.0F},
            {GLFW.GLFW_KEY_DOWN, "Down", 1.0F},
            {GLFW.GLFW_KEY_RIGHT, "Right", 1.0F}
        });
        addRow(keys, SECTION_NUMPAD, 21.8F, 7.4F, new Object[][] {
            {GLFW.GLFW_KEY_KP_0, "K0", 2.2F},
            {GLFW.GLFW_KEY_KP_DECIMAL, "K.", 1.0F}
        });

        return List.copyOf(keys);
    }

    private static List<KeyboardSection> createSections(List<KeyboardKey> keys) {
        Map<String, SectionBounds> boundsBySection = new LinkedHashMap<>();
        boundsBySection.put(SECTION_F13, new SectionBounds());
        boundsBySection.put(SECTION_FUNCTIONS, new SectionBounds());
        boundsBySection.put(SECTION_MAIN, new SectionBounds());
        boundsBySection.put(SECTION_NAVIGATION, new SectionBounds());
        boundsBySection.put(SECTION_ARROWS, new SectionBounds());
        boundsBySection.put(SECTION_NUMPAD, new SectionBounds());

        for (KeyboardKey key : keys) {
            boundsBySection.get(key.section()).include(key.xUnits(), key.yUnits(), key.widthUnits(), 1.0F);
        }

        List<KeyboardSection> sections = new ArrayList<>();
        for (Map.Entry<String, SectionBounds> entry : boundsBySection.entrySet()) {
            SectionBounds bounds = entry.getValue();
            sections.add(new KeyboardSection(entry.getKey(), bounds.minX - 0.25F, bounds.minY - 0.25F, bounds.width() + 0.5F, bounds.height() + 0.5F));
        }
        return List.copyOf(sections);
    }

    private static void addRow(List<KeyboardKey> keys, String section, float startX, float y, Object[][] row) {
        float x = startX;
        for (Object[] key : row) {
            int keyCode = (int) key[0];
            String label = (String) key[1];
            float width = (float) key[2];
            keys.add(new KeyboardKey(section, keyCode, label, x, y, width));
            x += width + 0.2F;
        }
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
