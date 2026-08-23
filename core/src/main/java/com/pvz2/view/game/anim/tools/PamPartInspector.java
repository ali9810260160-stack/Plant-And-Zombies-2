package com.pvz2.view.game.anim.tools;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

/**
 * ابزار دیباگ برای کشف نام‌های واقعی part های داخل یک فایل PAM —
 * به‌خصوص برای پیدا کردن نام دقیق sprite های armor (که در هیچ فایل
 * JSON موجود نیستند و فقط داخل خود باینری PAM ذخیره شده‌اند).
 *
 * ═══════════════════════════════════════════════════════════════════
 *  چرا این ابزار لازم است؟
 * ═══════════════════════════════════════════════════════════════════
 *  کتابخانه libPVZ یک متد رسمی برای این کار دارد:
 *
 *      PamPlayer.AnimationPart root = pamPlayer.getParts(pamPath);
 *
 *  که یک درخت کامل از تمام part های آن PAM را برمی‌گرداند (نام هر
 *  part + آیا خودش یک تصویر/resource است یا فقط یک گروه/bone). خود
 *  توسعه‌دهندگان libPVZ در کامنت این متد نوشته‌اند: "not really usefull.
 *  only for the browser." — یعنی دقیقاً برای همین منظور (کاوش/debug)
 *  طراحی شده است.
 *
 *  طبق نمونه رسمی Demo.java از خود کتابخانه، نام‌های armor واقعی به
 *  این شکل هستند (نمونه واقعی برای زامبی پایه مصر):
 *      "_zombie_egypt_armor2_states"   (یک state-container)
 *      "zombie_armor_bucket_norm"      (خود sprite زره سطل)
 *
 *  یعنی الگوی کلی احتمالی: zombie_armor_{type}_{variant}
 *  اما تنها راه قطعی برای تایید نام هر PAM خاص همین ابزار (یا PVZ
 *  Asset Browser) است.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  نحوه استفاده
 * ═══════════════════════════════════════════════════════════════════
 * <pre>
 *   // یک‌بار، در طول توسعه (نه در build نهایی):
 *   PamPartInspector.dumpPartsToLog(GameAssets.getInstance().getPamPlayer(),
 *       "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM");
 *
 *   // خروجی در Logcat/Console چیزی شبیه این است:
 *   //   root
 *   //     body [resource]
 *   //     _zombie_egypt_armor2_states
 *   //       zombie_armor_cone_norm [resource]
 *   //       zombie_armor_bucket_norm [resource]
 *   //       zombie_armor_helmet_norm [resource]
 *
 *   // یا برای گرفتن یک لیست تخت از فقط نام‌های حاوی "armor":
 *   List&lt;String&gt; armorNames = PamPartInspector.findPartsContaining(
 *       pamPlayer, pamPath, "armor");
 * </pre>
 *
 * پیشنهاد عملی: یک دستور debug موقت (مثلاً کلید F9 در حالت توسعه، یا
 * یک متد در {@code com.pvz2.util} صدا زده‌شده از یک منوی دیباگ) اضافه
 * کنید که برای PAM زامبی زیر نشانگر موس این را اجرا کند؛ این سریع‌ترین
 * راه برای کشف تمام armor part names ۳۳ زامبی این پروژه است، بدون
 * نیاز به اجرای جداگانه Asset Browser.
 *
 * این کلاس با reflection به {@code pvz.libpvz.pam.PamPlayer.getParts()}
 * و ساختار {@code AnimationPart} دسترسی پیدا می‌کند تا در صورت نبود
 * asset واقعی (یا نسخه متفاوت libPVZ)، بدون کرش فقط خطا لاگ کند —
 * دقیقاً هم‌راستا با فلسفه fallback-ایمن {@link com.pvz2.view.game.anim.controller.PamDrawUtil}.
 */
public final class PamPartInspector {

    private static final String TAG = "PamPartInspector";

    private PamPartInspector() { }

