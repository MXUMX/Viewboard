package com.mx.viewboard.client.keybind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ViewBoardClientConfig {
    private String selectedLayout = KeyboardLayoutId.QWERTY.name();
    private final Set<String> ignoredBindings = new LinkedHashSet<>();
    private final List<KeybindGroupConfig> groups = new ArrayList<>();

    public KeyboardLayoutId selectedLayout() {
        try {
            return KeyboardLayoutId.valueOf(this.selectedLayout);
        } catch (IllegalArgumentException exception) {
            return KeyboardLayoutId.QWERTY;
        }
    }

    public void setSelectedLayout(KeyboardLayoutId layoutId) {
        this.selectedLayout = layoutId.name();
    }

    public Set<String> ignoredBindings() {
        return this.ignoredBindings;
    }

    public List<KeybindGroupConfig> groups() {
        return this.groups;
    }
}
