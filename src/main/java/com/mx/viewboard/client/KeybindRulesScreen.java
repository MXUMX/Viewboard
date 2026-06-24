package com.mx.viewboard.client;

import com.mx.viewboard.client.keybind.KeybindGroupConfig;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KeybindRulesScreen extends Screen {
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFFB8B8B8;
    private static final int COLOR_PANEL = 0xE0151515;
    private static final int COLOR_BORDER = 0xFF2B2B2B;
    private static final int ROW_HEIGHT = 28;
    private static final int HEADER_TOP = 22;
    private static final int FOOTER_H = 34;
    private static final int PADDING = 12;

    private final Screen parent;
    private final String targetGroupId;
    private final ViewBoardKeybindRules rules = ViewBoardKeybindRules.getInstance();
    private EditBox searchBox;
    private Button filterButton;
    private RulesList rulesList;
    private String lastSearch = "";
    private IgnoredFilter ignoredFilter = IgnoredFilter.ALL;
    private Button backButton;

    public KeybindRulesScreen(Screen parent) {
        super(Component.translatable("viewboard.rules.title"));
        this.parent = parent;
        this.targetGroupId = null;
    }

    public KeybindRulesScreen(Screen parent, String targetGroupId) {
        super(Component.translatable("viewboard.rules.title"));
        this.parent = parent;
        this.targetGroupId = targetGroupId;
    }

    @Override
    protected void init() {
        this.rules.ensureLoaded();

        this.searchBox = new EditBox(this.font, PADDING + 8, 30, this.searchBoxWidth(), 20, Component.translatable("viewboard.rules.search"));
        this.searchBox.setHint(Component.translatable("viewboard.rules.search"));
        this.searchBox.setMaxLength(80);
        this.addRenderableWidget(this.searchBox);

        this.filterButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            this.ignoredFilter = this.ignoredFilter.next();
            this.refreshFilterButton();
            this.refreshList();
        }).bounds(this.searchBox.getX() + this.searchBox.getWidth() + 6, 30, 96, 20).build());
        this.refreshFilterButton();

        int listTop = this.searchBox.getY() + this.searchBox.getHeight() + 8;
        int listBottom = this.height - 28 - 6;
        this.rulesList = this.addRenderableWidget(new RulesList(Minecraft.getInstance(), this.width, this.height, listTop, ROW_HEIGHT));
        this.rulesList.updateSizeAndPosition(this.width, Math.max(40, listBottom - listTop), 0, listTop);
        this.refreshList();

        this.backButton = this.addRenderableWidget(Button.builder(this.targetGroupId == null ? Component.translatable("gui.back") : Component.translatable("viewboard.rules.close"), button -> this.onClose())
            .bounds(this.width / 2 - 75, this.height - 28, 150, 20)
            .build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (this.searchBox != null) {
            this.searchBox.setX(PADDING + 8);
            this.searchBox.setWidth(this.searchBoxWidth());
        }
        if (this.filterButton != null && this.searchBox != null) {
            this.filterButton.setPosition(this.searchBox.getX() + this.searchBox.getWidth() + 6, this.searchBox.getY());
        }
        if (this.backButton != null) {
            this.backButton.setPosition(this.width / 2 - 75, this.height - 28);
        }
        if (this.rulesList != null && this.searchBox != null) {
            int listTop = this.searchBox.getY() + this.searchBox.getHeight() + 8;
            int listBottom = this.height - 28 - 6;
            this.rulesList.updateSizeAndPosition(this.width, Math.max(40, listBottom - listTop), 0, listTop);
        }
    }

    @Override
    public void tick() {
        super.tick();
        String currentSearch = this.searchBox.getValue();
        if (!currentSearch.equals(this.lastSearch)) {
            this.refreshList();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = PADDING;
        int right = this.width - PADDING;
        int top = HEADER_TOP;
        int bottom = this.height - FOOTER_H;

        guiGraphics.fill(left, top, right, bottom, COLOR_PANEL);
        guiGraphics.fill(left, top, right, top + 1, COLOR_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, COLOR_BORDER);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, COLOR_TEXT);
        guiGraphics.drawCenteredString(this.font, this.targetGroupId == null
            ? Component.translatable("viewboard.rules.subtitle")
            : Component.translatable("viewboard.rules.assign_subtitle", this.targetGroupName()), this.width / 2, 18, COLOR_SUBTEXT);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void refreshList() {
        this.lastSearch = this.searchBox == null ? "" : this.searchBox.getValue();
        String query = this.lastSearch.trim().toLowerCase(Locale.ROOT);
        List<KeyMapping> mappings = new ArrayList<>(List.of(Minecraft.getInstance().options.keyMappings));
        mappings.sort(Comparator
            .comparing((KeyMapping mapping) -> ViewBoardKeybindRules.categoryString(mapping), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(KeyMapping::getName, String.CASE_INSENSITIVE_ORDER));

        List<RuleEntry> entries = new ArrayList<>();
        for (KeyMapping mapping : mappings) {
            boolean ignored = this.rules.isIgnored(mapping);
            if (this.ignoredFilter == IgnoredFilter.IGNORED_ONLY && !ignored) {
                continue;
            }
            if (this.ignoredFilter == IgnoredFilter.ACTIVE_ONLY && ignored) {
                continue;
            }

            String haystack = (ViewBoardKeybindRules.categoryString(mapping) + " " + Component.translatable(mapping.getName()).getString()).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !haystack.contains(query)) {
                continue;
            }

            entries.add(new RuleEntry(mapping));
        }
        this.rulesList.replace(entries);
    }

    private int searchBoxWidth() {
        return Math.max(120, Math.min(240, this.width - (PADDING + 8) * 2 - 102));
    }

    private void refreshFilterButton() {
        if (this.filterButton != null) {
            this.filterButton.setMessage(Component.translatable(this.ignoredFilter.translationKey));
            this.filterButton.visible = this.targetGroupId == null;
        }
    }

    private KeybindGroupConfig targetGroup() {
        if (this.targetGroupId == null) {
            return null;
        }
        return this.rules.groups().stream().filter(group -> group.id().equals(this.targetGroupId)).findFirst().orElse(null);
    }

    private Component targetGroupName() {
        KeybindGroupConfig group = this.targetGroup();
        return group == null ? Component.literal("-") : Component.literal(group.name());
    }

    private final class RulesList extends ContainerObjectSelectionList<RuleEntry> {
        private RulesList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.max(40, this.width - PADDING * 2 - 18);
        }

        @Override
        public int getRowLeft() {
            return PADDING + 2;
        }

        @Override
        protected int scrollBarX() {
            return this.width - PADDING - 6;
        }

        private void replace(List<RuleEntry> entries) {
            this.replaceEntries(entries);
        }
    }

    private final class RuleEntry extends ContainerObjectSelectionList.Entry<RuleEntry> {
        private final KeyMapping mapping;
        private final Button ignoreButton;
        private final Button groupButton;
        private int ignoreWidth = 88;
        private int groupWidth = 110;

        private RuleEntry(KeyMapping mapping) {
            this.mapping = mapping;
            this.ignoreButton = Button.builder(Component.empty(), button -> {
                rules.setIgnored(this.mapping, !rules.isIgnored(this.mapping));
                KeybindRulesScreen.this.refreshList();
            }).bounds(0, 0, this.ignoreWidth, 20).build();
            this.groupButton = Button.builder(Component.empty(), button -> this.handleGroupButtonClick())
                .bounds(0, 0, this.groupWidth, 20)
                .build();
            this.refreshButtons();
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int top = this.getY();
            int left = this.getX();
            int width = this.getWidth();
            int rowTop = top + 2;
            int rowHeight = ROW_HEIGHT - 4;

            int available = width - 8 - 6;
            int desired = KeybindRulesScreen.this.targetGroupId == null ? 88 + 110 : 132;
            if (available < desired) {
                int shrink = desired - available;
                int ignore = Math.max(60, 88 - shrink / 2);
                int group = Math.max(80, (KeybindRulesScreen.this.targetGroupId == null ? 110 : 132) - (shrink - (88 - ignore)));
                this.ignoreWidth = ignore;
                this.groupWidth = group;
                this.ignoreButton.setWidth(ignore);
                this.groupButton.setWidth(group);
            } else {
                if (this.ignoreWidth != 88) {
                    this.ignoreWidth = 88;
                    this.ignoreButton.setWidth(88);
                }
                int targetGroupWidth = KeybindRulesScreen.this.targetGroupId == null ? 110 : 132;
                if (this.groupWidth != targetGroupWidth) {
                    this.groupWidth = targetGroupWidth;
                    this.groupButton.setWidth(targetGroupWidth);
                }
            }

            int ignoreX = left + width - this.ignoreButton.getWidth() - 4;
            int groupX = KeybindRulesScreen.this.targetGroupId == null ? ignoreX - this.groupButton.getWidth() - 6 : left + width - this.groupButton.getWidth() - 4;

            guiGraphics.fill(left, rowTop, left + width, rowTop + rowHeight, hovered ? 0x552E2E2E : 0x33242424);
            guiGraphics.fill(left, rowTop, left + width, rowTop + 1, COLOR_BORDER);
            guiGraphics.fill(left, rowTop + rowHeight - 1, left + width, rowTop + rowHeight, COLOR_BORDER);

            this.groupButton.setPosition(groupX, rowTop + 2);
            this.ignoreButton.setPosition(ignoreX, rowTop + 2);
            this.groupButton.render(guiGraphics, mouseX, mouseY, partialTick);
            if (KeybindRulesScreen.this.targetGroupId == null) {
                this.ignoreButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            int textRight = groupX - 8;
            String title = Component.translatable(this.mapping.getName()).getString();
            String category = Component.translatable(ViewBoardKeybindRules.categoryString(this.mapping)).getString();
            String key = this.mapping.getTranslatedKeyMessage().getString();

            guiGraphics.drawString(font, title, left + 8, rowTop + 4, COLOR_TEXT, false);
            guiGraphics.drawString(font, category + " | " + key, left + 8, rowTop + 14, COLOR_SUBTEXT, false);

            if (font.width(title) > textRight - left - 12) {
                guiGraphics.fill(textRight - 12, rowTop + 3, textRight, rowTop + 12, 0xE0151515);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return KeybindRulesScreen.this.targetGroupId == null ? List.of(this.ignoreButton, this.groupButton) : List.of(this.groupButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return KeybindRulesScreen.this.targetGroupId == null ? List.of(this.ignoreButton, this.groupButton) : List.of(this.groupButton);
        }

        private void refreshButtons() {
            this.ignoreButton.setMessage(Component.translatable(
                rules.isIgnored(this.mapping) ? "viewboard.rules.ignore_on" : "viewboard.rules.ignore_off"));

            KeybindGroupConfig group = rules.groupFor(this.mapping).orElse(null);
            if (KeybindRulesScreen.this.targetGroupId == null) {
                this.groupButton.active = true;
                this.groupButton.setMessage(Component.translatable(
                    group == null ? "viewboard.rules.group_none" : "viewboard.rules.group_named",
                    group == null ? Component.empty() : Component.literal(group.name())));
            } else if (group == null) {
                this.groupButton.active = KeybindRulesScreen.this.targetGroup() != null;
                this.groupButton.setMessage(Component.translatable("viewboard.rules.add_to_group"));
            } else if (group.id().equals(KeybindRulesScreen.this.targetGroupId)) {
                this.groupButton.active = true;
                this.groupButton.setMessage(Component.translatable("viewboard.rules.remove_from_group"));
            } else {
                this.groupButton.active = false;
                this.groupButton.setMessage(Component.translatable("viewboard.rules.in_other_group", Component.literal(group.name())));
            }
        }

        private void handleGroupButtonClick() {
            if (KeybindRulesScreen.this.targetGroupId == null) {
                Minecraft.getInstance().setScreen(new GroupEditorScreen(KeybindRulesScreen.this, this.mapping));
                return;
            }

            KeybindGroupConfig group = rules.groupFor(this.mapping).orElse(null);
            if (group == null) {
                rules.assignToGroup(this.mapping, KeybindRulesScreen.this.targetGroupId);
            } else if (group.id().equals(KeybindRulesScreen.this.targetGroupId)) {
                rules.removeFromGroup(this.mapping);
            }
            KeybindRulesScreen.this.refreshList();
        }
    }

    private enum IgnoredFilter {
        ALL("viewboard.rules.filter_all"),
        IGNORED_ONLY("viewboard.rules.filter_ignored"),
        ACTIVE_ONLY("viewboard.rules.filter_active");

        private final String translationKey;

        IgnoredFilter(String translationKey) {
            this.translationKey = translationKey;
        }

        private IgnoredFilter next() {
            return switch (this) {
                case ALL -> IGNORED_ONLY;
                case IGNORED_ONLY -> ACTIVE_ONLY;
                case ACTIVE_ONLY -> ALL;
            };
        }
    }
}
