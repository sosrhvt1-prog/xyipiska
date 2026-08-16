package net.triax.visual.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.triax.visual.render.RenderUtils2D;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * ClickGUI мода Triax Visual: несколько категорий-панелей в ряд, каждая со
 * списком модулей. Все панели и ряды — со скруглёнными углами (см. RenderUtils2D).
 *
 * Рабочая (кликабельная) категория — "Visual": Trail, HUD, ArrayList.
 * Остальные категории показаны только для визуального сходства с референсом
 * (макет пользователя) — их модули (Aim Assist, Aura, Fly и т.п.) намеренно
 * НЕ реализованы: это были бы чит-функции с нечестным преимуществом в
 * мультиплеере. Строки в этих колонках задизейблены и некликабельны.
 */
public class ClickGuiScreen extends Screen {

    private static final int PANEL_WIDTH = 148;
    private static final int PANEL_GAP = 10;
    private static final int HEADER_HEIGHT = 26;
    private static final int ROW_HEIGHT = 18;
    private static final int TOP_MARGIN = 40;
    private static final int RADIUS = 8;
    private static final int ROW_RADIUS = 5;

    private static final int PANEL_BG = 0xE6121218;
    private static final int PANEL_BORDER = 0xFF2A2A3A;
    private static final int HEADER_BG = 0xF01B1B26;

    private final List<Category> categories = buildCategories();
    private final List<Row> clickableRows = new ArrayList<>();

    public ClickGuiScreen() {
        super(Text.literal("Triax Visual"));
    }

    @Override
    protected void init() {
        clickableRows.clear();
    }

