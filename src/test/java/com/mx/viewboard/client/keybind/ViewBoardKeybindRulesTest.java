package com.mx.viewboard.client.keybind;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewBoardKeybindRulesTest {
    @Test
    void summarizeBindingsRespectsIgnoredAndSharedGroups() {
        String key = "KEYSYM:65";
        var summary = KeyConflictAnalyzer.summarize(List.of(
            new KeyConflictAnalyzer.ConflictBindingState("binding.a", key, false, null),
            new KeyConflictAnalyzer.ConflictBindingState("binding.b", key, true, null),
            new KeyConflictAnalyzer.ConflictBindingState("binding.c", key, false, "group-1"),
            new KeyConflictAnalyzer.ConflictBindingState("binding.d", key, false, "group-1")
        ));

        assertTrue(summary.get(key).conflict(), "ungrouped binding should still conflict with grouped bindings");
        assertTrue(summary.get(key).ignoredCount() == 1);
    }

    @Test
    void summarizeBindingsSkipsConflictsWithinSameGroup() {
        String key = "KEYSYM:70";
        var summary = KeyConflictAnalyzer.summarize(List.of(
            new KeyConflictAnalyzer.ConflictBindingState("binding.a", key, false, "group-1"),
            new KeyConflictAnalyzer.ConflictBindingState("binding.b", key, false, "group-1")
        ));

        assertFalse(summary.get(key).conflict());
        assertTrue(summary.get(key).used());
    }
}
