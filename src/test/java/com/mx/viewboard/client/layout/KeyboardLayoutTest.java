package com.mx.viewboard.client.layout;

import com.mx.viewboard.client.keybind.KeyboardLayoutId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardLayoutTest {
    @Test
    void layoutSwitchRemapsAlphabetPositions() {
        KeyboardLayoutSpec.LayoutKey azertyQSlot = KeyboardLayoutSpec.resolve(KeyboardLayoutId.AZERTY, "q", "KEYSYM:81", "Q");
        KeyboardLayoutSpec.LayoutKey azertyASlot = KeyboardLayoutSpec.resolve(KeyboardLayoutId.AZERTY, "a", "KEYSYM:65", "A");
        KeyboardLayoutSpec.LayoutKey qwertzYSlot = KeyboardLayoutSpec.resolve(KeyboardLayoutId.QWERTZ, "y", "KEYSYM:89", "Y");

        assertEquals("KEYSYM:65", azertyQSlot.serializedKey());
        assertEquals("KEYSYM:81", azertyASlot.serializedKey());
        assertEquals("KEYSYM:90", qwertzYSlot.serializedKey());
    }

    @Test
    void customKeysAreAddedAsVisualElements() {
        assertTrue(KeyboardLayoutSpec.hasCustomSection(1));
    }
}
