Проект: Shoutdowner — добавить "включение ПК" через Magic Packet (Wake-on-LAN)

Цель
- Добавить в приложение возможность отправлять Magic Packet (Wake-on-LAN), чтобы включать заранее настроенный компьютер в локальной сети.

Коротко (для ленивых)
- Требуется: MAC-адрес целевого ПК + желательно Broadcast IP (например 192.168.1.255) или знание, что телефон и ПК в одной подсети.
- Мы добавим: WOL-менеджер (отправка UDP magic packet), настройки (MAC, broadcast, порт), кнопку на главном экране, и права в AndroidManifest.

Предусловия (важно)
1. На целевом компьютере включён Wake-on-LAN в BIOS/UEFI.
2. Сетевая карта ОС настроена на прием Magic Packet (в Windows — в настройках адаптера/Power Management).
3. Телефон должен быть в той же локальной сети (подсети) или у нас есть маршрутизатор/сервер, который умеет пересылать magic-пакеты внутрь сети.
4. Если вы планируете шлёпать пакет через интернет — требуется проброс портов/динамический DNS или использовать SSH-прокси (см. запасной вариант).

Что нужно от тебя (чётко)
- MAC-адрес целевого ПК (формат XX:XX:XX:XX:XX:XX). Обязательно.
- Если знаешь — Broadcast IP подсети (напр., 192.168.1.255) — это ускорит тестирование. Если не знаешь, я попытаюсь вычислить автоматически из Wi-Fi интерфейса.
- Уточнить: тестировать будем на том же Wi‑Fi, где телефон и ПК? (да/нет)
- Если телефон в другой сети: SSH-доступ к локальной машине в сети (ip, порт, логин, пароль или ключ) — тогда можно переслать команду через SSH с удалённого хоста.
- Разрешаешь ли правки манифеста (adds INTERNET и возможно ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE)?

Детальный план реализации (шаги, приоритеты)
Шаг 0 — обзор (1 час)
- Пройтись по SettingsRepository.kt, SettingsViewModel/Screen и SSHManager чтобы понять текущую архитектуру сохранения настроек и как выполняются сетевые операции.
- Решение: реализовать WOL как отдельный небольшой utility/class (WOLManager.kt) с простым API: sendWake(mac: String, broadcastIp: String?, port: Int = 9)

Шаг 1 — добавление полей в настройки (ниже — файлы)
- Файлы для правки:
  - app/src/main/java/com/example/shoutdowner/data/SettingsRepository.kt — добавить хранилище для mac, broadcast, port
  - app/src/main/java/com/example/shoutdowner/viewmodel/SettingsViewModel.kt (+Factory) — обернуть новые поля в UI
  - app/src/main/java/com/example/shoutdowner/ui/SettingsScreen.kt — добавить поля ввода: MAC, Broadcast IP (опционально), Port (default 9)
- Комментарий: использовать уже существующий механизм SharedPreferences / DataStore (как в проекте) — просто расширить.

Шаг 2 — реализовать WOL-менеджер
- Новый файл:
  - app/src/main/java/com/example/shoutdowner/wol/WOLManager.kt
- Что делает:
  - Формирует magic packet: 6 байт 0xFF + 16 повторов MAC (6 байт).
  - Открывает DatagramSocket (UDP), устанавливает broadcast=true.
  - Отправляет DatagramPacket на broadcast-адрес (по умолчанию 255.255.255.255 или вычисленный из Wi-Fi) и порт (обычно 7 или 9; будем по умолчанию ставить 9).
  - Работает в фоне (kotlin coroutines / Dispatchers.IO) и возвращает успех/ошибку.
- Дополнительно: реализовать функцию автопоиска broadcast IP если не задан (по информации о WiFiManager/NetworkInterfaces).

Шаг 3 — привязка к UI / ViewModel
- Файлы:
  - app/src/main/java/com/example/shoutdowner/viewmodel/MainViewModel.kt — добавить метод wakeDevice()
  - app/src/main/java/com/example/shoutdowner/ui/MainScreen.kt — добавить кнопку "Wake" рядом с Shutdown (или отдельную)
- Поведение:
  - При нажатии: валидировать MAC (regex), взять broadcast/port из настроек, вызвать WOLManager.sendWake(...)
  - Показать результат (Toast/Snackbar) — успех или детализированную ошибку.

