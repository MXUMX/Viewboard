package com.mx.viewboard.client.layout;

import com.mx.viewboard.client.keybind.SerializedKey;

public record VisualKey(
    String id,
    String sectionId,
    SerializedKey key,
    String label,
    float xUnits,
    float yUnits,
    float widthUnits,
    float heightUnits,
    boolean custom
) {
}
