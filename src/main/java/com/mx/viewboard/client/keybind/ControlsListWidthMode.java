package com.mx.viewboard.client.keybind;

public enum ControlsListWidthMode {
    DEFAULT("viewboard.controls.width.default", 340),
    LARGE("viewboard.controls.width.large", 520),
    VERY_LARGE("viewboard.controls.width.very_large", 680);

    private final String translationKey;
    private final int targetWidth;

    ControlsListWidthMode(String translationKey, int targetWidth) {
        this.translationKey = translationKey;
        this.targetWidth = targetWidth;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public int rowWidth(int screenWidth) {
        return switch (this) {
            case DEFAULT -> this.targetWidth;
            case LARGE -> Math.min(this.targetWidth, Math.max(340, screenWidth - 96));
            case VERY_LARGE -> Math.min(this.targetWidth, Math.max(340, screenWidth - 32));
        };
    }

    public ControlsListWidthMode next() {
        return switch (this) {
            case DEFAULT -> LARGE;
            case LARGE -> VERY_LARGE;
            case VERY_LARGE -> DEFAULT;
        };
    }
}