Шаг 4 — права и манифест
- Изменить AndroidManifest.xml:
  - Добавить: <uses-permission android:name="android.permission.INTERNET" />
  - Рекомендовано (для получения сети/инфы): ACCESS_NETWORK_STATE и ACCESS_WIFI_STATE (если реализуем автоматическое вычисление broadcast)
  - Если используем multicast (возможно не требуется): CHANGE_WIFI_MULTICAST_STATE и захват MulticastLock при необходимости.
- Примечание: не нуждаемся в runtime runtime-permissions для этих (не dangerous) — просто добавить в манифест.

Шаг 5 — тестирование
- Локальный тест:
  - Подключи телефон к той же Wi-Fi сети.
  - Укажи MAC в настройках приложения.
  - Нажми Wake — проверь, включается ли ПК.
- Диагностика, если не работает:
  - Уточнить, включён ли WOL в BIOS/OS.
  - Проверить, отвечает ли роутер на broadcast (некоторые роутеры блокируют 255.255.255.255; тогда нужен directed broadcast 192.168.1.255).
  - Попробовать отправить пакет с ПК/другого устройства (wakeonlan tool) для подтверждения.
- Доп. проверка: логирование отправки пакета и ошибок (IOException, SecurityException и пр.).

Шаг 6 — запасные варианты / fallback
- Если телефон не в той же сети:
  - Использовать SSHManager уже в проекте: если у тебя есть доступ к машине внутри LAN (Raspberry Pi, домашний сервер), можем по SSH выполнить утилиту wakeonlan или отправить UDP пакет с той машины.
  - Потребуется SSH-адрес, учётные данные и команда для отправки пакета (пример: sudo wakeonlan XX:XX:XX:...).
- Если роутер блокирует broadcast:
  - Использовать directed broadcast (подсеть.255) или SSH-прокси, либо настроить портфорвардинг на роутере.

Файлы, которые предположительно будут изменены/созданы
- Изменить:
  - app/src/main/java/com/example/shoutdowner/data/SettingsRepository.kt (+ добавить ключи/получатели)
  - app/src/main/java/com/example/shoutdowner/viewmodel/SettingsViewModel.kt
  - app/src/main/java/com/example/shoutdowner/viewmodel/MainViewModel.kt
  - app/src/main/java/com/example/shoutdowner/ui/SettingsScreen.kt
  - app/src/main/java/com/example/shoutdowner/ui/MainScreen.kt
  - app/src/main/AndroidManifest.xml (permissions)
- Добавить:
  - app/src/main/java/com/example/shoutdowner/wol/WOLManager.kt

Пример алгоритма (Kotlin-ish, упрощённо)
- Формирование magic packet:
  - bytes = ByteArray(6 + 16*6)
  - fill first 6 bytes with 0xFF
  - parse MAC -> 6 bytes
  - repeat MAC 16 times appended
- Отправка:
  - DatagramSocket().use { socket ->
      socket.broadcast = true
      val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(broadcast), port)
      socket.send(packet)
    }

Расписание / оценки по времени
- Анализ + добавление настроек: 0.5-1 день
- Реализация WOLManager + ViewModel: 0.5 дня
- UI (Settings + кнопка): 0.5 дня
- Тесты и исправления: 0.5 дня
Итого ~1.5-2.5 рабочих дня (в зависимости от нюансов сети и тестов).

Что от тебя прямо сейчас (коротко)
1. MAC-адрес целевого ПК.
2. Подтверждение: телефон и ПК в одной Wi‑Fi сети? (да/нет)
3. Разрешение на изменение AndroidManifest (добавить INTERNET и т.д.) — ответ «да»/«нет».
4. Если нет в одной сети — SSH-доступ к машине в той подсети (ip, порт, логин, пароль/ключ) для варианта с прокси.
5. Желательный порт (по умолчанию 9) или оставить дефолт.

Если хочешь — могу сразу приступить к реализации (создам WOLManager + добавлю поля в Settings и UI), но для этого мне нужно подтверждение что можно изменять манифест и MAC/нижеописанные данные (или скажи, что оставляем поля пустыми для тебя, чтобы потом заполнить вручную). Продолжим?