    /**
     * چاپ کامل درخت part های یک PAM در لاگ (Gdx.app.log).
     * از این برای کشف دستی نام‌های armor استفاده کنید.
     *
     * @param pamPlayer نمونه‌ی واقعی {@code pvz.libpvz.pam.PamPlayer}
     *                  (به‌عنوان Object پاس داده می‌شود تا این کلاس به
     *                  libPVZ کامپایل‌تایم وابسته نباشد و در نبود asset
     *                  هم بدون کرش کار کند)
     * @param pamPath   مسیر PAM (مثلاً از character_animations.json)
     */
    public static void dumpPartsToLog(Object pamPlayer, String pamPath) {
        if (pamPlayer == null) {
            Gdx.app.error(TAG, "pamPlayer null است — asset واقعی بارگذاری نشده");
            return;
        }
        try {
            Object root = pamPlayer.getClass()
                    .getMethod("getParts", String.class)
                    .invoke(pamPlayer, pamPath);
            if (root == null) {
                Gdx.app.error(TAG, "PAM یافت نشد یا parse نشد: " + pamPath);
                return;
            }
            Gdx.app.log(TAG, "=== درخت part های " + pamPath + " ===");
            printRecursive(root, 0);
        } catch (NoSuchMethodException e) {
            Gdx.app.error(TAG, "متد getParts در این نسخه از libPVZ یافت نشد — "
                + "PVZ Asset Browser را برای کشف نام‌های part استفاده کنید.");
        } catch (Exception e) {
            Gdx.app.error(TAG, "خطا در فراخوانی getParts برای " + pamPath + ": " + e.getMessage());
        }
    }

    private static void printRecursive(Object part, int depth) {
        try {
            java.lang.reflect.Field nameField     = part.getClass().getField("name");
            java.lang.reflect.Field resourceField = part.getClass().getField("resource");
            java.lang.reflect.Field childrenField = part.getClass().getField("children");

            String name = String.valueOf(nameField.get(part));
            boolean resource = Boolean.TRUE.equals(resourceField.get(part));
            java.util.List<?> children = (java.util.List<?>) childrenField.get(part);

            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) indent.append("  ");
            Gdx.app.log(TAG, indent + name + (resource ? " [resource]" : ""));

            if (children != null) {
                for (Object child : children) printRecursive(child, depth + 1);
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "خطا در پیمایش درخت part: " + e.getMessage());
        }
    }

    /**
     * جستجوی همه part هایی که نامشان شامل یک substring خاص است
     * (مثلاً "armor") — برای پیدا کردن سریع نام‌های armor بدون نیاز
     * به خواندن کل درخت با چشم.
     *
     * @param keyword substring مورد جستجو (حساس به بزرگی/کوچکی حروف نیست)
     * @return لیست تخت نام‌های part منطبق (خالی اگر پیدا نشد یا asset نبود)
     */
    public static List<String> findPartsContaining(Object pamPlayer, String pamPath, String keyword) {
        List<String> result = new ArrayList<>();
        if (pamPlayer == null) return result;
        try {
            Object root = pamPlayer.getClass()
                    .getMethod("getParts", String.class)
                    .invoke(pamPlayer, pamPath);
            if (root == null) return result;
            collectMatching(root, keyword.toLowerCase(), result);
        } catch (Exception e) {
            Gdx.app.error(TAG, "خطا در findPartsContaining: " + e.getMessage());
        }
        return result;
    }

    private static void collectMatching(Object part, String keywordLower, List<String> out) {
        try {
            java.lang.reflect.Field nameField     = part.getClass().getField("name");
            java.lang.reflect.Field childrenField = part.getClass().getField("children");

            String name = String.valueOf(nameField.get(part));
            if (name != null && name.toLowerCase().contains(keywordLower)) out.add(name);

            java.util.List<?> children = (java.util.List<?>) childrenField.get(part);
            if (children != null) {
                for (Object child : children) collectMatching(child, keywordLower, out);
            }
        } catch (Exception ignored) { }
    }
}
