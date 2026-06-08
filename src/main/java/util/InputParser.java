package util;

import model.enums.CommandRegex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * پارس‌کننده مرکزی دستورات ورودی با Regex.
 * هر دستور با CommandRegex تطبیق داده می‌شود.
 * تمام متدها static هستند — نیازی به نمونه‌سازی نیست.
 */
public class InputParser {

    // جلوگیری از نمونه‌سازی
    private InputParser() {
    }

    /**
     * ورودی را با یک الگوی CommandRegex تطبیق می‌دهد.
     * حالت CASE_INSENSITIVE و DOTALL فعال است.
     *
     * @param input رشته ورودی کاربر (trim نشده هم قبول می‌شود)
     * @param regex الگوی مورد نظر از CommandRegex
     * @return Matcher آماده (گروه‌ها قابل خواندن‌اند) یا null اگر مطابقت نداشت
     */
    public static Matcher match(String input, CommandRegex regex) {
        if (input == null || regex == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(
                regex.getPattern(),
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(input.trim());
        return matcher.matches() ? matcher : null;
    }

    /**
     * بررسی می‌کند آیا ورودی با یک الگو مطابقت دارد.
     *
     * @param input ورودی
     * @param regex الگو
     * @return true اگر مطابقت کامل داشت
     */
    public static boolean matches(String input, CommandRegex regex) {
        return match(input, regex) != null;
    }

    /**
     * گروه n-ام را از Matcher برمی‌گرداند.
     * اگر matcher یا گروه نامعتبر باشد null برمی‌گردد.
     *
     * @param matcher نتیجه match()
     * @param group   شماره گروه (از 1)
     * @return مقدار گروه یا null
     */
    public static String getGroup(Matcher matcher, int group) {
        if (matcher == null) {
            return null;
        }
        try {
            return matcher.group(group);
        } catch (IndexOutOfBoundsException | IllegalStateException e) {
            return null;
        }
    }

    /**
     * گروه n-ام را به صورت عدد صحیح برمی‌گرداند.
     * اگر تبدیل ممکن نباشد -1 برمی‌گردد.
     *
     * @param matcher نتیجه match()
     * @param group   شماره گروه
     * @return عدد صحیح یا -1
     */
    public static int getInt(Matcher matcher, int group) {
        String value = getGroup(matcher, group);
        if (value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * بررسی می‌کند آیا ورودی دستور stay-logged-in دارد.
     * چون این پرچم اختیاری است، جداگانه بررسی می‌شود.
     *
     * @param input ورودی خام
     * @return true اگر "-stay-logged-in" در ورودی باشد
     */
    public static boolean hasStayLoggedIn(String input) {
        if (input == null) {
            return false;
        }
        return input.trim().toLowerCase().contains("-stay-logged-in");
    }

    /**
     * ورودی را نرمال‌سازی می‌کند:
     * فاصله‌های اضافی بین کلمات حذف و trim انجام می‌شود.
     *
     * @param input ورودی خام
     * @return ورودی نرمال‌شده
     */
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * بررسی می‌کند آیا ورودی خالی یا null است.
     *
     * @param input ورودی
     * @return true اگر خالی یا null باشد
     */
    public static boolean isEmpty(String input) {
        return input == null || input.trim().isEmpty();
    }
}
