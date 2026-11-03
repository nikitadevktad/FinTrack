# Шаг 02. Базовый Android-проект

На этом этапе создано минимальное приложение FinTrack с одним модулем `app`.

## Состав проекта

- `settings.gradle.kts` и `build.gradle.kts` подключают модуль и Gradle-плагины.
- `gradle/libs.versions.toml` хранит версии плагинов и зависимостей.
- `gradle/wrapper` вместе с `gradlew` и `gradlew.bat` позволяет собирать проект без отдельной установки Gradle.
- `app/src/main/java/.../MainActivity.kt` задаёт корневой Compose-контент.
- `app/src/main/java/.../presentation/FinTrackScreen.kt` содержит единственный экран приложения.
- `app/src/main/java/.../presentation/theme` содержит светлую и тёмную темы Material 3.

Экран показывает название приложения и состояние без операций. Операции, категории, навигация, база данных, ViewModel, DI и графики пока не реализованы.

## Запуск

1. Установите JDK 17 и Android SDK Platform 37.
2. Откройте корень проекта в Android Studio или подключите устройство либо эмулятор.
3. Для проверки сборки выполните:

   ```powershell
   .\gradlew.bat assembleDebug
   .\gradlew.bat lintDebug
   ```

Debug APK создаётся в `app/build/outputs/apk/debug/` и не сохраняется в Git.
