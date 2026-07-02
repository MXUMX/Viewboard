package com.mx.viewboard.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mx.viewboard.client.keybind.KeybindGroupConfig;
import com.mx.viewboard.client.keybind.KeybindMemberConfig;
import com.mx.viewboard.client.keybind.SerializedKey;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mx.viewboard.client.keybind.KeyModifier;

public final class GroupEditorScreen extends Screen {
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFFB8B8B8;
    private static final int COLOR_PANEL = 0xE0151515;
    private static final int COLOR_PANEL_SOLID = 0xFF151515;
    private static final int COLOR_BORDER = 0xFF2B2B2B;
    private static final int COLOR_ACCENT = 0x6654A4FF;
    private static final int COLOR_DANGER = 0xFFB93A3A;
    private static final int COLOR_DISABLED = 0xFF686868;
    private static final int ROW_HEIGHT = 32;
    private static final int MEMBER_ROW_HEIGHT = 24;
    private static final int PADDING = 12;
    private static final int HEADER_TOP = 24;
    private static final int FOOTER_H = 34;
    private static final int TOOLBAR_H = 24;

    private final Screen parent;
    private final KeyMapping focusMapping;
    private final ViewBoardKeybindRules rules = ViewBoardKeybindRules.getInstance();

    private EditBox searchBox;
    private Button triggerSearchButton;
    private Button clearTriggerSearchButton;
    private Button newGroupButton;
    private Button backButton;
    private GroupsList groupsList;

    private Button panelCloseButton;
    private Button renameButton;
    private Button triggerButton;
    private Button deleteButton;
    private Button assignButton;
    private Button removeMemberButton;

    private EditBox popupNameBox;
    private Button popupConfirmButton;
    private Button popupCancelButton;

    private String selectedGroupId;
    private String selectedMemberKeybindId;
    private String lastSearch = "";
    private SerializedKey triggerSearchKey;
    private KeyModifier triggerSearchModifier = KeyModifier.NONE;
    private SerializedKey pendingModifierKey;
    private CaptureMode captureMode = CaptureMode.NONE;
    private PopupMode popupMode = PopupMode.NONE;

    private int toolbarTop;
    private int listTop;
    private int listBottom;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int memberListTop;
    private int memberListHeight;
    private boolean narrowPanel;

    public GroupEditorScreen(Screen parent) {
        this(parent, null);
    }

    public GroupEditorScreen(Screen parent, KeyMapping focusMapping) {
        super(Component.translatable("viewboard.groups.title"));
        this.parent = parent;
        this.focusMapping = focusMapping;
    }

