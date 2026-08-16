package net.triax.visual.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.triax.visual.ModuleRegistry;
import net.triax.visual.TriaxVisualClient;
import net.triax.visual.render.RenderUtils2D;

import java.util.List;

/**
 * Рисует HUD-элементы мода: аккуратный watermark слева сверху и список
 * активных эффектов зелий под ним — оба в скруглённом стиле (см. RenderUtils2D).
 *
 * ВАЖНО: с версии 1.21.2 Fabric API перешёл на HudElementRegistry вместо
 * старого HudRenderCallback. Регистрация — в TriaxVisualClient. Если тут
 * что-то не скомпилируется (например у HudElement другая сигнатура метода
 * в твоей версии Fabric API) — пришли текст ошибки, поправим.
 */
public final class HudModule {

    private static final int ACCENT = 0xFF7C7CFF;
    private static final int PANEL_BG = 0xCC121218;
    private static final int PANEL_BORDER = 0xFF2A2A3A;
    private static final int TEXT_MAIN = 0xFFFFFFFF;
    private static final int TEXT_SUB = 0xFFB5B5C0;

    private HudModule() {}

    private static Text styled(String s, boolean bold, int color) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(TriaxVisualClient.FONT).withColor(color).withBold(bold));
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        int x = 10;
        int y = 10;

        if (TriaxVisualClient.hudEnabled) {
            y = drawWatermark(context, mc, x, y) + 6;
            drawPotionEffects(context, mc, x, y);
        }

        if (TriaxVisualClient.arrayListEnabled) {
            drawArrayList(context, mc);
        }
    }

    private static int drawWatermark(DrawContext context, MinecraftClient mc, int x, int y) {
        String title = "Triax Visual";
        int textWidth = mc.textRenderer.getWidth(styled(title, true, TEXT_MAIN));
        int panelWidth = textWidth + 22;
        int panelHeight = 18;

        RenderUtils2D.roundedPanel(context, x, y, panelWidth, panelHeight, 6, 1, PANEL_BORDER, PANEL_BG);

        // Маленький акцентный индикатор слева от текста.
        RenderUtils2D.roundedRect(context, x + 7, y + 6, 6, 6, 3, ACCENT);

        context.drawText(mc.textRenderer, styled(title, true, TEXT_MAIN), x + 18, y + 5, TEXT_MAIN, false);

        return y + panelHeight;
    }

    private static void drawPotionEffects(DrawContext context, MinecraftClient mc, int x, int y) {
        if (mc.player == null) return;

        List<StatusEffectInstance> effects = mc.player.getStatusEffects().stream().toList();
        if (effects.isEmpty()) return;

        int rowHeight = 20;
        int rowWidth = 150;

        for (StatusEffectInstance effect : effects) {
            RenderUtils2D.roundedPanel(context, x, y, rowWidth, rowHeight, 6, 1, PANEL_BORDER, PANEL_BG);

            // Иконка эффекта (стандартный спрайт ванильного Minecraft).
            var sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
            context.drawSprite(x + 4, y + 3, 14, 14, sprite);

            String name = effect.getEffectType().value().getName().getString();
            String amplifier = effect.getAmplifier() > 0 ? " " + toRoman(effect.getAmplifier() + 1) : "";
            String duration = formatDuration(effect);

            context.drawText(mc.textRenderer, styled(name + amplifier, false, TEXT_MAIN), x + 22, y + 4, TEXT_MAIN, false);
            context.drawText(mc.textRenderer, styled(duration, false, TEXT_SUB), x + 22, y + 13, TEXT_SUB, false);

            y += rowHeight + 4;
        }
    }

    private static void drawArrayList(DrawContext context, MinecraftClient mc) {
        List<ModuleRegistry.Module> enabled = ModuleRegistry.enabledForArrayList();
        if (enabled.isEmpty()) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int y = 10;

        for (var module : enabled) {
            int textWidth = mc.textRenderer.getWidth(styled(module.name(), true, ACCENT));
            int panelWidth = textWidth + 16;
            int x = screenWidth - panelWidth - 10;

            RenderUtils2D.roundedPanel(context, x, y, panelWidth, 16, 5, 1, PANEL_BORDER, PANEL_BG);
            context.drawText(mc.textRenderer, styled(module.name(), true, ACCENT), x + 8, y + 4, ACCENT, false);

            y += 20;
        }
    }

    private static String formatDuration(StatusEffectInstance effect) {
        if (effect.isInfinite()) return "\u221E"; // ∞
        int ticks = effect.getDuration();
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }

    private static String toRoman(int n) {
        // Хватает для амплификаторов зелий (1-10).
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return (n >= 1 && n <= romans.length) ? romans[n - 1] : String.valueOf(n);
    }
}
