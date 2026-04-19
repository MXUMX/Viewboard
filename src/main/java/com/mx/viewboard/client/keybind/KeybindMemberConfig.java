package com.mx.viewboard.client.keybind;

public final class KeybindMemberConfig {
    private String keybindId;
    private String originalKey;
    private String originalModifier;

    public KeybindMemberConfig() {
    }

    public KeybindMemberConfig(String keybindId, String originalKey, String originalModifier) {
        this.keybindId = keybindId;
        this.originalKey = originalKey;
        this.originalModifier = originalModifier;
    }

    public String keybindId() {
        return this.keybindId;
    }

    public String originalKey() {
        return this.originalKey;
    }

    public String originalModifier() {
        return this.originalModifier;
    }

    public void setOriginalKey(String originalKey) {
        this.originalKey = originalKey;
    }

    public void setOriginalModifier(String originalModifier) {
        this.originalModifier = originalModifier;
    }
}