    @Override
    protected void init() {
        this.rules.ensureLoaded();
        this.ensureSelectionValid();
        this.computeLayout();

        this.searchBox = new EditBox(this.font, PADDING + 8, this.toolbarTop, this.searchBoxWidth(), 20, Component.translatable("viewboard.groups.search"));
        this.searchBox.setHint(Component.translatable("viewboard.groups.search"));
        this.searchBox.setMaxLength(80);
        this.addRenderableWidget(this.searchBox);

        this.triggerSearchButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            this.captureMode = this.captureMode == CaptureMode.SEARCH_TRIGGER ? CaptureMode.NONE : CaptureMode.SEARCH_TRIGGER;
            this.pendingModifierKey = null;
            this.refreshButtons();
        }).bounds(0, this.toolbarTop, 112, 20).build());

        this.clearTriggerSearchButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.clear_trigger_search"), button -> {
            this.triggerSearchKey = null;
            this.triggerSearchModifier = KeyModifier.NONE;
            this.captureMode = CaptureMode.NONE;
            this.refreshAll();
        }).bounds(0, this.toolbarTop, 24, 20).build());

        this.newGroupButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.new"), button ->
            this.openNamePopup(PopupMode.CREATE, Component.translatable("viewboard.groups.default_name", this.rules.groups().size() + 1).getString()))
            .bounds(0, this.toolbarTop, 112, 20)
            .build());

        this.groupsList = this.addRenderableWidget(new GroupsList(Minecraft.getInstance(), this.width, this.height, this.listTop, ROW_HEIGHT));
        this.panelCloseButton = this.addRenderableWidget(Button.builder(Component.literal("X"), button -> {
            this.selectedGroupId = null;
            this.selectedMemberKeybindId = null;
            this.refreshAll();
        }).bounds(0, 0, 20, 20).build());

        this.renameButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.rename"), button -> {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                this.openNamePopup(PopupMode.RENAME, group.name());
            }
        }).bounds(0, 0, 72, 20).build());

        this.triggerButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            this.captureMode = this.captureMode == CaptureMode.GROUP_TRIGGER ? CaptureMode.NONE : CaptureMode.GROUP_TRIGGER;
            this.pendingModifierKey = null;
            this.refreshButtons();
        }).bounds(0, 0, 120, 20).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.delete_icon"), button -> {
            if (this.selectedGroup() != null) {
                this.openConfirmDeletePopup();
            }
        }).bounds(0, 0, 28, 20).build());

        this.assignButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.assign_new"), button -> {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                Minecraft.getInstance().setScreen(new KeybindRulesScreen(this, group.id()));
            }
        }).bounds(0, 0, 136, 20).build());

        this.removeMemberButton = this.addRenderableWidget(Button.builder(Component.translatable("viewboard.groups.remove_selected"), button -> {
            KeyMapping mapping = this.resolveKeyMapping(this.selectedMemberKeybindId);
            if (mapping != null) {
                this.rules.removeFromGroup(mapping);
                this.selectedMemberKeybindId = null;
                this.refreshAll();
            }
        }).bounds(0, 0, 136, 20).build());

        this.popupNameBox = new EditBox(this.font, 0, 0, 160, 20, Component.translatable("viewboard.groups.name"));
        this.popupNameBox.setMaxLength(40);
        this.addRenderableWidget(this.popupNameBox);
        this.popupConfirmButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.confirmPopup()).bounds(0, 0, 72, 20).build());
        this.popupCancelButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.closePopup()).bounds(0, 0, 72, 20).build());

        this.backButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
            .bounds(this.width / 2 - 75, this.height - 28, 150, 20)
            .build());

        this.applyLayoutToWidgets();
        this.refreshAll();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        if (this.groupsList == null) {
            return;
        }
        this.computeLayout();
        this.applyLayoutToWidgets();
        this.refreshAll();
    }

    @Override
    public void tick() {
        super.tick();
        String currentSearch = this.searchBox == null ? "" : this.searchBox.getValue();
        if (!currentSearch.equals(this.lastSearch)) {
            this.refreshAll();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.popupMode != PopupMode.NONE) {
            if (keyCode == 256) {
                this.closePopup();
                return true;
            }
            if (this.popupMode != PopupMode.DELETE && keyCode == 257) {
                this.confirmPopup();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (this.captureMode != CaptureMode.NONE) {
            if (keyCode == 256) {
                this.captureMode = CaptureMode.NONE;
                this.pendingModifierKey = null;
                this.refreshButtons();
                return true;
            }

            InputConstants.Key inputKey = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
            KeyModifier modifier = activeModifierForTrigger(inputKey);
            if (KeyModifier.isKeyCodeModifier(inputKey) && modifier == KeyModifier.NONE) {
                this.pendingModifierKey = SerializedKey.fromInputKey(inputKey);
                return true;
            }

            this.applyCapturedKey(SerializedKey.fromInputKey(inputKey), modifier);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.captureMode != CaptureMode.NONE && this.pendingModifierKey != null) {
            SerializedKey releasedKey = SerializedKey.fromInputKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            if (this.pendingModifierKey.equals(releasedKey)) {
                this.applyCapturedKey(this.pendingModifierKey, KeyModifier.NONE);
                return true;
            }
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.popupMode != PopupMode.NONE) {
            if (!this.isInsidePopup(mouseX, mouseY)) {
                return true;
            }
            return (this.popupNameBox.visible && this.popupNameBox.mouseClicked(mouseX, mouseY, button))
                || this.popupConfirmButton.mouseClicked(mouseX, mouseY, button)
                || this.popupCancelButton.mouseClicked(mouseX, mouseY, button)
                || true;
        }

        if (this.captureMode != CaptureMode.NONE) {
            this.applyCapturedKey(
                SerializedKey.fromInputKey(InputConstants.Type.MOUSE.getOrCreate(button)),
                activeModifierForTrigger(null)
            );
            return true;
        }

        if (this.selectedGroup() != null && this.isInsidePanel(mouseX, mouseY)) {
            if (this.panelCloseButton.mouseClicked(mouseX, mouseY, button)
                || this.renameButton.mouseClicked(mouseX, mouseY, button)
                || this.triggerButton.mouseClicked(mouseX, mouseY, button)
                || this.deleteButton.mouseClicked(mouseX, mouseY, button)
                || this.assignButton.mouseClicked(mouseX, mouseY, button)
                || this.removeMemberButton.mouseClicked(mouseX, mouseY, button)
                || this.clickMemberList(mouseX, mouseY)) {
                return true;
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.popupMode != PopupMode.NONE) {
            return (this.popupNameBox.visible && this.popupNameBox.mouseReleased(mouseX, mouseY, button))
                || this.popupConfirmButton.mouseReleased(mouseX, mouseY, button)
                || this.popupCancelButton.mouseReleased(mouseX, mouseY, button)
                || true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int left = PADDING;
        int right = this.width - PADDING;
        int top = HEADER_TOP;
        int bottom = this.height - FOOTER_H;

        guiGraphics.fill(left, top, right, bottom, COLOR_PANEL);
        guiGraphics.fill(left, top, right, top + 1, COLOR_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, COLOR_BORDER);
        this.renderToolbarFrame(guiGraphics, this.toolbarTop - 4, this.toolbarTop + 24);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, COLOR_TEXT);
        if (this.focusMapping != null) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("viewboard.groups.subtitle_keybind", Component.translatable(this.focusMapping.getName())), this.width / 2, 18, COLOR_SUBTEXT);
        }

        this.renderToolbarWidgetBox(guiGraphics, this.searchBox);
        this.renderToolbarWidgetBox(guiGraphics, this.triggerSearchButton);
        this.renderToolbarWidgetBox(guiGraphics, this.clearTriggerSearchButton);
        this.renderToolbarWidgetBox(guiGraphics, this.newGroupButton);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderToolbarWidgetBorder(guiGraphics, this.searchBox);
        this.renderToolbarWidgetBorder(guiGraphics, this.triggerSearchButton);
        this.renderToolbarWidgetBorder(guiGraphics, this.clearTriggerSearchButton);
        this.renderToolbarWidgetBorder(guiGraphics, this.newGroupButton);

        if (this.selectedGroup() != null) {
            this.renderPanel(guiGraphics, mouseX, mouseY, partialTick);
            this.renderMemberList(guiGraphics, mouseX, mouseY);
            this.panelCloseButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.renameButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.triggerButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.assignButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.removeMemberButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (this.popupMode != PopupMode.NONE) {
            this.renderPopup(guiGraphics);
            this.popupNameBox.render(guiGraphics, mouseX, mouseY, partialTick);
            this.popupConfirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.popupCancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderToolbarFrame(GuiGraphics guiGraphics, int top, int bottom) {
        int left = PADDING + 4;
        int right = this.width - PADDING - 4;
        guiGraphics.fill(left, top, right, bottom, 0xAA050505);
        guiGraphics.fill(left, top, right, top + 1, COLOR_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, COLOR_BORDER);
    }

    private void renderToolbarWidgetBox(GuiGraphics guiGraphics, AbstractWidget widget) {
        if (widget == null) {
            return;
        }
        int left = widget.getX() - 1;
        int top = widget.getY() - 1;
        int right = widget.getX() + widget.getWidth() + 1;
        int bottom = widget.getY() + widget.getHeight() + 1;
        guiGraphics.fill(left, top, right, bottom, 0xFF111820);
        guiGraphics.fill(left, top, right, top + 1, COLOR_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, COLOR_BORDER);
    }

    private void renderToolbarWidgetBorder(GuiGraphics guiGraphics, AbstractWidget widget) {
        if (widget == null) {
            return;
        }
        int left = widget.getX() - 1;
        int top = widget.getY() - 1;
        int right = widget.getX() + widget.getWidth() + 1;
        int bottom = widget.getY() + widget.getHeight() + 1;
        int border = widget.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF8A94A3;
        guiGraphics.fill(left, top, right, top + 1, border);
        guiGraphics.fill(left, bottom - 1, right, bottom, border);
        guiGraphics.fill(left, top, left + 1, bottom, border);
        guiGraphics.fill(right - 1, top, right, bottom, border);
    }

    private void renderPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        KeybindGroupConfig group = this.selectedGroup();
        if (group == null) {
            return;
        }

        int x = this.panelX;
        int y = this.panelY;
        int right = x + this.panelWidth;
        int bottom = y + this.panelHeight;
        guiGraphics.fill(x, y, right, bottom, COLOR_PANEL_SOLID);
        guiGraphics.fill(x, y, right, y + 1, COLOR_BORDER);
        guiGraphics.fill(x, bottom - 1, right, bottom, COLOR_BORDER);
        guiGraphics.fill(x, y, x + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(right - 1, y, right, bottom, COLOR_BORDER);

        int titleRight = this.renameButton.getX() - 8;
        guiGraphics.drawString(this.font, truncate(group.name(), Math.max(30, titleRight - x - 12)), x + 10, y + 8, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("viewboard.groups.trigger_summary", this.triggerLabel(group)), x + 10, y + 42, COLOR_SUBTEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("viewboard.groups.members_count", group.members().size()), x + 10, y + 84, COLOR_TEXT, false);

        if (group.members().isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("viewboard.groups.no_members"), x + this.panelWidth / 2, y + 142, COLOR_SUBTEXT);
        }
    }

    private void renderPopup(GuiGraphics guiGraphics) {
        int[] bounds = this.popupBounds();
        int x = bounds[0];
        int y = bounds[1];
        int w = bounds[2];
        int h = bounds[3];

        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        guiGraphics.fill(x, y, x + w, y + h, COLOR_PANEL_SOLID);
        guiGraphics.fill(x, y, x + w, y + 1, COLOR_BORDER);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);
        guiGraphics.fill(x, y, x + 1, y + h, COLOR_BORDER);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);

        Component title = switch (this.popupMode) {
            case CREATE -> Component.translatable("viewboard.groups.popup_create");
            case RENAME -> Component.translatable("viewboard.groups.popup_rename");
            case DELETE -> Component.translatable("viewboard.groups.popup_delete");
            case NONE -> Component.empty();
        };
        guiGraphics.drawCenteredString(this.font, title, x + w / 2, y + 10, COLOR_TEXT);

        if (this.popupMode == PopupMode.DELETE) {
            KeybindGroupConfig group = this.selectedGroup();
            guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("viewboard.groups.popup_delete_message", group == null ? Component.literal("-") : Component.literal(group.name())),
                x + w / 2,
                y + 36,
                COLOR_SUBTEXT
            );
        }
    }

    private void renderMemberList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        KeybindGroupConfig group = this.selectedGroup();
        if (group == null || group.members().isEmpty()) {
            return;
        }

        int x = this.memberListX();
        int y = this.memberListTop;
        int width = this.memberListWidth();
        int height = this.memberListHeight;
        int bottom = y + height;

        guiGraphics.fill(x, y, x + width, bottom, 0xAA050505);
        guiGraphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
        guiGraphics.fill(x, bottom - 1, x + width, bottom, COLOR_BORDER);
        guiGraphics.fill(x, y, x + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, bottom, COLOR_BORDER);

        guiGraphics.enableScissor(x + 1, y + 1, x + width - 1, bottom - 1);
        int rowY = y + 6;
        int rowWidth = width - 12;
        for (KeybindMemberConfig member : group.members()) {
            if (rowY + MEMBER_ROW_HEIGHT > bottom - 2) {
                break;
            }

            boolean selected = member.keybindId().equals(this.selectedMemberKeybindId);
            boolean hovered = mouseX >= x + 6 && mouseX < x + 6 + rowWidth && mouseY >= rowY && mouseY < rowY + MEMBER_ROW_HEIGHT;
            int rowColor = selected ? COLOR_ACCENT : hovered ? 0x55363636 : 0x33242424;
            guiGraphics.fill(x + 6, rowY + 2, x + 6 + rowWidth, rowY + MEMBER_ROW_HEIGHT - 2, rowColor);
            guiGraphics.drawString(this.font, truncate(mappingName(member.keybindId()).getString(), rowWidth - 12), x + 12, rowY + 7, COLOR_TEXT, false);
            rowY += MEMBER_ROW_HEIGHT;
        }
        guiGraphics.disableScissor();
    }

    private boolean clickMemberList(double mouseX, double mouseY) {
        KeybindGroupConfig group = this.selectedGroup();
        if (group == null || group.members().isEmpty()) {
            return false;
        }

        int x = this.memberListX();
        int y = this.memberListTop;
        int width = this.memberListWidth();
        int height = this.memberListHeight;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }

        int index = ((int) mouseY - y - 6) / MEMBER_ROW_HEIGHT;
        if (index >= 0 && index < group.members().size()) {
            String keybindId = group.members().get(index).keybindId();
            this.selectedMemberKeybindId = keybindId.equals(this.selectedMemberKeybindId) ? null : keybindId;
            this.refreshButtons();
        }
        return true;
    }

    private int memberListX() {
        return this.panelX + 10;
    }

    private int memberListWidth() {
        return Math.max(40, this.panelWidth - 20);
    }

    private void computeLayout() {
        this.toolbarTop = 32;
        this.listTop = this.toolbarTop + TOOLBAR_H + 8;
        this.listBottom = this.height - FOOTER_H - 6;
        this.narrowPanel = this.width < 560;
        if (this.narrowPanel) {
            this.panelWidth = Math.max(220, this.width - PADDING * 4);
            this.panelHeight = Math.max(120, this.listBottom - this.listTop);
            this.panelX = (this.width - this.panelWidth) / 2;
            this.panelY = this.listTop;
        } else {
            this.panelWidth = Math.min(340, Math.max(260, this.width / 3));
            this.panelHeight = Math.max(120, this.listBottom - this.listTop);
            this.panelX = this.width - PADDING - this.panelWidth;
            this.panelY = this.listTop;
        }
    }

    private void applyLayoutToWidgets() {
        int left = PADDING + 8;
        int right = this.width - PADDING - 8;
        int newWidth = Math.min(112, Math.max(86, this.width / 5));
        int clearWidth = 24;
        int triggerWidth = Math.min(140, Math.max(104, this.width / 4));
        int searchWidth = Math.max(90, right - left - newWidth - triggerWidth - clearWidth - 18);

        this.searchBox.setPosition(left, this.toolbarTop);
        this.searchBox.setWidth(searchWidth);
        int x = this.searchBox.getX() + this.searchBox.getWidth() + 6;
        this.triggerSearchButton.setPosition(x, this.toolbarTop);
        this.triggerSearchButton.setWidth(triggerWidth);
        x += triggerWidth + 4;
        this.clearTriggerSearchButton.setPosition(x, this.toolbarTop);
        this.clearTriggerSearchButton.setWidth(clearWidth);
        this.newGroupButton.setPosition(Math.max(x + clearWidth + 6, right - newWidth), this.toolbarTop);
        this.newGroupButton.setWidth(newWidth);

        this.groupsList.updateSize(this.width, this.height, this.listTop, this.listBottom);

        this.panelCloseButton.setPosition(this.panelX + this.panelWidth - 28, this.panelY + 6);
        this.renameButton.setPosition(this.panelX + this.panelWidth - 104, this.panelY + 6);
        this.triggerButton.setPosition(this.panelX + 10, this.panelY + 58);
        this.triggerButton.setWidth(Math.max(100, this.panelWidth - 48));
        this.deleteButton.setPosition(this.panelX + this.panelWidth - 36, this.panelY + 58);
        if (this.panelWidth < 300) {
            this.assignButton.setPosition(this.panelX + 10, this.panelY + 96);
            this.assignButton.setWidth(this.panelWidth - 20);
            this.removeMemberButton.setPosition(this.panelX + 10, this.panelY + 120);
            this.removeMemberButton.setWidth(this.panelWidth - 20);
            this.memberListTop = this.panelY + 148;
        } else {
            this.assignButton.setPosition(this.panelX + 10, this.panelY + 96);
            this.assignButton.setWidth((this.panelWidth - 30) / 2);
            this.removeMemberButton.setPosition(this.assignButton.getX() + this.assignButton.getWidth() + 8, this.panelY + 96);
            this.removeMemberButton.setWidth(this.panelWidth - 20 - this.assignButton.getWidth() - 8);
            this.memberListTop = this.panelY + 112;
        }
        this.memberListHeight = Math.max(40, this.panelY + this.panelHeight - this.memberListTop - 12);

        int[] popup = this.popupBounds();
        this.popupNameBox.setPosition(popup[0] + 16, popup[1] + 34);
        this.popupNameBox.setWidth(popup[2] - 32);
        this.popupCancelButton.setPosition(popup[0] + popup[2] - 88, popup[1] + popup[3] - 30);
        this.popupConfirmButton.setPosition(popup[0] + popup[2] - 168, popup[1] + popup[3] - 30);
        this.backButton.setPosition(this.width / 2 - 75, this.height - 28);
    }

    private int searchBoxWidth() {
        return Math.max(90, Math.min(260, this.width - PADDING * 2 - 260));
    }

    private void refreshAll() {
        this.ensureSelectionValid();
        this.lastSearch = this.searchBox == null ? "" : this.searchBox.getValue();
        if (this.groupsList != null) {
            this.groupsList.refreshEntries();
        }
        this.refreshButtons();
    }

    private void refreshButtons() {
        KeybindGroupConfig group = this.selectedGroup();
        boolean panelOpen = group != null && this.popupMode == PopupMode.NONE;

        this.triggerSearchButton.setMessage(this.triggerSearchButtonMessage());
        this.clearTriggerSearchButton.active = this.triggerSearchKey != null;
        boolean popupOpen = this.popupMode != PopupMode.NONE;
        this.searchBox.active = !popupOpen;
        this.searchBox.setEditable(!popupOpen);
        this.triggerSearchButton.active = !popupOpen;
        this.clearTriggerSearchButton.active = !popupOpen && this.triggerSearchKey != null;
        this.newGroupButton.active = !popupOpen;
        this.backButton.active = !popupOpen;

        this.panelCloseButton.visible = panelOpen;
        this.renameButton.visible = panelOpen;
        this.triggerButton.visible = panelOpen;
        this.deleteButton.visible = panelOpen;
        this.assignButton.visible = panelOpen;
        this.removeMemberButton.visible = panelOpen;

        this.panelCloseButton.active = panelOpen && !popupOpen;
        this.renameButton.active = panelOpen && !popupOpen;
        this.triggerButton.active = panelOpen && !popupOpen;
        this.deleteButton.active = panelOpen && !popupOpen;
        this.assignButton.active = panelOpen && !popupOpen;
        this.removeMemberButton.active = panelOpen && !popupOpen && this.selectedMemberKeybindId != null;

        if (group != null) {
            this.triggerButton.setMessage(Component.translatable(
                this.captureMode == CaptureMode.GROUP_TRIGGER ? "viewboard.groups.capturing" : "viewboard.groups.trigger",
                Component.literal(this.triggerLabel(group))
            ));
        }

        this.popupNameBox.visible = popupOpen && this.popupMode != PopupMode.DELETE;
        this.popupConfirmButton.visible = popupOpen;
        this.popupCancelButton.visible = popupOpen;
        this.popupConfirmButton.active = popupOpen;
        this.popupCancelButton.active = popupOpen;
        this.popupNameBox.setEditable(popupOpen && this.popupMode != PopupMode.DELETE);

        if (popupOpen) {
            this.popupConfirmButton.setMessage(this.popupMode == PopupMode.DELETE
                ? Component.translatable("viewboard.groups.confirm_delete")
                : Component.translatable("gui.done"));
        }
    }

    private Component triggerSearchButtonMessage() {
        if (this.captureMode == CaptureMode.SEARCH_TRIGGER) {
            return Component.translatable("viewboard.groups.capturing");
        }
        if (this.triggerSearchKey == null) {
            return Component.translatable("viewboard.groups.search_trigger");
        }
        return Component.literal(ViewBoardKeybindRules.displayBindingLabel(this.triggerSearchKey, this.triggerSearchModifier));
    }

    private void openNamePopup(PopupMode mode, String initialValue) {
        this.popupMode = mode;
        this.captureMode = CaptureMode.NONE;
        this.pendingModifierKey = null;
        this.popupNameBox.setValue(initialValue == null ? "" : initialValue);
        this.setInitialFocus(this.popupNameBox);
        this.refreshButtons();
    }

    private void openConfirmDeletePopup() {
        this.popupMode = PopupMode.DELETE;
        this.captureMode = CaptureMode.NONE;
        this.pendingModifierKey = null;
        this.refreshButtons();
    }

    private void closePopup() {
        this.popupMode = PopupMode.NONE;
        this.refreshButtons();
    }

    private void confirmPopup() {
        if (this.popupMode == PopupMode.CREATE) {
            String name = this.popupNameBox.getValue().isBlank()
                ? Component.translatable("viewboard.groups.default_name", this.rules.groups().size() + 1).getString()
                : this.popupNameBox.getValue().trim();
            KeybindGroupConfig group = this.rules.createGroup(name, SerializedKey.fromInputKey(InputConstants.UNKNOWN));
            this.selectedGroupId = group.id();
            this.selectedMemberKeybindId = null;
        } else if (this.popupMode == PopupMode.RENAME) {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null && !this.popupNameBox.getValue().isBlank()) {
                this.rules.renameGroup(group.id(), this.popupNameBox.getValue().trim());
            }
        } else if (this.popupMode == PopupMode.DELETE) {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                this.rules.deleteGroup(group.id());
                this.selectedGroupId = null;
                this.selectedMemberKeybindId = null;
            }
        }
        this.popupMode = PopupMode.NONE;
        this.refreshAll();
    }

    private void applyCapturedKey(SerializedKey key, KeyModifier modifier) {
        if (this.captureMode == CaptureMode.SEARCH_TRIGGER) {
            this.triggerSearchKey = key;
            this.triggerSearchModifier = modifier;
        } else if (this.captureMode == CaptureMode.GROUP_TRIGGER) {
            KeybindGroupConfig group = this.selectedGroup();
            if (group != null) {
                this.rules.setGroupTrigger(group.id(), key, modifier);
            }
        }
        this.captureMode = CaptureMode.NONE;
        this.pendingModifierKey = null;
        this.refreshAll();
    }

    private void ensureSelectionValid() {
        if (this.selectedGroupId != null && this.rules.groups().stream().noneMatch(group -> group.id().equals(this.selectedGroupId))) {
            this.selectedGroupId = null;
            this.selectedMemberKeybindId = null;
        }
    }

    private KeybindGroupConfig selectedGroup() {
        if (this.selectedGroupId == null) {
            return null;
        }
        return this.rules.groups().stream().filter(group -> group.id().equals(this.selectedGroupId)).findFirst().orElse(null);
    }

    private List<KeybindGroupConfig> filteredGroups() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        return this.rules.groups().stream()
            .filter(group -> query.isEmpty() || group.name().toLowerCase(Locale.ROOT).contains(query))
            .filter(group -> this.triggerSearchKey == null || Objects.equals(SerializedKey.parse(group.triggerKey()), this.triggerSearchKey)
                && ViewBoardKeybindRules.parseModifier(group.triggerModifier()) == this.triggerSearchModifier)
            .sorted(Comparator.comparing(KeybindGroupConfig::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private KeyMapping resolveKeyMapping(String keybindId) {
        if (keybindId == null) {
            return null;
        }
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getName().equals(keybindId)) {
                return mapping;
            }
        }
        return null;
    }

    private Component mappingName(String keybindId) {
        KeyMapping mapping = this.resolveKeyMapping(keybindId);
        return mapping == null ? Component.translatable(keybindId) : Component.translatable(mapping.getName());
    }

    private String triggerLabel(KeybindGroupConfig group) {
        return ViewBoardKeybindRules.displayBindingLabel(SerializedKey.parse(group.triggerKey()), ViewBoardKeybindRules.parseModifier(group.triggerModifier()));
    }

    private String truncate(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
    }

    private int[] popupBounds() {
        int w = Math.min(300, Math.max(220, this.width - PADDING * 4));
        int h = this.popupMode == PopupMode.DELETE ? 92 : 104;
        return new int[] {(this.width - w) / 2, (this.height - h) / 2, w, h};
    }

    private boolean isInsidePopup(double mouseX, double mouseY) {
        int[] bounds = this.popupBounds();
        return mouseX >= bounds[0] && mouseX <= bounds[0] + bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[1] + bounds[3];
    }

    private boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= this.panelX && mouseX <= this.panelX + this.panelWidth && mouseY >= this.panelY && mouseY <= this.panelY + this.panelHeight;
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

    private final class GroupsList extends ObjectSelectionList<GroupEntry> {
        private GroupsList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, height - FOOTER_H - 6, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.max(80, this.width - PADDING * 2 - 18);
        }

        @Override
        public int getRowLeft() {
            return PADDING + 2;
        }
        protected int getScrollbarPosition() {
            return this.width - PADDING - 6;
        }

        private void refreshEntries() {
            this.clearEntries();
            for (KeybindGroupConfig group : filteredGroups()) {
                this.addEntry(new GroupEntry(group));
            }
        }
    }

    private final class GroupEntry extends ObjectSelectionList.Entry<GroupEntry> {
        private final KeybindGroupConfig group;
        private int quickX;
        private int quickY;
        private int quickWidth;

        private GroupEntry(KeybindGroupConfig group) {
            this.group = group;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (focusMapping != null && this.quickWidth > 0 && this.isInsideQuickButton(mouseX, mouseY)) {
                KeybindGroupConfig currentGroup = rules.groupFor(focusMapping).orElse(null);
                if (currentGroup != null && currentGroup.id().equals(this.group.id())) {
                    rules.removeFromGroup(focusMapping);
                } else if (currentGroup == null) {
                    rules.assignToGroup(focusMapping, this.group.id());
                    selectedGroupId = this.group.id();
                }
                refreshAll();
                return true;
            }

            selectedMemberKeybindId = null;
            selectedGroupId = this.group.id().equals(selectedGroupId) ? null : this.group.id();
            refreshAll();
            return true;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean selected = this.group.id().equals(selectedGroupId);
            int rowTop = top + 2;
            int rowHeight = ROW_HEIGHT - 4;

            guiGraphics.fill(left, rowTop, left + width, rowTop + rowHeight, selected ? COLOR_ACCENT : hovered ? 0x55363636 : 0x33242424);
            guiGraphics.fill(left, rowTop, left + width, rowTop + 1, COLOR_BORDER);
            guiGraphics.fill(left, rowTop + rowHeight - 1, left + width, rowTop + rowHeight, COLOR_BORDER);

            int textRight = left + width - 8;
            this.quickWidth = 0;
            if (focusMapping != null) {
                this.quickWidth = 94;
                this.quickX = left + width - this.quickWidth - 6;
                this.quickY = rowTop + 4;
                textRight = this.quickX - 8;
                this.renderQuickButton(guiGraphics, mouseX, mouseY);
            }

            guiGraphics.drawString(font, truncate(this.group.name(), textRight - left - 12), left + 8, rowTop + 5, COLOR_TEXT, false);
            guiGraphics.drawString(
                font,
                Component.translatable("viewboard.groups.row_summary", Component.literal(triggerLabel(this.group)), this.group.members().size()),
                left + 8,
                rowTop + 17,
                COLOR_SUBTEXT,
                false
            );
        }

        private void renderQuickButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            KeybindGroupConfig currentGroup = rules.groupFor(focusMapping).orElse(null);
            boolean inThisGroup = currentGroup != null && currentGroup.id().equals(this.group.id());
            boolean disabled = currentGroup != null && !inThisGroup;
            boolean hovered = this.isInsideQuickButton(mouseX, mouseY);
            int color = disabled ? 0x663A3A3A : hovered ? 0x884E6C98 : 0x664E6C98;
            guiGraphics.fill(this.quickX, this.quickY, this.quickX + this.quickWidth, this.quickY + 20, color);
            guiGraphics.fill(this.quickX, this.quickY, this.quickX + this.quickWidth, this.quickY + 1, COLOR_BORDER);
            guiGraphics.drawCenteredString(
                font,
                Component.translatable(inThisGroup ? "viewboard.groups.quick_remove" : disabled ? "viewboard.groups.quick_blocked" : "viewboard.groups.quick_add"),
                this.quickX + this.quickWidth / 2,
                this.quickY + 6,
                disabled ? COLOR_DISABLED : COLOR_TEXT
            );
        }

        private boolean isInsideQuickButton(double mouseX, double mouseY) {
            return mouseX >= this.quickX && mouseX <= this.quickX + this.quickWidth && mouseY >= this.quickY && mouseY <= this.quickY + 20;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.group.name());
        }
    }

    private enum CaptureMode {
        NONE,
        SEARCH_TRIGGER,
        GROUP_TRIGGER
    }

    private enum PopupMode {
        NONE,
        CREATE,
        RENAME,
        DELETE
    }
}
