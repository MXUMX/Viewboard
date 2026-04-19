package com.mx.viewboard.client.layout;

import com.mx.viewboard.client.keybind.SerializedKey;
import java.util.List;
import java.util.Set;

public record KeyboardViewModel(
    List<VisualSection> sections,
    List<VisualKey> keys,
    Set<SerializedKey> representedKeys,
    float totalWidthUnits,
    float totalHeightUnits
) {
}
