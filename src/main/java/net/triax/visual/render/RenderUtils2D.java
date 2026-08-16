package net.triax.visual.render;

import net.minecraft.client.gui.DrawContext;

/**
 * Рисование прямоугольников со скруглёнными углами.
 *
 * Специально построено только на стабильном публичном
 * {@code DrawContext.fill(x1, y1, x2, y2, color)}, без обращения к
 * низкоуровневым Tessellator/BufferBuilder/RenderLayer API — та часть
 * рендер-пайплайна Minecraft чаще всего меняется между версиями и требует
 * доступа к декомпилированным исходникам для проверки, а этот метод не
 * ломается годами.
 */
public final class RenderUtils2D {

    private RenderUtils2D() {}

    /** Заливка прямоугольника со скруглёнными углами радиуса radius (все 4 угла). */
    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        roundedRect(ctx, x, y, w, h, radius, color, true, true, true, true);
    }

    /**
     * То же самое, но с возможностью выбрать, какие углы скруглять —
     * полезно для хедеров панелей (скруглены только верхние углы, низ ровно
     * стыкуется с телом панели без "квадратных" артефактов на стыке).
     */
    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int color,
                                    boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        if (w <= 0 || h <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));

        if (radius == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }

        // "Крест": всё, кроме четырёх угловых квадратов radius x radius.
        ctx.fill(x + radius, y, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + radius, y + h - radius, color);
        ctx.fill(x + w - radius, y + radius, x + w, y + h - radius, color);

        // Углы: либо скруглённая дуга, либо обычный квадрат (если флаг выключен).
        drawCornerOrSquare(ctx, x, y, radius, color, -1, -1, roundTL);
        drawCornerOrSquare(ctx, x + w - radius, y, radius, color, 1, -1, roundTR);
        drawCornerOrSquare(ctx, x, y + h - radius, radius, color, -1, 1, roundBL);
        drawCornerOrSquare(ctx, x + w - radius, y + h - radius, radius, color, 1, 1, roundBR);
    }

    private static void drawCornerOrSquare(DrawContext ctx, int cornerX, int cornerY, int radius, int color,
                                            int dirX, int dirY, boolean round) {
        if (!round) {
            ctx.fill(cornerX, cornerY, cornerX + radius, cornerY + radius, color);
            return;
        }
        int cx = dirX < 0 ? cornerX + radius : cornerX;
        int cy = dirY < 0 ? cornerY + radius : cornerY;
        drawCorner(ctx, cx, cy, radius, color, dirX, dirY);
    }

    /**
     * Панель с рамкой: снаружи цвет рамки, внутри (с отступом thickness) — цвет заливки.
     * Удобно для карточек ClickGUI/HUD.
     */
    public static void roundedPanel(DrawContext ctx, int x, int y, int w, int h, int radius,
                                     int thickness, int borderColor, int fillColor) {
        roundedRect(ctx, x, y, w, h, radius, borderColor);
        roundedRect(ctx, x + thickness, y + thickness, w - thickness * 2, h - thickness * 2,
                Math.max(0, radius - thickness), fillColor);
    }

    private static void drawCorner(DrawContext ctx, int cx, int cy, int radius, int color, int dirX, int dirY) {
        for (int dy = 0; dy < radius; dy++) {
            int dx = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
            int rowY = dirY < 0 ? cy - dy - 1 : cy + dy;
            int rowX1 = dirX < 0 ? cx - dx : cx;
            int rowX2 = dirX < 0 ? cx : cx + dx;
            ctx.fill(rowX1, rowY, rowX2, rowY + 1, color);
        }
    }
}
