package com.mx.viewboard.client.keybind;

import java.util.ArrayList;
import java.util.List;

public final class KeybindGroupConfig {
    private String id;
    private String name;
    private String triggerKey;
    private final List<KeybindMemberConfig> members = new ArrayList<>();

    public KeybindGroupConfig() {
    }

    public KeybindGroupConfig(String id, String name, String triggerKey) {
        this.id = id;
        this.name = name;
        this.triggerKey = triggerKey;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String triggerKey() {
        return this.triggerKey;
    }

    public void setTriggerKey(String triggerKey) {
        this.triggerKey = triggerKey;
    }

    public List<KeybindMemberConfig> members() {
        return this.members;
    }
}
