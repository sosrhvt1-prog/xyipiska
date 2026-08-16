# Triax Visual

Клиентский визуальный мод для Fabric (Minecraft 1.21.4).

## Функции
- **Right Shift** — открывает ClickGUI (многоколоночный дизайн, скруглённые панели).
- **Trail** — плавный (интерполированный) радужный затухающий след за игроком.
- **HUD** — красивый watermark ("Triax Visual") + список активных эффектов
  зелий (иконка, название, оставшееся время), в скруглённых панельках.
- **ArrayList** — список сейчас включённых модулей, справа сверху экрана.

## Структура проекта
```
TriaxVisual/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── src/main/
    ├── java/net/triax/visual/
    │   ├── TriaxVisualClient.java     // entrypoint, keybinding, регистрация HUD
    │   ├── ModuleRegistry.java        // общий список модулей (ClickGUI + ArrayList)
    │   ├── gui/ClickGuiScreen.java    // ClickGUI
    │   ├── hud/HudModule.java         // watermark + эффекты зелий + arraylist
    │   └── render/
    │       ├── TrailRenderer.java     // рендер радужного следа
    │       └── RenderUtils2D.java     // скруглённые панели (без рискованных low-level API)
    └── resources/
        └── fabric.mod.json
```

### Про остальные категории в GUI (Combat / Movement / Player / Misc)
Присутствуют только для визуального сходства с макетом. Модули в них (Aim
Assist, Aura, Fly и т.д.) **намеренно не реализованы** — это были бы
чит-функции с нечестным преимуществом в мультиплеере. Строки задизейблены.

### Про шрифт
Кастомный TTF-шрифт пока убран, GUI/HUD используют стандартный шрифт
Minecraft — добавим кастомный позже отдельным шагом.

## Сборка
Нужен Gradle Wrapper — возьми `gradlew`/`gradlew.bat` + `gradle/wrapper/` из
официального https://github.com/FabricMC/fabric-example-mod (просто
скопируй эти файлы в корень, не трогая мой код).

```bash
./gradlew build
```

Jar появится в `build/libs/triaxvisual-1.0.0.jar`.

## Возможные проблемы при сборке
Два места в коде наиболее чувствительны к точной версии Fabric API/маппингов
для 1.21.4 и могут потребовать мелкой правки (без доступа в интернет я не
могу свериться 1-в-1 с актуальными исходниками):
1. `TriaxVisualClient.java` — регистрация `HudElementRegistry.addLast(...)`
   (актуальный способ добавить HUD-элемент начиная с Fabric API 1.21.2+).
2. Если ошибка — пришли мне текст ошибки компиляции, поправлю сразу.

## Установка
1. Fabric Loader для Minecraft 1.21.4.
2. В `.minecraft/mods/`: `triaxvisual-1.0.0.jar` + `fabric-api` (Modrinth/CurseForge).
3. Запусти игру через профиль Fabric.
# xyipiska
