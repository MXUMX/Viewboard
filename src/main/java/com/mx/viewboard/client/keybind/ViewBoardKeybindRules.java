package com.mx.viewboard.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.KeyModifier;

public final class ViewBoardKeybindRules {
    private static final ViewBoardKeybindRules INSTANCE = new ViewBoardKeybindRules();

    private ViewBoardClientConfig config = new ViewBoardClientConfig();
    private boolean loaded;
    private boolean syncInProgress;

    private ViewBoardKeybindRules() {
    }

    public static ViewBoardKeybindRules getInstance() {
        return INSTANCE;
    }

    public void ensureLoaded() {
        if (this.loaded) {
            return;
        }

        this.config = ViewBoardConfigStore.load();
        this.loaded = true;
    }

    public ViewBoardClientConfig config() {
        this.ensureLoaded();
        return this.config;
    }

    public KeyboardLayoutId selectedLayout() {
        return this.config().selectedLayout();
    }

    public void cycleLayout() {
        KeyboardLayoutId next = this.selectedLayout().next();
        this.config.setSelectedLayout(next);
        this.save();
    }

    public boolean isIgnored(KeyMapping mapping) {
        return this.config().ignoredBindings().contains(mapping.getName());
    }

    public void setIgnored(KeyMapping mapping, boolean ignored) {
        if (ignored) {
            this.config().ignoredBindings().add(mapping.getName());
        } else {
            this.config().ignoredBindings().remove(mapping.getName());
        }
        this.save();
    }

    public Optional<KeybindGroupConfig> groupFor(KeyMapping mapping) {
        this.ensureLoaded();
        return this.config.groups().stream()
            .filter(group -> group.members().stream().anyMatch(member -> member.keybindId().equals(mapping.getName())))
            .findFirst();
    }

    public List<KeybindGroupConfig> groups() {
        this.ensureLoaded();
        return this.config.groups();
    }

    public KeybindGroupConfig createGroup(String baseName, SerializedKey triggerKey) {
        this.ensureLoaded();
        String suffix = Integer.toHexString(UUID.randomUUID().hashCode()).toLowerCase(Locale.ROOT);
        String groupId = "group-" + suffix;
        KeybindGroupConfig group = new KeybindGroupConfig(groupId, baseName, triggerKey.asConfigString());
        this.config.groups().add(group);
        this.save();
        return group;
    }

    public void deleteGroup(String groupId) {
        this.ensureLoaded();
        KeybindGroupConfig group = this.findGroup(groupId).orElse(null);
        if (group == null) {
            return;
        }

        for (KeybindMemberConfig member : List.copyOf(group.members())) {
            KeyMapping mapping = this.resolveKeyMapping(member.keybindId()).orElse(null);
            if (mapping != null) {
                this.restoreMember(mapping, member);
            }
        }

        this.config.groups().remove(group);
        this.save();
    }

    public void renameGroup(String groupId, String name) {
        this.findGroup(groupId).ifPresent(group -> {
            group.setName(name);
            this.save();
        });
    }

    public void setGroupTrigger(String groupId, SerializedKey triggerKey) {
        this.findGroup(groupId).ifPresent(group -> {
            group.setTriggerKey(triggerKey.asConfigString());
            this.applyGroupMembers(group);
            this.save();
        });
    }

    public void assignToGroup(KeyMapping mapping, String groupId) {
        this.ensureLoaded();
        if (groupId == null) {
            this.removeFromGroup(mapping);
            return;
        }

        KeybindGroupConfig targetGroup = this.findGroup(groupId).orElse(null);
        if (targetGroup == null) {
            return;
        }

        this.removeFromGroup(mapping);
        targetGroup.members().add(new KeybindMemberConfig(
            mapping.getName(),
            SerializedKey.fromInputKey(mapping.getKey()).asConfigString(),
            mapping.getKeyModifier().name()
        ));
        this.applyGroupMember(mapping, targetGroup, targetGroup.members().get(targetGroup.members().size() - 1));
        this.save();
    }

