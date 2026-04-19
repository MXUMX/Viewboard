package com.mx.viewboard.client.keybind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeyConflictAnalyzer {
    private KeyConflictAnalyzer() {
    }

    public static Map<String, ConflictSummary> summarize(Collection<ConflictBindingState> states) {
        Map<String, List<ConflictBindingState>> byKey = new LinkedHashMap<>();
        for (ConflictBindingState state : states) {
            byKey.computeIfAbsent(state.serializedKey(), unused -> new ArrayList<>()).add(state);
        }

        Map<String, ConflictSummary> summaryByKey = new LinkedHashMap<>();
        for (Map.Entry<String, List<ConflictBindingState>> entry : byKey.entrySet()) {
            Set<String> effectiveConflictBuckets = new LinkedHashSet<>();
            for (ConflictBindingState state : entry.getValue()) {
                if (state.ignored()) {
                    continue;
                }

                effectiveConflictBuckets.add(state.groupId() != null ? "group:" + state.groupId() : "binding:" + state.keybindId());
            }

            summaryByKey.put(entry.getKey(), new ConflictSummary(
                effectiveConflictBuckets.size() > 1,
                !entry.getValue().isEmpty(),
                (int) entry.getValue().stream().filter(ConflictBindingState::ignored).count()
            ));
        }
        return summaryByKey;
    }

    public record ConflictBindingState(String keybindId, String serializedKey, boolean ignored, String groupId) {
    }

    public record ConflictSummary(boolean conflict, boolean used, int ignoredCount) {
    }
}
