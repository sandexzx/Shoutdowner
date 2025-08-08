# План разработки: Android-приложение для выключения Ubuntu по локальной сети

Коротко: делаем стильное Android-приложение (Jetpack Compose), которое по нажатию кнопки подключается к твоему Ubuntu по SSH и выполняет команду выключения. Всё безопасно — поддержка ключей и зашифрованного хранилища, экран настроек для host/user/auth, подтверждение перед выключением. Ниже подробный план задач, архитектура, зависимости и инструкции по Ubuntu.

Тон — неформальный, но по делу. Если хочешь, могу после этого сделать саму реализацию по шагам.

1. Цели и требования
- Основная фича: одна большая кнопка "Выключить" — при нажатии:
  - Показывает подтверждение (dialog).
  - Если подтверждён — выполняет SSH-команду shutdown на удалённой машине.
  - Показ статуса (выполняется / успешно / ошибка).
- Экран настроек для хранения:
  - host (IP локальной сети)
  - port (обычно 22)
  - username
  - метод аутентификации: password OR private key (рекомендуется key)
  - опция "sudo без пароля" инструкцию для Ubuntu (если нужен sudo для shutdown)
- Безопасность:
  - Данные аутентификации хранятся в зашифрованном виде (EncryptedSharedPreferences / Android Keystore).
  - По умолчанию подсказки по настройке SSH на Ubuntu.
- Требования Ubuntu:
  - Установлен и запущен OpenSSH server (openssh-server).
  - Пользователь имеет право выполнить shutdown (лучше настроить NOPASSWD для команды shutdown если не хотим вводить пароль).
- Поддержка: корневой доступ на ПК не нужен, достаточно sudo без пароля или запуск shutdown от root через конфиг sudoers.

2. Технологии и зависимости
- Язык: Kotlin
- UI: Jetpack Compose (проект уже выглядит как Compose).
- Сеть и SSH:
  - Вариант A (рекомендую): JSch (com.jcraft:jsch:0.1.55) — лёгкая SSH-библиотека для выполнения команд.
  - Вариант B: sshj (если хочется современней) — потребуется чуть больше конфигов.
- Хранение секретов: androidx.security:security-crypto (EncryptedSharedPreferences) / Android Keystore.
- Coroutine scope: kotlinx-coroutines-android для фоновой работы.
- Material3 для крутой UI: androidx.compose.material3 (если используем M3).
- Пример зависимостей (app/build.gradle.kts — добавить):
  - implementation("com.jcraft:jsch:0.1.55")
  - implementation("androidx.security:security-crypto:1.1.0")
  - implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
  - implementation("androidx.compose.material3:material3:<latest>")

3. Архитектура приложения (вкратце)
- UI layer (Compose):
  - MainScreen.kt — большая кнопка, индикатор статуса.
  - SettingsScreen.kt — поля для host/port/user/auth, кнопка "Сохранить/Тестовый вход".
  - Dialogs.kt — подтверждение, ошибки.
- Domain / Network:
  - SSHManager.kt — класс, инкапсулирует подключение SSH и выполнение команд (использует JSch).
  - CommandExecutor — обёртка вокруг SSHManager, возвращает результаты (sealed classes Success/Error/Progress).
- Data:
  - SettingsRepository — использует EncryptedSharedPreferences для хранения настроек и приватных ключей.
- ViewModels:
  - MainViewModel — логика кнопки выключения.
  - SettingsViewModel — логика сохранения/тестирования соединения.
- Utils:
  - SshKeyUtils — помощь при загрузке/конвертации приватного ключа (если потребуется).
  - Error mapping.

4. Команды, которые будем выполнять на Ubuntu
- Простой shutdown (если пользователь имеет право):
  - sudo shutdown -h now
- Если у пользователя есть root-доступ (не рекомендую хранить root пароль в приложении).
- Рекомендация по sudoers: чтобы избежать ввода пароля через sudo, на Ubuntu на машине-получателе:
  - Создать файл (через root): /etc/sudoers.d/shoutdowner
  - Добавить строку:
    username ALL=(ALL) NOPASSWD: /sbin/shutdown
  - Это позволит пользователю выполнить `sudo /sbin/shutdown -h now` без запроса пароля.
- Альтернатива (менее безопасно): запуск `echo password | sudo -S shutdown -h now` — НЕ рекомендую (пароль в приложении).

5. UI / UX идеи (классный дизайн)
- Главный экран:
  - Большая красная/градиентная круглая кнопка с иконкой молнии/питания.
  - Под кнопкой — подпись "Выключить удалённый ПК" и IP адрес (если сохранён).
  - После нажатия: модальный bottom-sheet / диалог "Точно выключить?" с анимацией.
  - Во время выполнения — Lottie или progress indicator с текстом "Отправляем команду...".
  - Успех/ошибка — Snackbar с текстом + toast/small confetti для успеха (опционально).
- Экран настроек:
  - Поля ввода с подсказками; переключатель аутентификации Password / Key; кнопка "Тест соединения".
  - Кнопка "Пример настройки Ubuntu" — открывает инструкцию.
- Тёмная тема, аккуратные отступы, Material3.

6. Детальный пошаговый план задач (milestones)
Milestone 0 — Подготовка (0.5ч)
- Проверить текущий проект (он уже Compose).
- Добавить разрешение INTERNET в AndroidManifest.xml (если ещё нет).
- Добавить зависимости в app/build.gradle.kts.