    public void removeFromGroup(KeyMapping mapping) {
        this.ensureLoaded();
        for (KeybindGroupConfig group : this.config.groups()) {
            KeybindMemberConfig member = group.members().stream()
                .filter(candidate -> candidate.keybindId().equals(mapping.getName()))
                .findFirst()
                .orElse(null);
            if (member == null) {
                continue;
            }

            this.restoreMember(mapping, member);
            group.members().remove(member);
            this.save();
            return;
        }
    }

    public void syncRuntimeState() {
        this.ensureLoaded();
        if (this.syncInProgress) {
            return;
        }

        this.syncInProgress = true;
        try {
            for (KeybindGroupConfig group : this.config.groups()) {
                SerializedKey trigger = SerializedKey.parse(group.triggerKey());
                for (KeybindMemberConfig member : group.members()) {
                    KeyMapping mapping = this.resolveKeyMapping(member.keybindId()).orElse(null);
                    if (mapping == null) {
                        continue;
                    }

                    SerializedKey currentKey = SerializedKey.fromInputKey(mapping.getKey());
                    KeyModifier currentModifier = mapping.getKeyModifier();
                    SerializedKey originalKey = SerializedKey.parse(member.originalKey());
                    KeyModifier originalModifier = parseModifier(member.originalModifier());

                    if (!currentKey.equals(trigger) && !currentKey.equals(originalKey)) {
                        member.setOriginalKey(currentKey.asConfigString());
                        member.setOriginalModifier(currentModifier.name());
                    } else if (currentKey.equals(trigger) && currentModifier != KeyModifier.NONE) {
                        member.setOriginalModifier(currentModifier.name());
                    }

                    this.applyGroupMember(mapping, group, member);
                }
            }
        } finally {
            this.syncInProgress = false;
        }
    }

    public KeyUsageSummary usageFor(InputConstants.Key key) {
        SerializedKey serializedKey = SerializedKey.fromInputKey(key);
        if (serializedKey.isUnknown()) {
            // "Not bound" is not a real trigger and never conflicts.
            return KeyUsageSummary.empty(serializedKey);
        }
        return summarizeBindings(this.collectBindingStates()).getOrDefault(serializedKey, KeyUsageSummary.empty(serializedKey));
    }

    public List<KeyBindingState> collectBindingStates() {
        this.ensureLoaded();
        List<KeyBindingState> states = new ArrayList<>();
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            Optional<KeybindGroupConfig> group = this.groupFor(mapping);
            states.add(new KeyBindingState(
                mapping.getName(),
                mapping,
                SerializedKey.fromInputKey(mapping.getKey()),
                this.isIgnored(mapping),
                group.map(KeybindGroupConfig::id).orElse(null),
                group.map(KeybindGroupConfig::name).orElse(null)
            ));
        }

        states.sort(Comparator
            .comparing(KeyBindingState::groupName, Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(state -> categoryString(state.mapping()))
            .thenComparing(state -> ComponentStringCache.keyName(state.mapping())));
        return states;
    }

    public List<SerializedKey> customKeysForLayout(Set<SerializedKey> representedKeys) {
        return this.collectBindingStates().stream()
            .map(KeyBindingState::effectiveKey)
            .filter(key -> !representedKeys.contains(key))
            .filter(key -> !key.isUnknown())
            .distinct()
            .sorted(Comparator.comparing(SerializedKey::asConfigString))
            .toList();
    }

    public static Map<SerializedKey, KeyUsageSummary> summarizeBindings(Collection<KeyBindingState> states) {
        List<KeyBindingState> nonUnknownStates = states.stream()
            .filter(state -> !state.effectiveKey().isUnknown())
            .toList();

        List<ConflictBindingState> descriptors = nonUnknownStates.stream()
            .map(state -> new ConflictBindingState(state.keybindId(), state.effectiveKey(), state.ignored(), state.groupId()))
            .toList();
        Map<SerializedKey, ConflictSummary> conflictByKey = summarizeConflictStates(descriptors);

        Map<SerializedKey, KeyUsageSummary> summaryByKey = new LinkedHashMap<>();
        Map<SerializedKey, List<KeyBindingState>> byKey = new LinkedHashMap<>();
        for (KeyBindingState state : nonUnknownStates) {
            byKey.computeIfAbsent(state.effectiveKey(), unused -> new ArrayList<>()).add(state);
        }

        for (Map.Entry<SerializedKey, List<KeyBindingState>> entry : byKey.entrySet()) {
            ConflictSummary conflictSummary = conflictByKey.getOrDefault(entry.getKey(), new ConflictSummary(false, false, 0));
            summaryByKey.put(entry.getKey(), new KeyUsageSummary(
                entry.getKey(),
                List.copyOf(entry.getValue()),
                conflictSummary.conflict(),
                conflictSummary.used(),
                conflictSummary.ignoredCount()
            ));
        }
        return summaryByKey;
    }

