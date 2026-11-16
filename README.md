<div align="center">

# MirageUShulkers — Шалкер сокровищ

**Механика «Шалкер сокровищ» на спавне с прямой привязкой к [MirageDungeons](https://github.com/sawervatoryy/MirageDungeons) и 5 уровнями отмычек**

![Paper](https://img.shields.io/badge/Paper-1.21.4-00ACFF?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-FF7B00?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.0.0-00B0F2?style=for-the-badge)
![Studio](https://img.shields.io/badge/MirageStudio-sawervatoryy-8A2BE2?style=for-the-badge)

*Разработчик: **sawervatoryy** · Студия: **MirageStudio***

</div>

---

## Описание

**MirageUShulkers** превращает любой шалкер в мире в *Шалкер сокровищ*. Он закрыт, неуязвим и открывается только
**отмычкой из MirageDungeons** (1–5 LVL). Чем выше уровень отмычки, тем лучше лут и выше шанс на событие
**«Красный шалкер»** — весь лут удваивается (×2), а на весь сервер уходит эпичное объявление.

| Возможность | Описание |
|---|---|
| 5 уровней отмычек | У каждого уровня свой пул лута, количество слотов и шанс Красного шалкера |
| Закрытый шалкер | Нельзя сломать, взорвать, сдвинуть поршнем или набить воронкой |
| Режим просмотра | Другие игроки видят лут в реальном времени, но ничего не могут взять |
| Красный шалкер ×2 | Смена блока на `RED_SHULKER_BOX`, звук дракона/тотема, глобальное объявление, удвоение лута |
| GUI-редактор лута | `/mushulker editloot <1-5>` — перетаскиваете предмет, шанс настраиваете кликами |
| Нативная сериализация | Предметы хранятся в Base64 со всеми Data Components 1.21.4 (имена, чары, модели, PDC других плагинов) |
| Защита от дюпов | Закрытие инвентаря, выход игрока, `onDisable`, `/reload`, любые клики в режиме просмотра |
| Дизайн | Adventure/MiniMessage, градиенты, `&#RRGGBB`, шрифт малых заглавных |

---

## Требования

- **Paper 1.21.4** (или совместимый форк)
- **Java 21**
- *(рекомендуется)* **MirageDungeons** — плагин данжей, из которого берутся отмычки. Указан как `softdepend`.

---

## Установка и настройка (пошагово)

1. Соберите плагин (`mvn clean package`) или возьмите готовый `MirageUShulkers-1.0.0.jar`.
2. Положите jar в папку `plugins/` и запустите сервер. Создадутся `config.yml` и `loot.yml`.
   Если на сервере есть MirageDungeons, в консоли появится сообщение
   `MirageStudio: MirageUShulkers <-> MirageDungeons — модули успешно связаны`.
3. **Привяжите все 5 уровней отмычек.** Для каждого уровня возьмите отмычку из MirageDungeons в правую руку и выполните:
   ```
   /mushulker bindkey 1
   /mushulker bindkey 2
   /mushulker bindkey 3
   /mushulker bindkey 4
   /mushulker bindkey 5
   ```
   Плагин считает материал, название, модель, `CustomModelData`, все Data Components и PDC-теги и сохранит их
   в `lockpicks.level-N.item`.
4. **Наполните лут** каждого уровня:
   ```
   /mushulker editloot 1   ...   /mushulker editloot 5
   ```
   Перетащите предметы из своего инвентаря в окно редактора (или Shift+клик по предмету в инвентаре).
5. **Установите шалкер.** Поставьте любой шалкер, посмотрите на него и выполните `/mushulker set`.
   Блок станет фиолетовым (`PURPLE_SHULKER_BOX`) с сохранением направления.
6. Готово! Игрок с отмычкой нажимает **ПКМ** по шалкеру — и получает лут своего уровня.

> Если у отмычек MirageDungeons есть уникальные значения в PDC (например, UUID), переключите
> `lockpicks.match-mode` на `KEYS`: тогда сравниваются только наличие ключей, а не значения.

---

## Как это работает

```
ПКМ по шалкеру
 ├─ нет отмычки ─────────► сообщение + звук отказа + лёгкое отталкивание
 ├─ шалкер занят ────────► режим просмотра (виртуальный инвентарь, только смотреть)
 └─ отмычка 1–5 LVL
     ├─ забираем 1 отмычку из руки
     ├─ генерируем лут из пула level-N (min-items … max-items)
     ├─ бросок на Красный шалкер (red-chance уровня)
     │    └─ успех: блок → RED_SHULKER_BOX, объявление на весь сервер, лут ×2
     ├─ шалкер закрепляется за игроком на 60 сек, крышка открывается (ShulkerBox#open())
     └─ закрытие окна / таймаут → остатки выпадают или пропадают, блок снова фиолетовый
```

### Красный шалкер (×2)

- Шанс зависит от уровня отмычки (по умолчанию: **3 / 6 / 10 / 15 / 25 %**).
- Стакающиеся предметы удваиваются (с разбиением по стакам), нестакающиеся дублируются в свободные слоты.
- Исключения из удвоения (по умолчанию броня, оружие и материалы из `red-shulker.exclude.materials`) остаются в единственном экземпляре.
- Если лута получилось больше, чем слотов в окне, лишнее не добавляется — увеличьте `settings.inventory-size`.

### Режим просмотра

Другие игроки, нажавшие ПКМ по открытому шалкеру, видят **тот же самый инвентарь** (изменения синхронизируются сразу),
но любые клики и перетаскивания для них отменяются. Владелец тоже может только **забирать** предметы:
класть, подменять, собирать на курсор и клонировать в креативе нельзя.

---

## Команды и права

| Команда | Право | Описание |
|---|---|---|
| `/mushulker set` | `mirageushulkers.admin` | Привязать шалкер, на который вы смотрите |
| `/mushulker remove` | `mirageushulkers.admin` | Отвязать шалкер (активная сессия завершается) |
| `/mushulker bindkey <1-5>` | `mirageushulkers.admin` | Привязать отмычку из правой руки к уровню 1–5 |
| `/mushulker editloot <1-5>` | `mirageushulkers.admin` | GUI-редактор лута уровня |
| `/mushulker list` | `mirageushulkers.admin` | Список привязанных шалкеров |
| `/mushulker reload` | `mirageushulkers.admin` | Перезагрузить `config.yml` и `loot.yml` |

Алиасы: `/mushulkers`, `/ushulker`. Tab-комплитер работает для подкоманд и уровней `1`–`5`.

| Право | По умолчанию | Описание |
|---|---|---|
| `mirageushulkers.admin` | `op` | Все команды управления |
| `mirageushulkers.use` | `true` | Открытие и просмотр Шалкеров сокровищ |

### Управление в редакторе лута

| Действие | Результат |
|---|---|
| Перетащить предмет в окно / Shift+клик по предмету в своём инвентаре | Добавить в лут |
| `ЛКМ` / `ПКМ` по записи | Шанс **+5%** / **−5%** |
| `Shift+ЛКМ` / `Shift+ПКМ` | Шанс **+1%** / **−1%** |
| Клавиши `1` / `2` | Минимальное количество −1 / +1 |
| Клавиши `3` / `4` | Максимальное количество −1 / +1 |
| `Q` | Удалить запись |
| Кнопка «Сохранить» | Записать `loot.yml` немедленно (иначе — при закрытии окна) |

---

## Структура конфигов

### `config.yml` (главное)

```yaml
shulkers: []                  # координаты шалкеров (заполняется командами)

settings:
  session-seconds: 60         # на сколько секунд шалкер закрепляется за игроком
  inventory-size: 27          # 9..54
  finish-on-owner-close: true # закрыл окно = шалкер завершён
  leftover-action: DROP       # DROP | CLEAR — судьба несобранного лута
  normal-material: PURPLE_SHULKER_BOX
  red-material: RED_SHULKER_BOX

lockpicks:
  match-mode: LOOSE           # LOOSE | KEYS | STRICT
  pdc-detection:              # запасной способ — поиск PDC-ключа miragedungeons
    enabled: true
    namespace: miragedungeons
    key-hints: [level, lvl, tier, lockpick, key]
  level-3:
    name: "&bОтмычка III ур."
    red-chance: 10.0          # шанс Красного шалкера, %
    min-items: 4              # минимум стаков лута
    max-items: 6              # максимум стаков лута
    item: "<Base64>"          # заполняется /mushulker bindkey 3

red-shulker:
  multiplier: 2
  exclude:
    armor: true
    weapons: true
    tools: false
    materials: [ELYTRA, TOTEM_OF_UNDYING]

gui:
  title: "&#00ACFF[ шᴀлᴋᴇᴘ ᴄоᴋᴘовищ | {level} уᴘ. ]"
  title-red: "&#FF3B3B[ ᴋᴘᴀᴄʜый шᴀлᴋᴇᴘ | {level} уᴘ. ]"

effects:                      # звуки "ИМЯ;громкость;высота", частицы "ИМЯ;кол-во;разброс;скорость"
  open:
    sounds: ["BLOCK_SHULKER_BOX_OPEN;1.0;1.0"]
    particles: ["END_ROD;25;0.4;0.05"]

messages: { ... }             # все сообщения: MiniMessage, &-коды и &#RRGGBB
```

Палитра по умолчанию: бирюзовый градиент `&#00ACFF → &#00B0F2` для обычного режима и
огненный `&#FF3B3B → &#FF7B00` для события Красного шалкера.

### `loot.yml`

```yaml
level-3:
  '1':
    material: DIAMOND         # справочное поле (для чтения человеком)
    chance: 30.0              # шанс выпадения, %
    min-amount: 2
    max-amount: 6
    item: "<Base64>"          # нативная сериализация ItemStack (все Data Components 1.21.4)
  '2': {material: GOLDEN_APPLE, chance: 25.0, min-amount: 2, max-amount: 5}   # запись без item — по material
```

Если у записи есть поле `item`, оно имеет приоритет над `material`. Лут каждого уровня полностью независим.

**Как генерируется лут:** количество стаков выбирается случайно в диапазоне `min-items … max-items`,
каждая запись проходит бросок по своему `chance`; если выпало меньше минимума, добор делается взвешенным выбором.

---

## Защита от дюпов

- Содержимое сессии забирается и инвентарь очищается **до** выпадения остатков; повторное завершение невозможно.
- Закрытие окна владельцем, выход игрока, смерть, таймаут, `/mushulker remove`, `/mushulker reload`,
  выключение сервера и `onDisable` — все пути ведут через один безопасный метод завершения.
- Для зрителей отменяются **все** клики и перетаскивания; владелец может только забирать.
- Воронки не могут класть предметы в шалкер и вынимать их; блок не ломается, не взрывается и не двигается поршнем.

---

## Сборка

```bash
git clone <репозиторий>
cd MirageUShulkers
mvn clean package
```

Готовый файл: `target/MirageUShulkers-1.0.0.jar`. Для сборки нужны JDK 21 и Maven 3.8+.
Репозиторий PaperMC и зависимость `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT` подключены в `pom.xml`.

### Структура проекта

```
MirageUShulkers
├── pom.xml
├── README.md
└── src/main
    ├── resources   (plugin.yml, config.yml, loot.yml)
    └── java/ru/miragestudio/sawervatoryy
        ├── MirageUShulkers.java        — главный класс
        ├── command/MUShulkerCommand    — /mushulker + Tab-комплитер
        ├── gui/LootEditor              — GUI-редактор лута
        ├── listener/                   — ShulkerListener, ProtectionListener, EditorListener
        ├── lockpick/                   — LockpickLevel, LockpickManager
        ├── loot/                       — LootEntry, LootManager
        ├── shulker/                    — BlockKey, ShulkerSession, ShulkerManager
        └── util/                       — Text, Messages, Effects, ItemCodec
```

---

## FAQ

**Шалкер на спавне не открывается, хотя отмычка есть.**
Проверьте, что уровень привязан (`/mushulker bindkey <1-5>`). Если у отмычек уникальные PDC-значения —
поставьте `lockpicks.match-mode: KEYS`. Плагин обрабатывает клик даже если защита спавна отменила событие.

**Как сменить внешний вид сообщений?**
Всё лежит в `config.yml` → `messages`. Поддерживаются MiniMessage-теги (`<gradient:#00ACFF:#00B0F2>`), `&`-коды и `&#RRGGBB`.

**Крышка не закрылась после сбоя сервера.**
При старте и загрузке чанка плагин возвращает все привязанные шалкеры к обычному цвету.

---

<div align="center">

**MirageUShulkers** · © MirageStudio · разработчик **sawervatoryy**

</div>
