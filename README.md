
# پروژه Plants vs. Zombies (Java) — راهنمای تیم، Git، کیفیت کد و مستندسازی

نام اعضا :
- علی امیر آبادی زاده ، شماره دانشجویی : 404105518
- حمیدرضا فرقدانی ، شماره دانشجویی : 404106163
- امیر حسین علیپور شهر بابکی ، شماره دانشجویی : 404106117

---

این پروژه یک بازسازی (Clone) از بازی **Plants vs. Zombies** با زبان **Java** است که به‌صورت تیمی توسعه داده می‌شود. این README نقش «راهنمای کار تیمی» را دارد تا همه اعضا:
- یک روش یکسان برای **Git و Branching** داشته باشند،
- استانداردهای **کدنویسی/کامنت‌گذاری/Javadoc** را رعایت کنند،
- و قبل از تحویل، کیفیت کد با **Checkstyle + PMD** کنترل شود.

---

## فهرست مطالب
1. [معرفی و فازهای پروژه](#معرفی-و-فازهای-پروژه)
2. [قواعد کلی کار تیمی](#قواعد-کلی-کار-تیمی)
3. [ساختار Git و Branching](#ساختار-git-و-branching)
4. [راهنمای کامل Git: pull/push/merge/MR](#راهنمای-کامل-git-pullpushmergemr)
5. [قوانین Commit و پیام‌های Commit](#قوانین-commit-و-پیامهای-commit)
6. [کیفیت کد: Checkstyle و PMD](#کیفیت-کد-checkstyle-و-pmd)
7. [استاندارد مستندسازی: کامنت و Javadoc](#استاندارد-مستندسازی-کامنت-و-javadoc)
8. [میانبرها و Auto-complete برای Javadoc در IntelliJ](#میانبرها-و-auto-complete-برای-javadoc-در-intellij)
9. [قوانین Merge Conflict و حل تعارض‌ها](#قوانین-merge-conflict-و-حل-تعارضها)
10. [چک‌لیست قبل از Merge به main](#چکلیست-قبل-از-merge-به-main)

---

## معرفی و فازهای پروژه
پروژه در ۵ فاز اصلی جلو می‌رود:

1. **فاز ۱:** طراحی UML و معماری اولیه
2. **فاز ۲:** منوها + منطق بازی (Game Logic)
3. **فاز ۳:** گرافیک/GUI
4. **فاز ۴:** شبکه (Networking / Multiplayer)
5. **فاز ۵:** باگ‌فیکس + بهینه‌سازی + آماده‌سازی نهایی

---

## قواعد کلی کار تیمی
- هیچ‌کس مستقیم روی شاخه `main` کار نمی‌کند.
- همه تغییرات باید در **feature branch** انجام شود.
- ادغام به `main` فقط از طریق **Merge Request** و بعد از review انجام می‌شود.
- قبل از push کردن، پروژه باید **build** شود و **quality checks** پاس شوند.

---

## ساختار Git و Branching
### شاخه‌ها
- `main`: شاخه پایدار/نهایی. فقط از طریق MR آپدیت می‌شود.
- `feature/<name>`: برای هر قابلیت جدید
  - مثال: `feature/zombie-ai`, `feature/sun-system`, `feature/main-menu`
- (اختیاری) `fix/<name>`: برای باگ‌فیکس‌های مشخص
  - مثال: `fix/crash-on-exit`

### قانون نام‌گذاری Branch
- کوتاه، مشخص، بدون فاصله
- از `-` استفاده کنید
- مثال خوب: `feature/plant-peashooter`
- مثال بد: `new branch`, `myFeature1`

---

## راهنمای کامل Git: pull/push/merge/MR

### 1) مفاهیم مهم (خیلی کوتاه و کاربردی)
- **clone**: گرفتن پروژه برای اولین بار از ریموت
- **pull**: گرفتن آخرین تغییرات از ریموت و اعمال روی شاخه فعلی
- **fetch**: گرفتن تغییرات بدون merge کردن
- **push**: فرستادن تغییرات لوکال به ریموت
- **merge**: ادغام یک شاخه در شاخه دیگر
- **rebase**: بازچینش تاریخچه (در تیم‌های تازه‌کار توصیه نمی‌شود مگر با هماهنگی)
- **MR (Merge Request)**: درخواست رسمی برای ادغام کد شما در `main`

---

### 2) اولین بار: clone کردن پروژه
**راه ۱ — HTTPS (با Token به جای پسورد):**
```bash
git clone https://<server>/<group>/<repo>.git
```

**راه ۲ — SSH (با کلید SSH):**
```bash
git clone git@<server>:<group>/<repo>.git
```

---

### 3) شروع یک کار جدید (ساخت feature branch)
همیشه قبل از ساخت شاخه جدید، `main` را آپدیت کنید:

```bash
git checkout main
git pull origin main
```

سپس شاخه جدید بسازید:

```bash
git checkout -b feature/<your-feature-name>
```

و اولین بار push کنید:

```bash
git push -u origin feature/<your-feature-name>
```

> `-u` باعث می‌شود شاخه لوکال به شاخه ریموت «متصل» شود و دفعات بعد فقط `git push` کافی باشد.

---

### 4) چه زمانی pull کنیم؟
قاعده طلایی:
- **قبل از شروع کار روزانه**
- **قبل از merge کردن**
- **قبل از اینکه MR را نهایی کنید**
- **اگر می‌دانید هم‌تیمی‌ها روی بخش‌های مشترک کار کرده‌اند**

پیشنهاد عملی:
- اگر روی feature branch هستید، معمولاً این کار کافی است:
```bash
git pull
```

---

### 5) چه زمانی push کنیم؟
- بعد از اینکه یک بخش کوچک و منطقی از کار کامل شد (نه خیلی بزرگ)
- قبل از اینکه سیستم‌تان خاموش شود
- قبل از درخواست review
- وقتی می‌خواهید بکاپ امن روی ریموت داشته باشید

```bash
git add .
git commit -m "feat: add basic zombie movement"
git push
```

> بهتر است commitها کوچک، قابل فهم، و قابل بازگشت باشند.

---

### 6) ساخت Merge Request (MR) — روش پیشنهادی
#### گام‌ها (روال استاندارد)
1. روی شاخه feature کار کنید و push کنید.
2. در HamGit/GitLab به بخش **Merge Requests** بروید.
3. **New Merge Request** را بزنید:
   - Source branch: `feature/...`
   - Target branch: `main`
4. توضیحات MR را کامل کنید:
   - چه چیزی اضافه شد؟
   - چه چیزی تغییر کرد؟
   - چگونه تست شد؟
   - اگر باگ/issue مرتبط دارد لینک بدهید.
5. یک نفر دیگر از تیم review کند.
6. بعد از تایید، merge انجام شود.

#### نکته مهم
- اگر `main` محافظت شده باشد، merge فقط از طریق MR انجام می‌شود (و این خوب است).

---

### 7) Merge کردن از طریق خط فرمان (اگر لازم شد)
در تیم‌های تازه‌کار، merge از طریق UI (MR) امن‌تر است.  
اما اگر مجبور بودید:

```bash
git checkout main
git pull origin main
git merge feature/<name>
git push origin main
```

> اگر `main` protected باشد، معمولاً push مستقیم به main اجازه داده نمی‌شود.

---

## قوانین Commit و پیام‌های Commit
### قانون کلی
- هر commit باید یک واحد «معنادار» باشد.
- commitهای خیلی بزرگ باعث conflict و سختی review می‌شوند.

### قالب پیشنهادی پیام Commit
- `feat:` قابلیت جدید
- `fix:` رفع باگ
- `refactor:` بازآرایی بدون تغییر رفتار
- `docs:` مستندات
- `test:` تست‌ها
- `chore:` کارهای جانبی (تنظیمات، dependencyها)

مثال‌ها:
- `feat: implement sunflower sun generation`
- `fix: prevent null pointer in game loop`
- `docs: add gameplay rules to README`
- `refactor: extract collision logic into helper`

---

## کیفیت کد: Checkstyle و PMD
در پروژه از دو ابزار استفاده می‌کنیم:

### Checkstyle (سبک و استانداردهای کدنویسی)
مواردی که کنترل می‌شوند (نمونه):
- نام‌گذاری کلاس/متد/فیلد/متغیرها
- طول خط (مثلاً ۱۲۰)
- طول متد (مثلاً ۵۰ خط)
- طول فایل/کلاس (مثلاً ۵۰۰ خط)

**مسیر فایل تنظیمات (طبق توافق تیم):**
- `config/checkstyle/checkstyle.xml`

### PMD (پیدا کردن کدهای بی‌استفاده و مشکلات رایج)
تمرکز فعلی:
- unused local variables
- unused private fields/methods

**مسیر فایل تنظیمات:**
- `config/pmd/ruleset.xml`

### اجرای تست کیفیت با Maven
قبل از MR این دستور را اجرا کنید:

```bash
mvn clean verify
```

---

## استاندارد مستندسازی: کامنت و Javadoc

### 1) فرق کامنت معمولی و Javadoc
- `//` و `/* ... */` برای توضیح داخلی کد (توسعه‌دهندگان)
- `/** ... */` مخصوص **Javadoc** است و می‌تواند به مستندات رسمی تبدیل شود

---

### 2) اصول طلایی کامنت‌گذاری حرفه‌ای
کامنت خوب یعنی:
- **چرایی (Why)** و **قصد (Intent)** را توضیح می‌دهد  
نه اینکه همان کد را دوباره تکرار کند.

#### مثال بد (واضح است، ارزش ندارد)
```java
// increment i
i++;
```

#### مثال خوب (چرایی را می‌گوید)
```java
// We skip index 0 because it is reserved for the base tile.
for (int i = 1; i < tiles.size(); i++) { ... }
```

---

### 3) استاندارد کامنت‌گذاری برای کلاس‌ها
برای هر کلاس (خصوصاً کلاس‌های public و مهم) Javadoc بگذارید:

```java
/**
 * توضیح کوتاه: این کلاس چه کاری انجام می‌دهد و چه مسئولیتی دارد.
 * اگر محدودیت/قانون مهمی دارد ذکر شود.
 *
 * @author ...
 * @since ...
 */
public class Plant { ... }
```

> پیشنهاد تیمی: `@author` را در سطح کلاس‌ها نگه دارید، نه در تمام متدها.

---

### 4) استاندارد کامنت‌گذاری متدها (Method Javadoc)
برای متدهای `public` (و متدهای مهم داخلی) Javadoc بنویسید.

#### قالب پیشنهادی
```java
/**
 * یک جمله خلاصه درباره کاری که متد انجام می‌دهد.
 * (در صورت نیاز) جزئیات رفتار، شرایط خاص، اثرات جانبی، و قوانین بازی.
 *
 * @param zombie توضیح اینکه این پارامتر چه چیزی است و چه محدودیتی دارد
 * @param damage میزان آسیب (باید >= 0 باشد)
 * @return مقدار خروجی و معنی آن
 * @throws IllegalArgumentException اگر damage منفی باشد
 */
public int dealDamage(Zombie zombie, int damage) { ... }
```

#### چه چیزهایی را در Javadoc متد بنویسیم؟
- **هدف متد**
- **شرایط ورودی** (مثلاً null نبودن، بازه مجاز)
- **اثر جانبی** (Side effects): تغییر وضعیت بازی، کم شدن HP، افزودن به لیست‌ها، IO
- **پیچیدگی/کارایی** (اگر مهم است)
- **استثناها** (چه زمانی throw می‌شود)

---

### 5) کامنت‌گذاری برای فیلدها (Fields)
فیلدها معمولاً Javadoc لازم ندارند مگر:
- `public` باشند (ترجیحاً public field نداشته باشیم)
- مفهوم خاص/غیر بدیهی داشته باشند
- واحد/محدودیت مهم داشته باشند

#### مثال
```java
/** Current health points of the plant. Must be between 0 and maxHp. */
private int hp;
```

یا اگر ثابت است:
```java
/** Maximum allowed tiles in a row (game design constraint). */
private static final int MAX_TILES_PER_ROW = 9;
```

---

### 6) کامنت‌گذاری برای کدهای پیچیده (Inline Comments)
Inline comment فقط وقتی استفاده شود که:
- الگوریتم پیچیده است
- دلیل یک تصمیم غیر بدیهی است
- workaround یا محدودیت خارجی دارید

#### مثال
```java
// Workaround: network packets may arrive out of order; we ignore outdated ticks.
if (packetTick < lastAppliedTick) return;
```

---

### 7) علائم اختصاری و انواع کامنت در Java
#### 1) کامنت تک‌خطی
```java
// توضیح کوتاه
```

#### 2) کامنت چندخطی
```java
/*
  توضیح چند خطی
*/
```

#### 3) Javadoc (برای تولید مستندات)
```java
/**
 * توضیح
 * @param ...
 */
```

#### 4) TODO / FIXME / NOTE
این‌ها برای مدیریت کارهای باقی‌مانده مفیدند و IntelliJ آنها را پیدا می‌کند.

- `TODO`: کاری که باید انجام شود
- `FIXME`: مشکل/باگ شناخته‌شده که باید اصلاح شود
- `NOTE`: نکته مهم برای خواننده
- `HACK`: راه‌حل موقت (بهتر است کم استفاده شود)

مثال:
```java
// TODO: add cooldown mechanic for plants
// FIXME: this method fails when waveIndex == 0
// NOTE: this value comes from game design doc
```

در IntelliJ:
- از منو: `View > Tool Windows > TODO`
می‌توانید همه TODOها را ببینید.

---

## میانبرها و Auto-complete برای Javadoc در IntelliJ

### 1) تولید سریع Javadoc برای متد با Auto-complete
روی خط بالای متد این را بنویسید و Enter بزنید:

```java
/**
```

IntelliJ معمولاً خودش این‌ها را اضافه می‌کند:
- `@param` برای همه پارامترها
- `@return` اگر خروجی غیر void باشد
- `@throws` اگر در signature استثنا اعلام شده باشد

### 2) تکمیل خودکار تگ‌ها
داخل Javadoc وقتی `@pa` تایپ کنید، IntelliJ پیشنهاد `@param` می‌دهد.  
همچنین هنگام نوشتن نام پارامترها، به شما پیشنهاد می‌دهد.

### 3) تنظیم قالب خودکار برای کلاس‌های جدید (File Template)
برای اینکه هنگام ساخت کلاس جدید، بالای کلاس این قالب بیاید:

```java
/**
 * $DESCRIPTION$
 *
 * @author YourName
 */
```

مسیر تنظیم:
- `Settings/Preferences`
- `Editor`
- `File and Code Templates`
- تب `Files`
- قالب `Class` را ویرایش کنید و Javadoc را بالای `public class ${NAME}` بگذارید.

> نکته: `DESCRIPTION` متغیر پیش‌فرض IntelliJ نیست؛ معمولاً از `TODO` یا متن ثابت استفاده می‌کنیم، یا با Live Template جایگزین می‌کنیم.

### 4) Live Template برای درج سریع Javadoc (برای کلاس‌های موجود)
اگر می‌خواهید با یک میانبر Javadoc را سریع درج کنید:
- `Settings > Editor > Live Templates`
- یک template مثل `jcls` بسازید و متن را قرار دهید:

```java
/**
 * $END$
 *
 * @author YourName
 */
```

سپس در کد:
- `jcls` تایپ کنید و `Tab` بزنید.

---

## قوانین Merge Conflict و حل تعارض‌ها
### تعارض (Conflict) کی رخ می‌دهد؟
وقتی دو نفر یک قسمت مشابه از کد را تغییر داده باشند.

### راه کاهش Conflict
- کارها را خرد کنید و زودتر push کنید
- قبل از تغییر فایل‌های مشترک، با هم هماهنگ کنید
- ویژگی‌ها را در شاخه جدا پیاده کنید

### حل Conflict در IntelliJ
IntelliJ ابزار merge دارد:
- هنگام conflict، فایل‌ها مشخص می‌شوند
- با **Merge Tool** سه پنل می‌بینید (Yours / Theirs / Result)
- نتیجه را ذخیره کنید و commit کنید

---

## چک‌لیست قبل از Merge به main
قبل از اینکه MR را merge کنید:

- [ ] `mvn clean verify` بدون خطا اجرا شود
- [ ] Checkstyle و PMD پاس شوند
- [ ] کد بی‌استفاده (unused) حذف شده باشد
- [ ] نام‌گذاری‌ها استاندارد باشد
- [ ] متدهای public دارای Javadoc باشند
- [ ] توضیحات MR کامل باشد (چه شد؟ چطور تست شد؟)
- [ ] حداقل یک نفر review کرده باشد

---

## نکات نهایی مفید برای تیم
- فایل‌های IDE مثل `.idea/` و `*.iml` را **commit نکنید** (در `.gitignore` باشد).
- تغییرات تنظیمات کیفیت کد (`checkstyle.xml`, `ruleset.xml`, `pom.xml`) حساس هستند؛ قبل از تغییر با تیم هماهنگ کنید.
- اگر `main` محافظت شده است، **Force Push ممنوع** (حتی اگر بلد باشید، در تیم خطرناک است).
- برای کارهای بزرگ، Issue بسازید و شاخه را به Issue لینک کنید.

---

### پیشنهاد مسیرهای پروژه (پیشنهادی)
```
config/
  checkstyle/
    checkstyle.xml
  pmd/
    ruleset.xml
src/
  main/
    java/
  test/
    java/
```

---

## اجرای سریع (Quick Start)
1. پروژه را clone کنید
2. در IntelliJ وارد کنید
3. (در صورت نیاز) افزونه‌های Checkstyle/PMD را فعال کنید و به فایل‌های config اشاره دهید
4. قبل از MR، `mvn clean verify` را اجرا کنید
5. MR بسازید و review بگیرید

---

