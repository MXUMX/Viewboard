package com.mx.viewboard.client.keybind;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewBoardConfigStoreTest {
    @Test
    void roundTripsConfigJson() {
        ViewBoardClientConfig config = new ViewBoardClientConfig();
        config.setSelectedLayout(KeyboardLayoutId.QWERTY.next());
        config.ignoredBindings().add("key.test.ignore");
        KeybindGroupConfig group = new KeybindGroupConfig("group-a", "Alpha", "KEYSYM:65");
        group.members().add(new KeybindMemberConfig("key.test.member", "KEYSYM:66", "NONE"));
        config.groups().add(group);

        String json = ViewBoardConfigCodec.toJson(config);
        ViewBoardClientConfig restored = ViewBoardConfigCodec.fromJson(json);

        assertEquals(KeyboardLayoutId.AZERTY, restored.selectedLayout());
        assertTrue(restored.ignoredBindings().contains("key.test.ignore"));
        assertEquals(1, restored.groups().size());
        assertEquals("Alpha", restored.groups().get(0).name());
        assertEquals("key.test.member", restored.groups().get(0).members().get(0).keybindId());
    }
}
