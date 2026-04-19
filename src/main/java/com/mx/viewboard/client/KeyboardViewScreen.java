package com.mx.viewboard.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mx.viewboard.client.keybind.SerializedKey;
import com.mx.viewboard.client.keybind.ViewBoardKeybindRules;
import com.mx.viewboard.client.layout.KeyboardLayout;
import com.mx.viewboard.client.layout.KeyboardViewModel;
import com.mx.viewboard.client.layout.VisualKey;
import com.mx.viewboard.client.layout.VisualSection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KeyboardViewScreen extends Screen {
    private static final int COLOR_UNUSED = 0xFFEFEFEF;
    private static final int COLOR_SINGLE = 0xFF59D978;
    private static final int COLOR_CONFLICT = 0xFFE05252;
    private static final int COLOR_BORDER = 0xFF2B2B2B;
    private static final int COLOR_PANEL = 0xF0181818;
    private static final int COLOR_SECTION = 0x662C2C2C;
    private static final int COLOR_TEXT_DARK = 0xFF000000;
    private static final int COLOR_TEXT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SUBTLE = 0xFFB8B8B8;

    private final Screen parent;
    private final ViewBoardKeybindRules rules = ViewBoardKeybindRules.getInstance();
    private KeyboardViewModel viewModel;
    private KeyBounds hoveredKey;
    private Button layoutButton;

    public KeyboardViewScreen(Screen parent) {
        super(Component.translatable("viewboard.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.rules.ensureLoaded();
        this.rules.syncRuntimeState();
        this.rebuildViewModel();

        int topButtonY = 36;
        this.layoutButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            this.rules.cycleLayout();
            this.rebuildViewModel();
            this.refreshButtons();
        }).bounds(18, topButtonY, 120, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("viewboard.screen.manage"), button ->
                Minecraft.getInstance().setScreen(new KeybindRulesScreen(this)))
            .bounds(this.width / 2 - 60, topButtonY, 120, 20)
            .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
            .bounds(this.width - 138, topButtonY, 120, 20)
            .build());

        this.refreshButtons();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderMenuBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredKey = null;
        this.rules.syncRuntimeState();
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderPanel(guiGraphics);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, COLOR_TEXT_LIGHT);
        guiGraphics.drawCenteredString(this.font, Component.translatable("viewboard.screen.subtitle"), this.width / 2, 18, COLOR_TEXT_SUBTLE);
        int legendBottom = this.renderLegend(guiGraphics, 64);
        this.renderKeyboard(guiGraphics, mouseX, mouseY, legendBottom + 10, this.height - 54);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.hoveredKey != null) {
            guiGraphics.renderTooltip(
                this.font,
                this.createTooltip(this.hoveredKey.key()).stream()
                    .map(component -> ClientTooltipComponent.create(component.getVisualOrderText()))
                    .toList(),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
        }
    }

    private void rebuildViewModel() {
        KeyboardViewModel baseModel = KeyboardLayout.build(this.rules.selectedLayout(), List.of());
        List<SerializedKey> customKeys = this.rules.customKeysForLayout(baseModel.representedKeys());
        this.viewModel = KeyboardLayout.build(this.rules.selectedLayout(), customKeys);
    }

    private void refreshButtons() {
        this.layoutButton.setMessage(Component.translatable("viewboard.screen.layout", Component.literal(this.rules.selectedLayout().displayName())));
    }

    private void renderPanel(GuiGraphics guiGraphics) {
        int left = 12;
        int top = 30;
        int right = this.width - 12;
        int bottom = this.height - 34;
        guiGraphics.fill(left, top, right, bottom, COLOR_PANEL);
        guiGraphics.fill(left, top, right, top + 1, COLOR_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, COLOR_BORDER);
    }

    private int renderLegend(GuiGraphics guiGraphics, int startY) {
        LegendItem[] items = new LegendItem[] {
            new LegendItem(COLOR_SINGLE, Component.translatable("viewboard.legend.single")),
            new LegendItem(COLOR_CONFLICT, Component.translatable("viewboard.legend.conflict")),
            new LegendItem(COLOR_UNUSED, Component.translatable("viewboard.legend.unused"))
        };

        int x = 24;
        int y = startY;
        int maxX = this.width - 24;
        for (LegendItem item : items) {
            int itemWidth = 20 + this.font.width(item.text()) + 12;
            if (x + itemWidth > maxX) {
                x = 24;
                y += 16;
            }

            this.drawLegendChip(guiGraphics, x, y, item.color(), item.text());
            x += itemWidth;
        }

        return y + 14;
    }

    private void drawLegendChip(GuiGraphics guiGraphics, int x, int y, int color, Component text) {
        guiGraphics.fill(x, y, x + 12, y + 12, color);
        guiGraphics.fill(x, y, x + 12, y + 1, COLOR_BORDER);
        guiGraphics.fill(x, y + 11, x + 12, y + 12, COLOR_BORDER);
        guiGraphics.fill(x, y, x + 1, y + 12, COLOR_BORDER);
        guiGraphics.fill(x + 11, y, x + 12, y + 12, COLOR_BORDER);
        guiGraphics.drawString(this.font, text, x + 18, y + 2, COLOR_TEXT_LIGHT, false);
    }

    private void renderKeyboard(GuiGraphics guiGraphics, int mouseX, int mouseY, int contentTop, int contentBottom) {
        float availableWidth = this.width - 44.0F;
        float availableHeight = Math.max(60.0F, contentBottom - contentTop - 6.0F);
        int unitSize = Math.max(10, (int) Math.floor(Math.min(
            availableWidth / this.viewModel.totalWidthUnits(),
            availableHeight / this.viewModel.totalHeightUnits()
        )));
        int keyboardWidth = Math.round(this.viewModel.totalWidthUnits() * unitSize);
        int keyboardHeight = Math.round(this.viewModel.totalHeightUnits() * unitSize);
        int startX = Math.max(18, (this.width - keyboardWidth) / 2);
        int startY = contentTop + Math.max(0, ((contentBottom - contentTop) - keyboardHeight) / 2);

        for (VisualSection section : this.viewModel.sections()) {
            int x = startX + Math.round(section.xUnits() * unitSize);
            int y = startY + Math.round(section.yUnits() * unitSize);
            int width = Math.round(section.widthUnits() * unitSize);
            int height = Math.round(section.heightUnits() * unitSize);

            guiGraphics.fill(x, y, x + width, y + height, COLOR_SECTION);
            guiGraphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
            guiGraphics.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
            guiGraphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
            guiGraphics.fill(x + width - 1, y, x + width, y + height, COLOR_BORDER);
        }

        for (VisualKey key : this.viewModel.keys()) {
            int x = startX + Math.round(key.xUnits() * unitSize);
            int y = startY + Math.round(key.yUnits() * unitSize);
            int keyWidth = Math.max(16, Math.round(key.widthUnits() * unitSize));
            int keyHeight = Math.max(16, Math.round(key.heightUnits() * unitSize));
            int color = this.getKeyColor(key.key().toInputKey());
            int textColor = color == COLOR_UNUSED ? COLOR_TEXT_DARK : COLOR_TEXT_LIGHT;

            guiGraphics.fill(x, y, x + keyWidth, y + keyHeight, color);
            guiGraphics.fill(x, y, x + keyWidth, y + 1, COLOR_BORDER);
            guiGraphics.fill(x, y + keyHeight - 1, x + keyWidth, y + keyHeight, COLOR_BORDER);
            guiGraphics.fill(x, y, x + 1, y + keyHeight, COLOR_BORDER);
            guiGraphics.fill(x + keyWidth - 1, y, x + keyWidth, y + keyHeight, COLOR_BORDER);

            this.drawFittedLabel(guiGraphics, this.font, key.label(), x, y, keyWidth, keyHeight, textColor);

            if (mouseX >= x && mouseX < x + keyWidth && mouseY >= y && mouseY < y + keyHeight) {
                this.hoveredKey = new KeyBounds(key, x, y, keyWidth, keyHeight);
            }
        }
    }

    private void drawFittedLabel(GuiGraphics guiGraphics, Font font, String label, int x, int y, int width, int height, int color) {
        String renderLabel = label;
        float scale = 1.0F;
        for (int attempt = 0; attempt < 2; attempt++) {
            int textWidth = font.width(renderLabel);
            float widthScale = (width - 4.0F) / Math.max(1.0F, (float) textWidth);
            float heightScale = (height - 4.0F) / Math.max(1.0F, (float) font.lineHeight);
            scale = Math.min(1.0F, Math.min(widthScale, heightScale));

            // If the label is extremely small, prefer a short fallback in the cell.
            if (attempt == 0 && scale < 0.35F && renderLabel.length() > 4) {
                renderLabel = renderLabel.substring(0, Math.min(3, renderLabel.length()));
                continue;
            }
            if (scale < 0.25F) {
                renderLabel = "…";
                textWidth = font.width(renderLabel);
                widthScale = (width - 4.0F) / Math.max(1.0F, (float) textWidth);
                heightScale = (height - 4.0F) / Math.max(1.0F, (float) font.lineHeight);
                scale = Math.min(1.0F, Math.min(widthScale, heightScale));
            }
            break;
        }

        int finalWidth = font.width(renderLabel);
        float drawX = x + (width - finalWidth * scale) / 2.0F;
        float drawY = y + (height - font.lineHeight * scale) / 2.0F;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(drawX, drawY);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.drawString(font, renderLabel, 0, 0, color, false);
        guiGraphics.pose().popMatrix();
    }

    private int getKeyColor(InputConstants.Key key) {
        ViewBoardKeybindRules.KeyUsageSummary summary = this.rules.usageFor(key);
        if (summary.states().isEmpty()) {
            return COLOR_UNUSED;
        }
        return summary.conflict() ? COLOR_CONFLICT : COLOR_SINGLE;
    }

    private List<Component> createTooltip(VisualKey key) {
        List<Component> tooltip = new ArrayList<>();
        ViewBoardKeybindRules.KeyUsageSummary summary = this.rules.usageFor(key.key().toInputKey());

        tooltip.add(Component.literal(key.label()));
        if (summary.states().isEmpty()) {
            tooltip.add(Component.translatable("viewboard.tooltip.unused"));
            return tooltip;
        }

        tooltip.add(Component.translatable(
            summary.states().size() == 1 ? "viewboard.tooltip.single" : "viewboard.tooltip.multiple",
            summary.states().size()));

        for (ViewBoardKeybindRules.KeyBindingState state : summary.states()) {
            StringBuilder line = new StringBuilder();
            line.append(Component.translatable(ViewBoardKeybindRules.categoryString(state.mapping())).getString());
            line.append(" - ");
            line.append(Component.translatable(state.mapping().getName()).getString());
            if (state.ignored()) {
                line.append(" [");
                line.append(Component.translatable("viewboard.tooltip.ignored").getString());
                line.append("]");
            }
            if (state.groupName() != null) {
                line.append(" [");
                line.append(Component.translatable("viewboard.tooltip.group", Component.literal(state.groupName())).getString());
                line.append("]");
            }
            tooltip.add(Component.literal(line.toString()));
        }

        return tooltip;
    }

    private record LegendItem(int color, Component text) {
    }

    private record KeyBounds(VisualKey key, int x, int y, int width, int height) {
    }
}
