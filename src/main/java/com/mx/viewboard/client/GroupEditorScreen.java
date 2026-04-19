package com.mx.viewboard.client;

import com.mx.viewboard.client.keybind.KeybindGroupConfig;
import com.mx.viewboard.client.keybind.SerializedKey;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyModifier;

public final class GroupEditorScreen extends Screen {
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFFB8B8B8;
    private static final int COLOR_PANEL = 0xE0151515;
    private static final int COLOR_BORDER = 0xFF2B2B2B;
    private static final int ROW_HEIGHT = 28;
    private static final int HEADER_TOP = 22;
    private static final int FOOTER_H = 34;
    private static final int PADDING = 12;

    private final Screen parent;
    private final KeyMapping mapping;
    private final ViewBoardKeybindRules rules = ViewBoardKeybindRules.getInstance();
    private GroupsList groupsList;
    private EditBox nameBox;
    private Button triggerButton;
    private Button createButton;
    private Button saveNameButton;
    private Button assignButton;
    private Button removeButton;
    private Button deleteButton;
    private Button backButton;
    private boolean capturingTrigger;
    private SerializedKey pendingModifierKey;
    private String selectedGroupId;
    private boolean compactLayout;
    private int sidebarX;
    private int sidebarWidth;
    private int listRight;

    public GroupEditorScreen(Screen parent, KeyMapping mapping) {
        super(Component.translatable("viewboard.groups.title"));
        this.parent = parent;
        this.mapping = mapping;
    }

