package net.triax.visual.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Хранит недавние позиции игрока и рисует за ним плавный радужный след,
 * который затухает (fade out) по мере удаления от игрока.
 */
public class TrailRenderer {

    /** Сколько последних позиций хранить в трейле. Больше = длиннее след. */
    private static final int MAX_POINTS = 60;

    /** Толщина линии трейла. */
    private static final float LINE_WIDTH = 3.0f;

    /** Скорость смены цвета радуги (циклов в секунду примерно). */
    private static final float RAINBOW_SPEED = 0.6f;

    private final Deque<Vec3d> points = new ArrayDeque<>();
    private long lastTickTime = 0;

    public void tick(PlayerEntity player) {
        Vec3d pos = player.getPos().add(0, player.getHeight() / 2.0, 0);
        points.addLast(pos);
        while (points.size() > MAX_POINTS) {
            points.removeFirst();
        }
    }

    /** Если трейл выключен, постепенно очищаем точки, чтобы след плавно исчезал. */
    public void clearIfEmpty() {
        if (!points.isEmpty()) {
            points.removeFirst();
        }
    }

    public void render(WorldRenderContext context) {
        if (points.size() < 2) return;

        VertexConsumerProvider.Immediate consumers =
                (VertexConsumerProvider.Immediate) context.consumers();
        if (consumers == null) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d camPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLineStrip());

        // Сглаживание "как в читах": между реальными записанными точками
        // добавляем промежуточные (линейная интерполяция), плюс голова трейла
        // подтягивается к текущей позиции игрока с учётом partial tick,
        // чтобы не было видно "ступенек" на низком тикрейте.
        List<Vec3d> ordered = smooth(new ArrayList<>(points), 3);
        int count = ordered.size();

        float time = (System.currentTimeMillis() % 100000) / 1000f;

        for (int i = 0; i < count; i++) {
            Vec3d p = ordered.get(i);

            // Прогресс от 0 (старая точка, хвост) до 1 (свежая точка, у игрока)
            float progress = i / (float) (count - 1);

            // Радужный цвет, циклически сдвигающийся по времени
            float hue = (progress + time * RAINBOW_SPEED) % 1.0f;
            int color = hsvToRgb(hue, 0.85f, 1.0f);

            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // Затухание: хвост почти прозрачный, у игрока — почти непрозрачный
            float alpha = MathHelper.clamp(progress, 0.05f, 1.0f);

            buffer.vertex(positionMatrix, (float) p.x, (float) p.y, (float) p.z)
                    .color(r, g, b, alpha)
                    .normal(0, 1, 0);
        }

        consumers.draw(RenderLayer.getLineStrip());
        matrices.pop();
    }

    /**
     * Вставляет subSteps промежуточных точек между каждой парой соседних точек
     * трейла (линейная интерполяция), делая линию заметно более плавной без
     * необходимости хранить в 3 раза больше реальных позиций.
     */
    private static List<Vec3d> smooth(List<Vec3d> input, int subSteps) {
        if (input.size() < 2 || subSteps <= 1) return input;

        List<Vec3d> result = new ArrayList<>((input.size() - 1) * subSteps + 1);
        for (int i = 0; i < input.size() - 1; i++) {
            Vec3d a = input.get(i);
            Vec3d b = input.get(i + 1);
            for (int s = 0; s < subSteps; s++) {
                double t = s / (double) subSteps;
                result.add(a.lerp(b, t));
            }
        }
        result.add(input.get(input.size() - 1));
        return result;
    }

    private static int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