    public static Map<SerializedKey, ConflictSummary> summarizeConflictStates(Collection<ConflictBindingState> states) {
        Map<SerializedKey, ConflictSummary> summaryByKey = new LinkedHashMap<>();
        Map<String, KeyConflictAnalyzer.ConflictSummary> analyzed = KeyConflictAnalyzer.summarize(states.stream()
            .map(state -> new KeyConflictAnalyzer.ConflictBindingState(
                state.keybindId(),
                state.effectiveKey().asConfigString(),
                state.ignored(),
                state.groupId()))
            .toList());

        for (Map.Entry<String, KeyConflictAnalyzer.ConflictSummary> entry : analyzed.entrySet()) {
            KeyConflictAnalyzer.ConflictSummary summary = entry.getValue();
            summaryByKey.put(SerializedKey.parse(entry.getKey()), new ConflictSummary(summary.conflict(), summary.used(), summary.ignoredCount()));
        }
        return summaryByKey;
    }

    public static String categoryString(KeyMapping mapping) {
        return String.valueOf(mapping.getCategory());
    }

    public void save() {
        this.ensureLoaded();
        ViewBoardConfigStore.save(this.config);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options != null) {
            minecraft.options.save();
        }
    }

    private void restoreMember(KeyMapping mapping, KeybindMemberConfig member) {
        mapping.setKeyModifierAndCode(parseModifier(member.originalModifier()), SerializedKey.parse(member.originalKey()).toInputKey());
        KeyMapping.resetMapping();
    }

    private void applyGroupMembers(KeybindGroupConfig group) {
        for (KeybindMemberConfig member : group.members()) {
            this.resolveKeyMapping(member.keybindId()).ifPresent(mapping -> this.applyGroupMember(mapping, group, member));
        }
    }

    private void applyGroupMember(KeyMapping mapping, KeybindGroupConfig group, KeybindMemberConfig member) {
        mapping.setKeyModifierAndCode(KeyModifier.NONE, SerializedKey.parse(group.triggerKey()).toInputKey());
        KeyMapping.resetMapping();
    }

    private Optional<KeybindGroupConfig> findGroup(String groupId) {
        return this.config().groups().stream().filter(group -> Objects.equals(group.id(), groupId)).findFirst();
    }

    private Optional<KeyMapping> resolveKeyMapping(String keybindId) {
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getName().equals(keybindId)) {
                return Optional.of(mapping);
            }
        }
        return Optional.empty();
    }

    private static KeyModifier parseModifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return KeyModifier.NONE;
        }

        try {
            return KeyModifier.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            return KeyModifier.NONE;
        }
    }

    public record KeyBindingState(
        String keybindId,
        KeyMapping mapping,
        SerializedKey effectiveKey,
        boolean ignored,
        String groupId,
        String groupName
    ) {
    }

    public record KeyUsageSummary(
        SerializedKey key,
        List<KeyBindingState> states,
        boolean conflict,
        boolean used,
        int ignoredCount
    ) {
        public static KeyUsageSummary empty(SerializedKey key) {
            return new KeyUsageSummary(key, List.of(), false, false, 0);
        }
    }

    public record ConflictBindingState(
        String keybindId,
        SerializedKey effectiveKey,
        boolean ignored,
        String groupId
    ) {
    }

    public record ConflictSummary(boolean conflict, boolean used, int ignoredCount) {
    }

    private static final class ComponentStringCache {
        private static final Map<String, String> KEY_NAMES = new HashMap<>();

        private ComponentStringCache() {
        }

        private static String keyName(KeyMapping mapping) {
            return KEY_NAMES.computeIfAbsent(mapping.getName(), unused -> mapping.getTranslatedKeyMessage().getString());
        }
    }
}
