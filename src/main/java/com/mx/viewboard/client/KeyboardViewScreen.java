package com.mx.viewboard.client;

import com.mx.viewboard.client.layout.KeyboardKey;
import com.mx.viewboard.client.layout.KeyboardLayout;
import com.mx.viewboard.client.layout.KeyboardSection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
    private static final int COLOR_PANEL = 0xF01A1A1A;
    private static final int COLOR_SECTION = 0x662C2C2C;
    private static final int COLOR_TEXT_DARK = 0xFF000000;
    private static final int COLOR_TEXT_LIGHT = 0xFFFFFFFF;

    private final Screen parent;
    private final Map<Integer, List<KeyMapping>> bindingsByKey = new HashMap<>();
    private KeyBounds hoveredKey;

    public KeyboardViewScreen(Screen parent) {
        super(Component.translatable("viewboard.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.rebuildBindings();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button ->
                this.onClose())
            .bounds(this.width / 2 - 75, this.height - 28, 150, 20)
            .build());
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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderPanel(guiGraphics);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, COLOR_TEXT_LIGHT);
        guiGraphics.drawCenteredString(this.font, Component.translatable("viewboard.screen.subtitle"), this.width / 2, 24, 0xFFB8B8B8);
        this.renderLegend(guiGraphics);
        this.renderKeyboard(guiGraphics, mouseX, mouseY);

        for (var renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (this.hoveredKey != null) {
            guiGraphics.renderTooltip(this.font, this.createTooltip(this.hoveredKey.key()).stream().map(component -> ClientTooltipComponent.create(component.getVisualOrderText())).toList(), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    public boolean mouseScrolled(double scrollX, double scrollY, double scrollDelta) {
        if (scrollDelta > 0 && this.hoveredKey != null) {
            // Scroll up
        }
        if (scrollDelta < 0 && this.hoveredKey != null) {
            // Scroll down  
        }
        return false;
    }

    private void rebuildBindings() {
        this.bindingsByKey.clear();
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            this.bindingsByKey.computeIfAbsent(mapping.getKey().getValue(), unused -> new ArrayList<>()).add(mapping);
        }

        for (List<KeyMapping> mappings : this.bindingsByKey.values()) {
            mappings.sort(Comparator
                .comparing((KeyMapping mapping) -> Component.translatable(mapping.getCategory().toString()).getString())
                .thenComparing(mapping -> Component.translatable(mapping.getName()).getString()));
        }
    }

    private void renderLegend(GuiGraphics guiGraphics) {
        int legendY = 42;
        Component[] labels = new Component[] {
            Component.translatable("viewboard.legend.single"),
            Component.translatable("viewboard.legend.conflict"),
            Component.translatable("viewboard.legend.unused")
        };
        int[] colors = new int[] {COLOR_SINGLE, COLOR_CONFLICT, COLOR_UNUSED};
        int chipWidth = 12;
        int textGap = 8;
        int panelLeft = 28;
        int panelRight = this.width - 28;
        int totalItemWidth = 0;

        for (Component label : labels) {
            totalItemWidth += chipWidth + textGap + this.font.width(label);
        }

        int spacing = Math.max(18, (panelRight - panelLeft - totalItemWidth) / 4);
        int x = panelLeft + spacing;
        for (int i = 0; i < labels.length; i++) {
            int itemWidth = chipWidth + textGap + this.font.width(labels[i]);
            this.drawLegendChip(guiGraphics, x, legendY, colors[i], labels[i]);
            x += itemWidth + spacing;
        }
    }

    private void drawLegendChip(GuiGraphics guiGraphics, int x, int y, int color, Component text) {
        guiGraphics.fill(x, y, x + 12, y + 12, color);
        guiGraphics.fill(x, y, x + 12, y + 1, COLOR_BORDER);
        guiGraphics.fill(x, y + 11, x + 12, y + 12, COLOR_BORDER);
        guiGraphics.fill(x, y, x + 1, y + 12, COLOR_BORDER);
        guiGraphics.fill(x + 11, y, x + 12, y + 12, COLOR_BORDER);
        guiGraphics.drawString(this.font, text, x + 18, y + 2, COLOR_TEXT_LIGHT, false);
    }

    private void renderPanel(GuiGraphics guiGraphics) {
        int left = 14;
        int top = 36;
        int right = this.width - 14;
        int bottom = this.height - 48;
        guiGraphics.fill(left, top, right, bottom, COLOR_PANEL);
        guiGraphics.fill(left, top, right, top + 1, COLOR_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, COLOR_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, COLOR_BORDER);
    }

    private void renderKeyboard(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float availableWidth = this.width - 52.0F;
        float availableHeight = this.height - 132.0F;
        int unitSize = Math.max(14, (int) Math.floor(Math.min(
            availableWidth / KeyboardLayout.totalWidthUnits(),
            availableHeight / KeyboardLayout.totalHeightUnits()
        )));
        int keyboardWidth = Math.round(KeyboardLayout.totalWidthUnits() * unitSize);
        int keyboardHeight = Math.round(KeyboardLayout.totalHeightUnits() * unitSize);
        int startX = (this.width - keyboardWidth) / 2;
        int startY = 58;

        for (KeyboardSection section : KeyboardLayout.sections()) {
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

        for (KeyboardKey key : KeyboardLayout.keys()) {
            int x = startX + Math.round(key.xUnits() * unitSize);
            int y = startY + Math.round(key.yUnits() * unitSize);
            int keyWidth = Math.max(18, Math.round(key.widthUnits() * unitSize));
            int keyHeight = Math.max(18, unitSize);
            int color = this.getKeyColor(key.keyCode());
            int textColor = color == COLOR_UNUSED ? COLOR_TEXT_DARK : COLOR_TEXT_LIGHT;
            int textX = x + (keyWidth - this.font.width(key.label())) / 2;
            int textY = y + (keyHeight - this.font.lineHeight) / 2;

            guiGraphics.fill(x, y, x + keyWidth, y + keyHeight, color);
            guiGraphics.fill(x, y, x + keyWidth, y + 1, COLOR_BORDER);
            guiGraphics.fill(x, y + keyHeight - 1, x + keyWidth, y + keyHeight, COLOR_BORDER);
            guiGraphics.fill(x, y, x + 1, y + keyHeight, COLOR_BORDER);
            guiGraphics.fill(x + keyWidth - 1, y, x + keyWidth, y + keyHeight, COLOR_BORDER);
            guiGraphics.drawString(this.font, key.label(), textX, textY, textColor, false);

            if (mouseX >= x && mouseX < x + keyWidth && mouseY >= y && mouseY < y + keyHeight) {
                this.hoveredKey = new KeyBounds(key, x, y, keyWidth, keyHeight);
            }
        }
    }

    private int getKeyColor(int keyCode) {
        int usageCount = this.bindingsByKey.getOrDefault(keyCode, List.of()).size();
        if (usageCount == 0) {
            return COLOR_UNUSED;
        }
        return usageCount == 1 ? COLOR_SINGLE : COLOR_CONFLICT;
    }

    private List<Component> createTooltip(KeyboardKey key) {
        List<Component> tooltip = new ArrayList<>();
        List<KeyMapping> mappings = this.bindingsByKey.getOrDefault(key.keyCode(), List.of());

        tooltip.add(Component.literal(key.label()));
        if (mappings.isEmpty()) {
            tooltip.add(Component.translatable("viewboard.tooltip.unused"));
            return tooltip;
        }

        tooltip.add(Component.translatable(
            mappings.size() == 1 ? "viewboard.tooltip.single" : "viewboard.tooltip.multiple",
            mappings.size()));
        for (KeyMapping mapping : mappings) {
            tooltip.add(Component.literal(
                Component.translatable(mapping.getCategory().toString()).getString() + " - " +
                    Component.translatable(mapping.getName()).getString()));
        }
        return tooltip;
    }

    private record KeyBounds(KeyboardKey key, int x, int y, int width, int height) {
    }
}
