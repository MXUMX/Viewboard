package com.mx.viewboard.client.keybind;

public enum KeyboardLayoutId {
    QWERTY("QWERTY"),
    AZERTY("AZERTY"),
    QWERTZ("QWERTZ");

    private final String displayName;

    KeyboardLayoutId(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

    public KeyboardLayoutId next() {
        KeyboardLayoutId[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
