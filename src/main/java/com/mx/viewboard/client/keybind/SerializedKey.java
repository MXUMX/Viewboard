package com.mx.viewboard.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public record SerializedKey(InputConstants.Type type, int value) {
    public static SerializedKey fromInputKey(InputConstants.Key key) {
        return new SerializedKey(key.getType(), key.getValue());
    }

    public static SerializedKey parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return fromInputKey(InputConstants.UNKNOWN);
        }

        String[] parts = raw.split(":", 2);
        if (parts.length != 2) {
            return fromInputKey(InputConstants.UNKNOWN);
        }

        try {
            InputConstants.Type type = InputConstants.Type.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            int value = Integer.parseInt(parts[1].trim());
            return new SerializedKey(type, value);
        } catch (IllegalArgumentException exception) {
            return fromInputKey(InputConstants.UNKNOWN);
        }
    }

    public InputConstants.Key toInputKey() {
        return this.type.getOrCreate(this.value);
    }

    public String asConfigString() {
        return this.type.name() + ":" + this.value;
    }

    public boolean isUnknown() {
        return this.toInputKey().equals(InputConstants.UNKNOWN);
    }

    public String displayLabel() {
        return simplifyLabel(this.toInputKey().getDisplayName());
    }

    public static String simplifyLabel(Component component) {
        String label = component == null ? "" : component.getString();
        if (label.isBlank()) {
            return "Unknown";
        }
        // Use Minecraft's own localized display name verbatim.
        return label;
    }
}
