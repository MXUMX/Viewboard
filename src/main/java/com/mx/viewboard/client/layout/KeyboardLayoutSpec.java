package com.mx.viewboard.client.layout;

import com.mx.viewboard.client.keybind.KeyboardLayoutId;

public final class KeyboardLayoutSpec {
    private KeyboardLayoutSpec() {
    }

    public static LayoutKey resolve(KeyboardLayoutId layoutId, String slotId, String defaultKey, String defaultLabel) {
        return switch (layoutId) {
            case QWERTY -> new LayoutKey(defaultKey, defaultLabel);
            case AZERTY -> resolveAzerty(slotId, defaultKey, defaultLabel);
            case QWERTZ -> resolveQwertz(slotId, defaultKey, defaultLabel);
        };
    }

    public static boolean hasCustomSection(int customKeyCount) {
        return customKeyCount > 0;
    }

    private static LayoutKey resolveAzerty(String slotId, String defaultKey, String defaultLabel) {
        return switch (slotId) {
            case "grave" -> new LayoutKey("KEYSYM:162", "<");
            case "digit1" -> new LayoutKey("KEYSYM:49", "&");
            case "digit3" -> new LayoutKey("KEYSYM:51", "\"");
            case "digit4" -> new LayoutKey("KEYSYM:52", "'");
            case "digit5" -> new LayoutKey("KEYSYM:53", "(");
            case "digit6" -> new LayoutKey("KEYSYM:54", "-");
            case "digit8" -> new LayoutKey("KEYSYM:56", "_");
            case "minus" -> new LayoutKey("KEYSYM:45", ")");
            case "q" -> new LayoutKey("KEYSYM:65", "A");
            case "w" -> new LayoutKey("KEYSYM:90", "Z");
            case "a" -> new LayoutKey("KEYSYM:81", "Q");
            case "semicolon" -> new LayoutKey("KEYSYM:77", "M");
            case "world1" -> new LayoutKey("KEYSYM:87", "W");
            case "z" -> new LayoutKey("KEYSYM:87", "W");
            case "m" -> new LayoutKey("KEYSYM:59", ";");
            case "period" -> new LayoutKey("KEYSYM:46", ":");
            case "slash" -> new LayoutKey("KEYSYM:47", "!");
            default -> new LayoutKey(defaultKey, defaultLabel);
        };
    }

    private static LayoutKey resolveQwertz(String slotId, String defaultKey, String defaultLabel) {
        return switch (slotId) {
            case "y" -> new LayoutKey("KEYSYM:90", "Z");
            case "z" -> new LayoutKey("KEYSYM:89", "Y");
            default -> new LayoutKey(defaultKey, defaultLabel);
        };
    }

    public record LayoutKey(String serializedKey, String label) {
    }
}