    private Text styled(String s, boolean bold, int color) {
        Style style = Style.EMPTY
                .withFont(net.triax.visual.TriaxVisualClient.FONT)
                .withColor(color)
                .withBold(bold);
        return Text.literal(s).setStyle(style);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        clickableRows.clear();

        int totalWidth = categories.size() * PANEL_WIDTH + (categories.size() - 1) * PANEL_GAP;
        int startX = (this.width - totalWidth) / 2;
        int x = startX;

        for (Category cat : categories) {
            renderCategory(context, cat, x, TOP_MARGIN, mouseX, mouseY);
            x += PANEL_WIDTH + PANEL_GAP;
        }

        context.drawText(this.textRenderer, styled("Triax Visual", true, 0xFFFFFF),
                startX, TOP_MARGIN - 24, 0xFFFFFF, true);
        context.drawText(this.textRenderer, styled("Right Shift чтобы закрыть", false, 0x8A8A99),
                startX, TOP_MARGIN - 12, 0x8A8A99, false);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCategory(DrawContext context, Category cat, int x, int y, int mouseX, int mouseY) {
        int rowsHeight = cat.modules.size() * ROW_HEIGHT;
        int panelHeight = HEADER_HEIGHT + rowsHeight + 6;

        RenderUtils2D.roundedPanel(context, x, y, PANEL_WIDTH, panelHeight, RADIUS, 1, PANEL_BORDER, PANEL_BG);
        RenderUtils2D.roundedRect(context, x, y, PANEL_WIDTH, HEADER_HEIGHT, RADIUS, HEADER_BG,
                true, true, false, false);

        context.drawText(this.textRenderer, styled(cat.icon + "  " + cat.name, true, 0xFFFFFF),
                x + 10, y + 9, 0xFFFFFF, false);

        int rowY = y + HEADER_HEIGHT + 4;
        for (ModuleDef module : cat.modules) {
            boolean enabled = module.functional && module.getter.getAsBoolean();
            int textColor = !module.functional ? 0x50505A : (enabled ? 0xFFFFFF : 0xB5B5C0);

            boolean hovered = module.functional
                    && mouseX >= x + 6 && mouseX <= x + PANEL_WIDTH - 6
                    && mouseY >= rowY - 2 && mouseY <= rowY + ROW_HEIGHT - 4;

            if (hovered) {
                RenderUtils2D.roundedRect(context, x + 4, rowY - 2, PANEL_WIDTH - 8, ROW_HEIGHT - 2, ROW_RADIUS, 0x25FFFFFF);
            } else if (module.functional && enabled) {
                RenderUtils2D.roundedRect(context, x + 4, rowY - 2, PANEL_WIDTH - 8, ROW_HEIGHT - 2, ROW_RADIUS, 0x1A55E07A);
            }

            context.drawText(this.textRenderer, styled(module.name, enabled, textColor),
                    x + 12, rowY, textColor, false);

            if (module.functional) {
                int dotColor = enabled ? 0xFF55E07A : 0xFF56565A;
                RenderUtils2D.roundedRect(context, x + PANEL_WIDTH - 18, rowY + 2, 6, 6, 3, dotColor);
                clickableRows.add(new Row(x + 6, rowY - 2, PANEL_WIDTH - 12, ROW_HEIGHT - 2, module));
            } else {
                context.drawText(this.textRenderer, styled("...", false, 0x45454E),
                        x + PANEL_WIDTH - 24, rowY, 0x45454E, false);
            }

            rowY += ROW_HEIGHT;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Row row : clickableRows) {
                if (mouseX >= row.x && mouseX <= row.x + row.w && mouseY >= row.y && mouseY <= row.y + row.h) {
                    row.module.setter.accept(!row.module.getter.getAsBoolean());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // ---------------------------------------------------------------------

    private record Row(int x, int y, int w, int h, ModuleDef module) {}

    private static class ModuleDef {
        final String name;
        final boolean functional;
        final BooleanSupplier getter;
        final Consumer<Boolean> setter;

        ModuleDef(String name, boolean functional, BooleanSupplier getter, Consumer<Boolean> setter) {
            this.name = name;
            this.functional = functional;
            this.getter = getter;
            this.setter = setter;
        }

        static ModuleDef disabled(String name) {
            return new ModuleDef(name, false, () -> false, b -> {});
        }
    }

    private static class Category {
        final String name;
        final String icon;
        final List<ModuleDef> modules;

        Category(String name, String icon, List<ModuleDef> modules) {
            this.name = name;
            this.icon = icon;
            this.modules = modules;
        }
    }

    private static List<Category> buildCategories() {
        List<Category> list = new ArrayList<>();

        // Единственная рабочая категория.
        list.add(new Category("Visual", "\u2726", List.of(
                new ModuleDef("Trail", true,
                        () -> net.triax.visual.TriaxVisualClient.trailEnabled,
                        v -> net.triax.visual.TriaxVisualClient.trailEnabled = v),
                new ModuleDef("HUD", true,
                        () -> net.triax.visual.TriaxVisualClient.hudEnabled,
                        v -> net.triax.visual.TriaxVisualClient.hudEnabled = v),
                new ModuleDef("ArrayList", true,
                        () -> net.triax.visual.TriaxVisualClient.arrayListEnabled,
                        v -> net.triax.visual.TriaxVisualClient.arrayListEnabled = v)
        )));

        // Остальные категории — для сходства с макетом, намеренно не реализованы.
        list.add(new Category("Combat", "\u2694", List.of(
                ModuleDef.disabled("Aim Assist"),
                ModuleDef.disabled("Aura"),
                ModuleDef.disabled("Criticals"),
                ModuleDef.disabled("Hit Sound")
        )));

        list.add(new Category("Movement", "\u2726", List.of(
                ModuleDef.disabled("Auto Sprint"),
                ModuleDef.disabled("Fly"),
                ModuleDef.disabled("Speed"),
                ModuleDef.disabled("Strafe")
        )));

        list.add(new Category("Player", "\u263A", List.of(
                ModuleDef.disabled("Free Look"),
                ModuleDef.disabled("Name Protect"),
                ModuleDef.disabled("No Delay")
        )));

        list.add(new Category("Misc", "\u2699", List.of(
                ModuleDef.disabled("Client Sounds"),
                ModuleDef.disabled("Death Coords")
        )));

        return list;
    }
}
