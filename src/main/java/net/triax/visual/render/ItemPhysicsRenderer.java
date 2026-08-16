package net.triax.visual.render;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.triax.visual.TriaxVisualClient;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Переопределяет рендер выброшенных предметов: вместо ванильного постоянного
 * вращения вокруг Y — предмет "падает" физично и оседает под случайным,
 * но стабильным (per-entity) углом наклона, слегка покачиваясь.
 *
 * ВАЖНО: начиная с 1.20.5 Mojang переделали рендер сущностей на систему
 * render-state (ItemEntityRenderState). Этот класс написан по актуальной на
 * момент написания структуре, но у меня нет доступа к декомпилированным
 * исходникам 1.21.4 в этой сессии, чтобы свериться 1-в-1. Если при сборке
 * будет ошибка компиляции здесь (например, другое имя поля/метода) — пришли
 * мне текст ошибки, я поправлю под твои маппинги.
 */
public class ItemPhysicsRenderer extends ItemEntityRenderer {

    // Стабильный "случайный" наклон и скорость покачивания на каждую сущность,
    // чтобы предметы не дёргались и не рандомились каждый кадр.
    // WeakHashMap с ключом на сам объект state: когда предмет исчезает и
    // движок перестаёт его рендерить, объект state становится недостижим и
    // запись сама уходит из мапы сборщиком мусора — утечки памяти не будет.
    private final Map<ItemEntityRenderState, float[]> physicsData = new WeakHashMap<>();

    public ItemPhysicsRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(ItemEntityRenderState state, MatrixStack matrices,
                        net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light) {

        if (!TriaxVisualClient.itemPhysicsEnabled) {
            // Модуль выключен — обычный ванильный рендер.
            super.render(state, matrices, vertexConsumers, light);
            return;
        }

        // У ItemEntityRenderState нет поля id сущности, но сам объект state
        // переиспользуется движком для одного и того же предмета каждый
        // кадр — поэтому используем сам объект как ключ (см. WeakHashMap выше).
        int seed = System.identityHashCode(state);
        float[] data = physicsData.computeIfAbsent(state, k -> new float[]{
                // Псевдо-случайный, но детерминированный угол наклона по X и Z
                // на основе identity сущности — у каждого предмета свой устойчивый "лежачий" угол.
                (hash(seed, 1) % 40 - 20), // tiltX градусы (-20..20)
                (hash(seed, 2) % 360),     // yaw градусы (0..360), фиксированное направление
                (hash(seed, 3) % 100) / 100f // фаза покачивания
        });

        float tiltX = data[0];
        float baseYaw = data[1];
        float wobblePhase = data[2];

        // Лёгкое покачивание туда-сюда, как будто предмет ещё немного оседает.
        float wobble = MathHelper.sin((state.age + wobblePhase * 100) / 14f) * 3.0f;

        matrices.push();
        // Наклон "лёжа" вместо вертикального вращения + небольшое покачивание.
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(baseYaw + wobble));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(90.0f + tiltX));

        // Небольшой bounce по высоте, синхронизированный с покачиванием.
        float bounce = Math.abs(MathHelper.sin((state.age + wobblePhase * 100) / 14f)) * 0.02f;
        matrices.translate(0, bounce, 0);

        super.render(state, matrices, vertexConsumers, light);

        matrices.pop();
    }

    private static long hash(int id, int salt) {
        long h = id * 341873128712L + salt * 132897987541L;
        h ^= (h >>> 33);
        return Math.abs(h);
    }
}
