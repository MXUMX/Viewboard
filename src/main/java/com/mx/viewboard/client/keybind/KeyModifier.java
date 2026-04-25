package com.mx.viewboard.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public enum KeyModifier {
    NONE("", -1, -1),
    SHIFT("Shift", GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT),
    CONTROL("Control", GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL),
    ALT("Alt", GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);

    public static final KeyModifier[] MODIFIER_VALUES = {SHIFT, CONTROL, ALT};

    private final String displayName;
    private final int leftKey;
    private final int rightKey;

    KeyModifier(String displayName, int leftKey, int rightKey) {
        this.displayName = displayName;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }

    public static boolean isKeyCodeModifier(InputConstants.Key key) {
        for (KeyModifier modifier : MODIFIER_VALUES) {
            if (modifier.matches(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(InputConstants.Key key) {
        if (this == NONE || key == null || key.getType() != InputConstants.Type.KEYSYM) {
            return false;
        }
        int value = key.getValue();
        return value == this.leftKey || value == this.rightKey;
    }

    public boolean isActive(InputConstants.Key ignoredPrimaryKey) {
        if (this == NONE) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, this.leftKey) || InputConstants.isKeyDown(window, this.rightKey);
    }

    public Component getCombinedName(InputConstants.Key key, Supplier<Component> keyName) {
        if (this == NONE) {
            return keyName.get();
        }
        return Component.literal(this.displayName + " + ").append(keyName.get());
    }
}