Milestone 1 — Базовый UI (1.5ч)
- Создать MainScreen composable: большая кнопка, status.
- Заготовки ViewModel и навигации в проекте (если нет).

Milestone 2 — Экран настроек и хранение (2ч)
- Создать SettingsScreen, SettingsViewModel.
- Реализовать SettingsRepository с EncryptedSharedPreferences.
- UI для ввода приватного ключа (мелкая загрузка файла) или поля пароля.

Milestone 3 — SSHManager (2ч)
- Реализовать SSHManager на JSch:
  - Подключение по host/port/username.
  - Поддержка password или private key (в виде PEM).
  - Выполнение команды с таймаутом и чтением stderr/stdout.
- Обрабатывать ошибки (Auth fail / conn timeout / command error).

Milestone 4 — Интеграция кнопки (1ч)
- MainViewModel вызывает CommandExecutor, показывает прогресс и результат.
- При успехе — показать Snackbar "Выключение отправлено".
- Логирование (для отладки).

Milestone 5 — Тесты и инструкции Ubuntu (0.5–1ч)
- Добавить инструкцию в приложении (Settings -> Инструкции).
- Тестирование в LAN с реальной Ubuntu.

Milestone 6 — Полировка дизайна и публикация (1–2ч)
- Анимации, иконки, тёмная тема.
- Финальное тестирование, сборка релиза.

7. Безопасность и советы
- Никогда не хранить пароли в открытом виде — использовать EncryptedSharedPreferences.
- Предлагать вариант "использовать приватный ключ" — лучший путь.
- В инструкции для Ubuntu рекомендовать настроить NOPASSWD только на конкретную команду shutdown, не давать полного NOPASSWD на все команды.
- Если приватный ключ используется — шифровать его и не экспортировать наружу.
- Не реализовывать передачу пароля через командную подачу в sudo (echo | sudo -S) — fragile и небезопасно.

8. Файлы, которые будут добавлены/изменены
- Изменить:
  - app/src/main/AndroidManifest.xml — добавить permission INTERNET
  - app/build.gradle.kts — добавить зависимости (JSch, security-crypto, coroutines, material3)
- Создать:
  - app/src/main/java/.../ui/MainScreen.kt
  - app/src/main/java/.../ui/SettingsScreen.kt
  - app/src/main/java/.../ssh/SSHManager.kt
  - app/src/main/java/.../data/SettingsRepository.kt
  - app/src/main/java/.../viewmodel/MainViewModel.kt
  - app/src/main/java/.../viewmodel/SettingsViewModel.kt
  - res/layouts / composables при необходимости
  - plan.md (этот файл)

9. Пример сниппетов (gradle + manifest)
- AndroidManifest.xml:
  - Добавить внутри manifest:
    <uses-permission android:name="android.permission.INTERNET" />
- app/build.gradle.kts (пример):
  dependencies {
    implementation("com.jcraft:jsch:0.1.55") // SSH
    implementation("androidx.security:security-crypto:1.1.0") // EncryptedSharedPreferences
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    implementation("androidx.compose.material3:material3:1.1.0")
  }

10. Тестирование / чек-лист QA
- Тест 1: Успешное подключение с приватным ключом, команда выполняется, Ubuntu завершает работу.
- Тест 2: Неверный пароль/ключ => отображается корректная ошибка.
- Тест 3: Неправильный хост/порт => таймаут, понятный feedback.
- Тест 4: Попытка выключения без sudo NOPASSWD => ожидаемо спросит пароль — приложение корректно отработает ошибку.
- Тест 5: Смена сетей / reconnect — устойчивость.

11. Доп. фичи (опционально)
- История команд (когда отправлено, статус).
- Поддержка нескольких хостов (список ПК).
- Использование push-уведомлений / REST API прокси — если SSH запрещён, можно установить на Ubuntu маленький HTTP-сервер с auth и endpoint /shutdown (но это уже отдельная штука и менее безопасная).
- TLS + REST: если захочешь — можно поставить небольшой HTTPS API на ПК и вызывать его с токеном.

12. Порядок работ / оценка времени
- Оценка: 7–10 часов для MVP (UI + SSH + settings + тесты).
- Можно разбить на 2–3 рабочих сессии: UI+settings (1), ssh manager+integration (2), полировка и тесты (3).

13. Требования со стороны Ubuntu (шаги для подготовки)
- Установить OpenSSH:
  sudo apt update
  sudo apt install openssh-server
- Проверить статус:
  sudo systemctl status ssh
- Проверить доступность из LAN: с другого ПК выполнить `ssh username@ip`
- Настройка sudoers для passwordless shutdown:
  sudo visudo -f /etc/sudoers.d/shoutdowner
  Вставить:
    username ALL=(ALL) NOPASSWD: /sbin/shutdown
- Проверить команду:
  ssh username@ip "sudo /sbin/shutdown -h now"  # должно сработать без запроса пароля

14. Следующие шаги (что я сделаю по твоему подтверждению)
- Если подтверждаешь план — начну реализацию MVP:
  1) Добавлю INTERNET permission + зависимости.
  2) Реализую Settings + EncryptedSharedPreferences.
  3) Реализую SSHManager на JSch.
  4) Сделаю MainScreen с кнопкой и интеграцией.
  5) Протестирую и дам инструкции, как настроить Ubuntu.

Если всё ок — кидай "Делай" и я приступаю к реализации. Если хочешь правки в плане — говори, что подправить.
