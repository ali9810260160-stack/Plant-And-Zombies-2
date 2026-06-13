# 📦 سیستم Data-Driven — راهنمای توسعه

این دایرکتوری شامل تمام داده‌های بازی به فرمت JSON و Repository‌های متناظر
در Java است. برای اضافه کردن هر موجودیت جدید (گیاه، زامبی، کوئست، مرحله)
**فقط به ویرایش فایل JSON نیاز دارید — بدون تغییر در کد Java.**

---

## ساختار پوشه‌ها

```
data/
├── plants/
│   └── plants.json          ← تعریف تمام گیاهان (69 گیاه)
├── zombies/
│   └── zombies.json         ← تعریف زامبی‌ها (33) و زره‌ها (6)
├── quests/
│   └── quests.json          ← تعریف کوئست‌ها (20 کوئست)
└── levels/
    ├── levels.json          ← تعریف مراحل (16 مرحله)
    └── progress_*.json      ← پیشرفت هر کاربر (auto-generated)

src/main/java/com/pvz2/repository/
├── PlantDataRepository.java
├── ZombieDataRepository.java
├── QuestRepository.java
├── LevelRepository.java
└── UserRepository.java
```

---

## 🌱 اضافه کردن گیاه جدید

فایل `data/plants/plants.json` را باز کنید و یک شیء به آرایه `plants` اضافه کنید:

```json
{
  "id": 70,
  "name": "Laser Cactus",
  "internalName": "laser_cactus",
  "family": "Shooter",
  "tags": ["Fire", "Strike-through"],
  "sunCost": 200,
  "baseHp": 400,
  "damage": "60",
  "baseAbility": "شلیک لیزر آتشین که از چند زامبی رد می‌شود.",
  "plantFoodEffect": "شلیک لیزر عظیم که تمام زامبی‌های سطر را می‌سوزاند.",
  "upgrades": {
    "level2": "Dmg +20",
    "level3": "HP +150",
    "level4": "Cost -25"
  },
  "actionIntervalSeconds": 1.5,
  "rechargeSeconds": 5.0,
  "isBonus": false,
  "currentLevel": 1,
  "seedPacketsOwned": 0
}
```

**تمام!** بار بعدی که بازی اجرا شود، این گیاه در کلکسیون ظاهر می‌شود.

> ⚠️ **نکته:** فیلد `internalName` باید lowercase و با underscore باشد.
> فیلد `id` باید یکتا و بزرگ‌تر از آخرین ID موجود باشد.

---

## 🧟 اضافه کردن زامبی جدید

فایل `data/zombies/zombies.json` را باز کنید و به آرایه `zombies` اضافه کنید:

```json
{
  "alias": "ZombieDarkArcherDefault",
  "chapter": "DARK_AGES",
  "hp": 350,
  "eatDps": 100,
  "speed": 0.185,
  "waveCost": 300,
  "canSpawnPlantFood": true,
  "armors": [],
  "specialClass": "archer",
  "specialProps": {
    "arrowDamage": 50,
    "arrowRangeColumns": 4
  }
}
```

### فیلدهای اجباری زامبی

| فیلد | نوع | توضیح |
|------|-----|-------|
| `alias` | string | نام داخلی یکتا |
| `chapter` | string | فصل: `ALL`, `ANCIENT_EGYPT`, `FROSTBITE_CAVES`, `BIG_WAVE_BEACH`, `DARK_AGES` |
| `hp` | int | سلامتی پایه |
| `eatDps` | int | آسیب خوردن گیاه در ثانیه |
| `speed` | double | سرعت (خانه/ثانیه) |
| `waveCost` | int | هزینه موج |
| `canSpawnPlantFood` | bool | آیا می‌تواند plant food بیندازد |
| `armors` | array | لیست alias زره‌ها (می‌تواند خالی باشد) |
| `specialClass` | string | نوع رفتار خاص (توضیح پایین) |
| `specialProps` | object | پارامترهای اضافه (اختیاری) |

### مقادیر `specialClass`

| مقدار | توضیح |
|-------|-------|
| `standard` | زامبی معمولی |
| `flag` | زامبی پرچم (waveCost کم) |
| `imp` | ایمپ (سریع، کوچک) |
| `dragon_imp` | ایمپ مقاوم به آتش |
| `gargantuar` | غول‌پیکر (یک‌ضربه) |
| `ra` | خورشیددزد مصری |
| `explorer` | مشعل‌دار |
| `tomb_raiser` | قبرساز |
| `dodo_rider` | سوار دودو |
| `hunter` | شکارچی یخ |
| `troglobite` | هل‌دهنده یخ |
| `fisherman` | ماهیگیر |
| `snorkel` | غواص |
| `octopus` | اختاپوس‌پرت‌کن |
| `jester` | ژانگولر |
| `wizard` | جادوگر |
| `king` | پادشاه |
| `all_star` | فوتبالیست |
| `arcade` | زامبی آرکید |
| `turquoise` | خورشیددزد تورکوایز |
| `prospector` | اکتشافگر دینامیت |
| `pianist` | پیانیست |
| `newspaper` | پیرمرد روزنامه‌دار |

### اضافه کردن زره جدید

به آرایه `armors` در همان فایل اضافه کنید:

```json
{
  "alias": "HelmetDefault",
  "type": "Helmet",
  "baseHealth": 1800,
  "magnetShroom": true
}
```

---

## 📋 اضافه کردن کوئست جدید

فایل `data/quests/quests.json` را باز کنید و به آرایه `quests` اضافه کنید:

