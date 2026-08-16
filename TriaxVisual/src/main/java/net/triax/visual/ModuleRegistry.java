package net.triax.visual;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Общий список всех модулей мода: и ClickGUI, и ArrayList берут данные
 * отсюда, чтобы не дублировать состояние в двух местах.
 */
public final class ModuleRegistry {

    public record Module(String name, boolean showInArrayList, BooleanSupplier getter, Consumer<Boolean> setter) {
        public boolean isEnabled() {
            return getter.getAsBoolean();
        }

        public void toggle() {
            setter.accept(!getter.getAsBoolean());
        }
    }

    private static final Map<String, Module> MODULES = new LinkedHashMap<>();

    private ModuleRegistry() {}

    public static void register(String name, boolean showInArrayList, BooleanSupplier getter, Consumer<Boolean> setter) {
        MODULES.put(name, new Module(name, showInArrayList, getter, setter));
    }

    public static List<Module> all() {
        return List.copyOf(MODULES.values());
    }

    /** Модули, включённые прямо сейчас и помеченные для показа в ArrayList. */
    public static List<Module> enabledForArrayList() {
        return MODULES.values().stream()
                .filter(m -> m.showInArrayList && m.isEnabled())
                .collect(Collectors.toList());
    }
}
