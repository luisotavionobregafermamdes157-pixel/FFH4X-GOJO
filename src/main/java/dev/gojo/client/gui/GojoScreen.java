package dev.gojo.client.gui;

import dev.gojo.client.config.ConfigManager;
import dev.gojo.client.module.GojoModule;
import dev.gojo.client.module.ModuleCategory;
import dev.gojo.client.module.ModuleManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GojoScreen extends Screen {
    private static final int BG = 0xF0101018;
    private static final int PANEL = 0xFF191925;
    private static final int BLUE = 0xFF4B8DFF;
    private static final int PURPLE = 0xFF9A63FF;
    private ModuleCategory selected = ModuleCategory.COMBAT;
    private TextFieldWidget search;
    private int panelX, panelY, panelW, panelH;
    private int scroll;
    private String cachedQuery = null;
    private List<GojoModule> cachedVisible = List.of();

    public GojoScreen() { super(Text.literal("GOJO CLIENT")); }

    @Override protected void init() {
        panelW = Math.max(320, Math.min(650, width - 16));
        panelH = Math.max(240, Math.min(390, height - 16));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        search = new TextFieldWidget(textRenderer, panelX + 178, panelY + 43, panelW - 200, 20, Text.literal("Search modules"));
        search.setPlaceholder(Text.literal("Search modules..."));
        search.setChangedListener(value -> refreshFiltered());
        addDrawableChild(search);
        refreshFiltered();
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x99000000);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG);
        context.fill(panelX, panelY, panelX + panelW, panelY + 3, BLUE);
        context.fill(panelX, panelY + 3, panelX + panelW, panelY + 76, PANEL);
        context.drawText(textRenderer, "GOJO", panelX + 18, panelY + 14, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "CLIENT  /  1.21.11", panelX + 68, panelY + 17, 0xFF9A9AAF, false);
        context.drawText(textRenderer, "RIGHT SHIFT  ·  CLOSE", panelX + panelW - 145, panelY + 19, 0xFF77778A, false);
        context.fill(panelX + 14, panelY + 42, panelX + 162, panelY + 65, 0xFF222232);
        context.drawText(textRenderer, "MODULE MANAGER", panelX + 22, panelY + 49, 0xFFB9B9D0, false);
        context.fill(panelX + 14, panelY + 81, panelX + 146, panelY + panelH - 15, PANEL);
        int cy = panelY + 94;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean active = category == selected;
            if (active) context.fill(panelX + 19, cy - 5, panelX + 140, cy + 17, 0xFF2B2B49);
            context.fill(panelX + 19, cy - 5, panelX + 22, cy + 17, active ? PURPLE : 0xFF414152);
            context.drawText(textRenderer, category.name(), panelX + 30, cy, active ? 0xFFFFFFFF : 0xFF9696A9, false);
            cy += 34;
        }
        context.drawText(textRenderer, "CONFIG  ·  AUTO-SAVED", panelX + 25, panelY + panelH - 34, 0xFF77778A, false);
        int listX = panelX + 164;
        int listY = panelY + 78;
        int listW = panelW - 178;
        context.fill(listX, listY, listX + listW, panelY + panelH - 14, 0x99191925);
        List<GojoModule> visible = cachedVisible;
        int rowY = listY + 10 - scroll;
        for (GojoModule module : visible) {
            if (rowY + 42 > listY && rowY < panelY + panelH - 14) {
                context.fill(listX + 8, rowY, listX + listW - 8, rowY + 36, 0xFF222230);
                context.drawText(textRenderer, module.name(), listX + 18, rowY + 6, 0xFFE8E8F4, false);
                String description = module.description();
                if (description.length() > 53) description = description.substring(0, 50) + "...";
                context.drawText(textRenderer, description, listX + 18, rowY + 19, 0xFF85859B, false);
                int bx = listX + listW - 66;
                int color = module.implemented() ? (module.enabled() ? 0xFF245E83 : 0xFF343444) : 0xFF292932;
                context.fill(bx, rowY + 8, bx + 48, rowY + 28, color);
                String status = !module.implemented() ? "SOON" : module.enabled() ? "ON" : "OFF";
                int sw = textRenderer.getWidth(status);
                context.drawText(textRenderer, status, bx + (48 - sw) / 2, rowY + 14, module.enabled() ? 0xFF9EDBFF : 0xFFAAAAAF, false);
            }
            rowY += 44;
        }
        context.drawText(textRenderer, "" + visible.size() + " MODULES", listX + 10, panelY + panelH - 29, 0xFF77778A, false);
        super.render(context, mouseX, mouseY, delta);
    }

    private void refreshFiltered() {
        String query = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
        if (query.equals(cachedQuery)) return;
        cachedQuery = query;
        List<GojoModule> result = new ArrayList<>();
        for (GojoModule module : ModuleManager.all()) {
            if (module.category() == selected && (query.isBlank()
                    || module.name().toLowerCase(Locale.ROOT).contains(query)
                    || module.description().toLowerCase(Locale.ROOT).contains(query))) {
                result.add(module);
            }
        }
        cachedVisible = List.copyOf(result);
        clampScroll();
    }

    private void clampScroll() {
        int viewport = Math.max(0, panelH - 112);
        scroll = Math.max(0, Math.min(Math.max(0, cachedVisible.size() * 44 - viewport), scroll));
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            int catY = panelY + 94;
            for (ModuleCategory category : ModuleCategory.values()) {
                if (mouseX >= panelX + 15 && mouseX <= panelX + 146 && mouseY >= catY - 7 && mouseY <= catY + 19) {
                    selected = category; scroll = 0; cachedQuery = null; refreshFiltered(); return true;
                }
                catY += 34;
            }
            int listX = panelX + 164;
            int rowY = panelY + 88 - scroll;
            for (GojoModule module : cachedVisible) {
                if (mouseX >= listX + 8 && mouseX <= panelX + panelW - 14 && mouseY >= rowY && mouseY <= rowY + 36) {
                    module.toggle();
                    ConfigManager.save(ModuleManager.all());
                    return true;
                }
                rowY += 44;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) (verticalAmount * 22);
        clampScroll();
        return true;
    }

    @Override public boolean shouldPause() { return false; }
}
