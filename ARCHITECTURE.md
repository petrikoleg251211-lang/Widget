# Widget Studio X — Архітектура та Технічна Документація

## 1. Архітектура системи та Межі відповідальності

**Widget Studio X** побудовано за модульною Clean/MVVM архітектурою з чітким розмежуванням між інтерактивним середовищем розробки (Jetpack Compose IDE) та системним механізмом домашніх віджетів Android (`AppWidgetProvider` + `RemoteViews`):

1. **Application Core (`MainActivity.kt`, `StudioViewModel.kt`, `SettingsPreferences.kt`)**:
   - Керування життєвим циклом, станом робочого простору (`EditorWorkspaceState`), навігацією між 11 студіями з підтримкою системної кнопки Back (`BackHandler`), адаптивною компоновкою (`NavigationBar` для смартфонів, `NavigationRail` для планшетів/великих екранів) та централізованими налаштуваннями в `DataStore`.
2. **Project Manager & Persistence (`StudioDatabase.kt`, `StudioRepository.kt`)**:
   - Локальна база даних **Room (SQLite + KSP)** зі таблицями `widget_projects`, `project_versions` (історія версій і відновлення після збоїв), `installed_plugins` та `studio_logs`.
   - Імпорт та експорт проєктів у форматі пакета `WidgetStudioX.Bundle` (JSON) із перевіркою версії схеми та скануванням безпеки перед імпортом.
3. **Visual Widget Editor (`VisualEditorScreen.kt`, `WidgetCanvasRenderer.kt`)**:
   - Повноцінний WYSIWYG-редактор із підтримкою 15 типів компонентів (`TEXT`, `SHAPE`, `CONTAINER`, `BUTTON`, `PROGRESS_BAR`, `CLOCK`, `DATE_CALENDAR`, `BATTERY`, `SYSTEM_METRIC`, `ICON`, `DIVIDER_LINE`, `CUSTOM_GRAPHIC`, `INTERACTIVE_ZONE`, `ANIMATED_PARTICLE`, `GAME_OBJECT`), перетягуванням (Drag & Drop), прив'язкою до сітки 8dp, групуванням, блокуванням шарів, буфером обміну (Copy/Paste) та стеком **Undo/Redo**.
4. **WidgetScript Engine & Code Studio (`WidgetScriptEngine.kt`, `CodeStudioScreen.kt`)**:
   - Спеціалізована DSL та інтерпретатор **WidgetScript v1.0** із двосторонньою синхронізацією між Візуальним редактором та кодом.
   - Підтримка багатофайлових проєктів (`main.ws`, `animations.ws`, `game_logic.ws`, `manifest.json`), підсвічування синтаксису, нумерації рядків, автодоповнення, форматування, пошуку/заміни, дерева символів, діагностики з номерами рядків та консолі виконання.
5. **Animation Engine (`AnimationEngine.kt`, `AnimationAndGameScreen.kt`)**:
   - Таймлайн із ключовими кадрами (`Keyframe`), кривими плавності (`LINEAR`, `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`, `SPRING`, `BOUNCE`), тригерами подій та генератором частинок (`CYBER_SPARKS`, `NEBULA_ORBS`, `PULSE_RINGS`, `MATRIX_FLOW`) із підтримкою режиму енергозбереження.
6. **Mini Game Engine 2D (`MiniGameEngine.kt`, `AnimationAndGameScreen.kt`)**:
   - Ігровий цикл ~50-60 FPS для інтерактивного перегляду з колізіями AABB/кіл, фізикою швидкості й гравітації, режимом налагодження хітбоксів та 3 ігровими шаблонами (`Astro Dodger 2D`, `Cyber Pong Arcade`, `Orbit Pulse Tap`).
7. **Plugin System (`BuiltInCatalog.kt`, `PluginsScreen.kt`)**:
   - Валідатор маніфестів плагінів, перевірка сумісності з `STUDIO_API_VERSION = "1.0"`, контроль конфліктів просторів імен та автоматичне блокування небезпечних системних дозволів (`READ_SMS`, `CAMERA`, `RECORD_AUDIO` тощо).
8. **Android AppWidget Integration (`StudioAppWidgetProvider.kt`, `WidgetBitmapRenderer.kt`)**:
   - Системний `AppWidgetProvider`, який рендерить поточний закріплений проєкт у `Bitmap` через `android.graphics.Canvas` з реальними системними даними (час, дата, заряд батареї, вільна RAM) та обробляє інтерактивні натискання через `PendingIntent` (`ACTION_WIDGET_TAP`).

## 2. Обґрунтування вибору WidgetScript та Моделі Безпеки (Sandbox)

- **Чому власна спеціалізована DSL/AST-мова (`WidgetScript`) замість необмеженого JVM/Kotlin Scripting або WebView JS?**
  - Повний детермінізм і миттєвий запуск без накладних витрат компілятора у рантаймі.
  - **Сувора ізоляція (Zero-Trust Sandbox)**:
    - Ліміт кількості інструкцій на один цикл виконання (`maxInstructions = 2000`, налаштовується користувачем), що унеможливлює зависання UI через нескінченні цикли.
    - Ліміт глибини рекурсії (`maxRecursionDepth = 24`) та часу виконання (`80 мс`).
    - Статичний та динамічний фільтр заборонених викликів (`Runtime.getRuntime`, `ProcessBuilder`, `java.io.File`, `System.exit`).
    - Доступ лише до білого списку безпечних просторів імен: `system.*`, `ui.*`, `graphics.*`, `animation.*`, `storage.*`, `timer.*`, `game.*`, `audio.*`, `network.fetchCached`, `debug.*`.

## 3. Розмежування можливостей та Відомі обмеження Android AppWidgets

- **Стандартний домашній віджет Android (`StudioAppWidgetProvider`)**: працює в межах системних обмежень `RemoteViews` та батареї Android — підтримує кастомну Canvas-графіку будь-якої складності, системну телеметрію та реакцію на натискання (`on tap`), але не виконує безперервний 60 FPS цикл на робочому столі, щоб не виснажувати акумулятор.
- **Розширений інтерактивний режим (Studio Sandbox & Mini-Game Studio)**: забезпечує повноцінну 60 FPS анімацію, частинки та 2D-мініігри безпосередньо в застосунку зі збереженням рекордів і стану, які миттєво відображаються на домашньому віджеті.