```json
{
  "id": "no_explosives",
  "name": "مسالمت‌آمیز",
  "category": "DAILY",
  "priority": "HIGH",
  "conditionType": "WIN_WITHOUT_FAMILY",
  "conditionDescription": "یک مرحله را بدون استفاده از گیاهان انفجاری ببر",
  "params": { "family": "Explosive" },
  "rewardType": "GEMS",
  "rewardAmount": 15,
  "rewardFormula": null,
  "isParametric": false,
  "isDaily": true
}
```

### دسته‌بندی کوئست‌ها (`category`)

| مقدار | اولویت | توضیح |
|-------|--------|-------|
| `MAIN` | HIGH | کوئست‌های داستانی |
| `EPIC_CHALLENGE` | CRITICAL | چالش‌های بزرگ |
| `DAILY` | MEDIUM/LOW | کوئست‌های روزانه |

### انواع پاداش (`rewardType`)

| مقدار | توضیح |
|-------|-------|
| `COINS` | سکه |
| `GEMS` | الماس |
| `SEED_PACKETS` | بسته بذر |
| `RANDOM_PLANT` | گیاه تصادفی آنلاک |
| `UNLOCK_PLANT` | گیاه مشخص آنلاک |

### فرمول‌های پاداش (`rewardFormula`)

| مقدار | توضیح |
|-------|-------|
| `null` | از `rewardAmount` مستقیم استفاده می‌شود |
| `param1` | مقدار param1 |
| `param1_div_100` | param1 تقسیم بر 100 |
| `20_minus_param1` | 20 منهای param1 |

> **پارامتریک بودن:** اگر `isParametric: true` باشد، `params` باید حاوی
> آرایه‌ای از مقادیر ممکن باشد. سرویس QuestService هنگام assign،
> یک مقدار تصادفی انتخاب می‌کند.

---

## 🗺️ اضافه کردن مرحله جدید

فایل `data/levels/levels.json` را باز کنید و به آرایه `levels` اضافه کنید:

```json
{
  "id": "egypt_5",
  "chapter": "ANCIENT_EGYPT",
  "levelNumber": 5,
  "levelType": "TIMED_WAR",
  "rows": 5,
  "cols": 9,
  "plantSlots": 8,
  "initialWaveDifficulty": 700,
  "waveCount": 4,
  "allowedZombies": [
    "ZombieMummyDefault",
    "ZombieMummyArmor1Default",
    "ZombieRaDefault",
    "ZombieTutorialFlagDefault"
  ],
  "lockedPlants": [],
  "specialProps": {
    "timedWarZombieTarget": 20,
    "timedWarSeconds": 90
  },
  "isUnlocked": false,
  "isCompleted": false
}
```

### انواع مرحله (`levelType`)

| مقدار | توضیح |
|-------|-------|
| `NORMAL` | مرحله عادی |
| `BOSS` | مرحله رئیس |
| `CONVEYOR_BELT` | نوار کناری |
| `LOCKED_PLANTS` | گیاهان زندانی |
| `SAVE_OUR_SEEDS` | محافظ دانه‌ها |
| `TIMED_WAR` | نبرد زماندار |
| `NIGHT_OPS` | شب عملیات |
| `DEAD_LINE` | ددلاین |
| `LOVE_YOUR_PLANTS` | از دست نده |
| `PLANT_WHAT_YOU_GET` | هر چه رسد بکار |

### `specialProps` بر اساس نوع مرحله

| `levelType` | فیلدهای `specialProps` |
|-------------|----------------------|
| `TIMED_WAR` | `timedWarZombieTarget`, `timedWarSeconds` |
| `DEAD_LINE` | `deadLineColumn` |
| `LOVE_YOUR_PLANTS` | `maxPlantsLost` |
| `PLANT_WHAT_YOU_GET` | `initialSunAmount` |
| `NIGHT_OPS` | `noSkySun: true` |
| `CONVEYOR_BELT` | `conveyorIntervalSeconds` |
| `BIG_WAVE_BEACH` | `waterColumns: [...]`, `waterLevelChangesOnWave` |
| `FROSTBITE_CAVES` | `iceWindEveryWave: true`, `zombiesImmuneToIce: true` |
| `DARK_AGES` | `noSkySun: true`, `tombstonesSpawnEachWave: true` |

---

## 🔄 Caching و Hot-Reload

همه Repository‌ها نتیجه اولین بارگذاری را کش می‌کنند.
در محیط توسعه می‌توانید کش را پاک کنید:

```java
plantRepo.invalidateCache();   // گیاهان
zombieRepo.invalidateCache();  // زامبی‌ها
questRepo.invalidateCache();   // کوئست‌ها
levelRepo.invalidateCache();   // مراحل
```

> **نکته:** داده کاربران (users.json) کش ندارد و هر بار از فایل خوانده می‌شود.
> پیشرفت مراحل (progress_*.json) نیز بدون کش است.

---

## ✅ چک‌لیست قبل از اضافه کردن موجودیت جدید

- [ ] `id` یا `alias` یکتا است
- [ ] همه فیلدهای اجباری پر شده‌اند
- [ ] فایل JSON معتبر است (می‌توانید در [jsonlint.com](https://jsonlint.com) بررسی کنید)
- [ ] `chapter` از مقادیر مجاز است
- [ ] برای زامبی‌های ویژه، `specialClass` متناظر در `CombatService` هندل شده
- [ ] برای کوئست‌های جدید، `conditionType` در `QuestService.onEvent()` هندل شده