    @Override
    protected void init() {
        this.rules.ensureLoaded();
        this.selectedGroupId = this.rules.groupFor(this.mapping).map(KeybindGroupConfig::id).orElseGet(() ->
            this.rules.groups().isEmpty() ? null : this.rules.groups().get(0).id());

        this.computeLayout();
        // We'll size/position the list after laying out compact controls, so use a safe initial size here.
        this.groupsList = this.addRenderableWidget(new GroupsList(Minecraft.getInstance(), Math.max(40, this.width), Math.max(40, this.height - 120), 58, ROW_HEIGHT));

        this.nameBox = new EditBox(this.font, this.sidebarX, 80, this.sidebarWidth, 20, Component.translatable("viewboard.groups.name"));
        this.nameBox.setMaxLength(40);
        this.addRenderableWidget(this.nameBox);

        this.triggerButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            this.capturingTrigger = !this.capturingTrigger;
            this.refreshWidgets();
        }).bounds(this.sidebarX, 110, this.sidebarWidth, 20).build());

        this.createButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.create"), button -> {
            KeybindGroupConfig group = this.rules.createGroup(
                "Group " + (this.rules.groups().size() + 1),
                SerializedKey.fromInputKey(this.mapping.getKey()),
                this.mapping.getKeyModifier()
            );
            this.selectedGroupId = group.id();
            this.refreshWidgets();
        }).bounds(this.sidebarX, 140, this.sidebarWidth, 20).build());

        this.saveNameButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.save_name"), button -> {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                this.rules.renameGroup(group.id(), this.nameBox.getValue().isBlank() ? group.name() : this.nameBox.getValue().trim());
                this.refreshWidgets();
            }
        }).bounds(this.sidebarX, 170, this.sidebarWidth, 20).build());

        this.assignButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.assign"), button -> {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                this.rules.assignToGroup(this.mapping, group.id());
                this.selectedGroupId = group.id();
                this.refreshWidgets();
            }
        }).bounds(this.sidebarX, 200, this.sidebarWidth, 20).build());

        this.removeButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.remove_binding"), button -> {
            this.rules.removeFromGroup(this.mapping);
            this.refreshWidgets();
        }).bounds(this.sidebarX, 230, this.sidebarWidth, 20).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.delete"), button -> {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                this.rules.deleteGroup(group.id());
                this.selectedGroupId = this.rules.groups().isEmpty() ? null : this.rules.groups().get(0).id();
                this.refreshWidgets();
            }
        }).bounds(this.sidebarX, 260, this.sidebarWidth, 20).build());

        this.backButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
            .bounds(this.width / 2 - 75, this.height - 28, 150, 20)
            .build());

        this.refreshWidgets();
        this.applyLayoutToWidgets();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.capturingTrigger) {
            if (keyCode == 256) {
                this.capturingTrigger = false;
                this.pendingModifierKey = null;
                this.refreshWidgets();
                return true;
            }

            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                InputConstants.Key inputKey = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
                KeyModifier modifier = activeModifierForTrigger(inputKey);
                if (KeyModifier.isKeyCodeModifier(inputKey) && modifier == KeyModifier.NONE) {
                    this.pendingModifierKey = SerializedKey.fromInputKey(inputKey);
                    return true;
                }

                this.applyCapturedTrigger(group, SerializedKey.fromInputKey(inputKey), modifier);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.capturingTrigger && this.pendingModifierKey != null) {
            SerializedKey releasedKey = SerializedKey.fromInputKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            if (this.pendingModifierKey.equals(releasedKey)) {
                KeybindGroupConfig group = this.selectedGroup();
                if (group != null) {
                    this.applyCapturedTrigger(group, this.pendingModifierKey, KeyModifier.NONE);
                    return true;
                }
            }
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.capturingTrigger) {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                this.applyCapturedTrigger(
                    group,
                    SerializedKey.fromInputKey(InputConstants.Type.MOUSE.getOrCreate(button)),
                    activeModifierForTrigger(null)
                );
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderMenuBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
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
        guiGraphics.drawCenteredString(this.font, Component.translatable("viewboard.groups.subtitle", Component.translatable(this.mapping.getName())), this.width / 2, 18, COLOR_SUBTEXT);

        KeybindGroupConfig group = this.selectedGroup();
        int infoX = this.sidebarX;
        int infoY = 52;
        if (!this.compactLayout) {
            guiGraphics.drawString(this.font, Component.translatable("viewboard.groups.selected"), infoX, infoY, COLOR_TEXT, false);
            guiGraphics.drawString(this.font, Component.literal(group == null ? "-" : group.name()), infoX, infoY + 18, COLOR_SUBTEXT, false);
            guiGraphics.drawString(this.font, Component.translatable("viewboard.groups.members"), infoX, 300, COLOR_TEXT, false);

            if (group != null) {
                int memberY = 316;
                for (String member : group.members().stream().map(member -> member.keybindId()).limit(9).toList()) {
                    guiGraphics.drawString(this.font, Component.translatable(member), infoX, memberY, COLOR_SUBTEXT, false);
                    memberY += 12;
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        if (this.groupsList == null) {
            return;
        }
        this.computeLayout();
        this.applyLayoutToWidgets();
    }

    private void computeLayout() {
        int minListWidth = 220;
        int desiredSidebarWidth = 200;
        int pad3 = PADDING * 3;
        this.compactLayout = this.width < desiredSidebarWidth + minListWidth + pad3;
        if (this.compactLayout) {
            this.sidebarWidth = Math.min(220, this.width - PADDING * 2);
            this.sidebarX = (this.width - this.sidebarWidth) / 2;
            this.listRight = this.width - PADDING;
        } else {
            this.sidebarWidth = desiredSidebarWidth;
            this.sidebarX = this.width - PADDING - this.sidebarWidth;
            this.listRight = this.sidebarX - PADDING;
        }
    }

    private void applyLayoutToWidgets() {
        if (this.nameBox != null) {
            this.nameBox.setX(this.sidebarX);
            this.nameBox.setWidth(this.sidebarWidth);
        }
        if (this.triggerButton != null) this.triggerButton.setPosition(this.sidebarX, this.triggerButton.getY());
        if (this.triggerButton != null) this.triggerButton.setWidth(this.sidebarWidth);
        if (this.createButton != null) this.createButton.setPosition(this.sidebarX, this.createButton.getY());
        if (this.createButton != null) this.createButton.setWidth(this.sidebarWidth);
        if (this.saveNameButton != null) this.saveNameButton.setPosition(this.sidebarX, this.saveNameButton.getY());
        if (this.saveNameButton != null) this.saveNameButton.setWidth(this.sidebarWidth);
        if (this.assignButton != null) this.assignButton.setPosition(this.sidebarX, this.assignButton.getY());
        if (this.assignButton != null) this.assignButton.setWidth(this.sidebarWidth);
        if (this.removeButton != null) this.removeButton.setPosition(this.sidebarX, this.removeButton.getY());
        if (this.removeButton != null) this.removeButton.setWidth(this.sidebarWidth);
        if (this.deleteButton != null) this.deleteButton.setPosition(this.sidebarX, this.deleteButton.getY());
        if (this.deleteButton != null) this.deleteButton.setWidth(this.sidebarWidth);
        if (this.backButton != null) this.backButton.setPosition(this.width / 2 - 75, this.height - 28);

        if (this.compactLayout) {
            int controlsTop = 52;
            int gap = 4;
            int elementH = 20;
            int elements = 7; // nameBox + 6 buttons
            int controlsHeight = elements * elementH + (elements - 1) * gap;

            // If the screen is extremely short, compact the gaps further to keep controls visible.
            int backY = this.height - 28;
            if (controlsTop + controlsHeight + 10 + 40 > backY - 6) {
                gap = 2;
                controlsHeight = elements * elementH + (elements - 1) * gap;
            }

            int y = controlsTop;
            if (this.nameBox != null) this.nameBox.setY(y);
            y += elementH + gap;
            if (this.triggerButton != null) this.triggerButton.setY(y);
            y += elementH + gap;
            if (this.createButton != null) this.createButton.setY(y);
            y += elementH + gap;
            if (this.saveNameButton != null) this.saveNameButton.setY(y);
            y += elementH + gap;
            if (this.assignButton != null) this.assignButton.setY(y);
            y += elementH + gap;
            if (this.removeButton != null) this.removeButton.setY(y);
            y += elementH + gap;
            if (this.deleteButton != null) this.deleteButton.setY(y);

            int listTop = controlsTop + controlsHeight + 10;
            int listBottom = backY - 6;
            int listWidth = Math.max(40, this.width);
            int listHeight = Math.max(40, listBottom - listTop);
            this.groupsList.updateSizeAndPosition(listWidth, listHeight, listTop);
        } else {
            if (this.nameBox != null) this.nameBox.setY(80);
            if (this.triggerButton != null) this.triggerButton.setY(110);
            if (this.createButton != null) this.createButton.setY(140);
            if (this.saveNameButton != null) this.saveNameButton.setY(170);
            if (this.assignButton != null) this.assignButton.setY(200);
            if (this.removeButton != null) this.removeButton.setY(230);
            if (this.deleteButton != null) this.deleteButton.setY(260);

            int listTop = 58;
            int listBottom = this.height - 28 - 6;
            int listWidth = Math.max(40, this.listRight);
            int listHeight = Math.max(40, listBottom - listTop);
            this.groupsList.updateSizeAndPosition(listWidth, listHeight, listTop);
        }
    }

    private void refreshWidgets() {
        this.groupsList.refreshEntries();
        KeybindGroupConfig group = this.selectedGroup();
        this.nameBox.setValue(group == null ? "" : group.name());
        this.triggerButton.setMessage(Component.translatable(
            this.capturingTrigger ? "viewboard.groups.capturing" : "viewboard.groups.trigger",
            group == null
                ? Component.literal("-")
                : Component.literal(ViewBoardKeybindRules.displayBindingLabel(
                    SerializedKey.parse(group.triggerKey()),
                    ViewBoardKeybindRules.parseModifier(group.triggerModifier())
                ))
        ));
    }

    private void applyCapturedTrigger(KeybindGroupConfig group, SerializedKey key, KeyModifier modifier) {
        this.rules.setGroupTrigger(group.id(), key, modifier);
        this.pendingModifierKey = null;
        this.capturingTrigger = false;
        this.refreshWidgets();
    }

    private static KeyModifier activeModifierForTrigger(InputConstants.Key primaryKey) {
        for (KeyModifier modifier : KeyModifier.MODIFIER_VALUES) {
            if (primaryKey != null && modifier.matches(primaryKey)) {
                continue;
            }
            if (modifier.isActive(null)) {
                return modifier;
            }
        }
        return KeyModifier.NONE;
    }

    private KeybindGroupConfig selectedGroup() {
        if (this.selectedGroupId == null) {
            return null;
        }
        return this.rules.groups().stream().filter(group -> group.id().equals(this.selectedGroupId)).findFirst().orElse(null);
    }

    private final class GroupsList extends ObjectSelectionList<GroupEntry> {
        private GroupsList(Minecraft minecraft, int width, int height, int top, int bottom) {
            super(minecraft, width, height, top, bottom);
            this.refreshEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.max(40, Math.min(360, this.width - PADDING * 2 - 18));
        }

        @Override
        public int getRowLeft() {
            return PADDING + 2;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - PADDING - 6;
        }

        private void refreshEntries() {
            this.clearEntries();
            for (KeybindGroupConfig group : rules.groups()) {
                this.addEntry(new GroupEntry(group));
            }
        }
    }

    private final class GroupEntry extends ObjectSelectionList.Entry<GroupEntry> {
        private final KeybindGroupConfig group;

        private GroupEntry(KeybindGroupConfig group) {
            this.group = group;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selectedGroupId = this.group.id();
            refreshWidgets();
            return true;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean selected = this.group.id().equals(selectedGroupId);
            int rowTop = top + 2;
            int rowHeight = ROW_HEIGHT - 4;
            guiGraphics.fill(left, rowTop, left + width, rowTop + rowHeight, selected ? 0x6654A4FF : hovered ? 0x55363636 : 0x33242424);
            guiGraphics.fill(left, top + 2, left + width, top + 3, COLOR_BORDER);
            guiGraphics.drawString(font, this.group.name(), left + 8, top + 6, COLOR_TEXT, false);
            guiGraphics.drawString(
                font,
                ViewBoardKeybindRules.displayBindingLabel(
                    SerializedKey.parse(this.group.triggerKey()),
                    ViewBoardKeybindRules.parseModifier(this.group.triggerModifier())
                ),
                left + 8,
                top + 18,
                COLOR_SUBTEXT,
                false
            );
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.group.name());
        }
    }
}
