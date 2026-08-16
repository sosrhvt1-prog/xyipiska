package net.triax.visual;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.triax.visual.gui.ClickGuiScreen;
import net.triax.visual.hud.HudModule;
import net.triax.visual.render.TrailRenderer;
import org.lwjgl.glfw.GLFW;

/**
 * Triax Visual — клиентский визуальный мод.
 * Right Shift открывает ClickGUI, в котором можно включить модули:
 * Trail, HUD (watermark + эффекты зелий), ArrayList (список включённых модулей).
 */
public class TriaxVisualClient implements ClientModInitializer {

    public static final String MOD_ID = "triaxvisual";

    public static boolean trailEnabled = false;
    public static boolean hudEnabled = true;
    public static boolean arrayListEnabled = true;

    /**
     * Кастомный шрифт мода — см. assets/triaxvisual/font/triax.json.
     * Файлы inter-medium.ttf / inter-bold.ttf нужно положить туда вручную
     * (см. PUT_FONT_FILES_HERE.txt в той же папке). Если файлов нет,
     * Minecraft просто использует шрифт по умолчанию — не крашится.
     */
    public static final Identifier FONT = Identifier.of(MOD_ID, "triax");

    private static KeyBinding openGuiKey;
    private static final TrailRenderer TRAIL_RENDERER = new TrailRenderer();

    @Override
    public void onInitializeClient() {

        // Регистрируем клавишу открытия GUI — по умолчанию Right Shift.
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.triaxvisual.opengui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.triaxvisual.main"
        ));

        // Общий реестр модулей — читается и ClickGUI, и ArrayList.
        ModuleRegistry.register("Trail", true, () -> trailEnabled, v -> trailEnabled = v);
        ModuleRegistry.register("HUD", false, () -> hudEnabled, v -> hudEnabled = v);
        ModuleRegistry.register("ArrayList", false, () -> arrayListEnabled, v -> arrayListEnabled = v);

        // Каждый клиентский тик: проверяем нажатие клавиши и обновляем позиции трейла.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }

            if (trailEnabled && client.player != null) {
                TRAIL_RENDERER.tick(client.player);
            } else {
                TRAIL_RENDERER.clearIfEmpty();
            }
        });

        // Рендер трейла в мире, после отрисовки сущностей.
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (trailEnabled) {
                TRAIL_RENDERER.render(context);
            }
        });

        // HUD-элемент: watermark, эффекты зелий, arraylist.
        // NB: HudElementRegistry — актуальный API Fabric с 1.21.2+ (заменил
        // старый HudRenderCallback). Если у тебя другая версия Fabric API и
        // класс не найден — пришли ошибку компиляции, поправим под неё.
        HudElementRegistry.addLast(Identifier.of(MOD_ID, "hud"), HudModule::render);
    }

    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }
}
