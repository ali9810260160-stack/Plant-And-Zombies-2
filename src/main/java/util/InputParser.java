package util;

import model.enums.CommandRegex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * پارس‌کننده مرکزی دستورات ورودی با Regex.
 * هر دستور با CommandRegex تطبیق داده می‌شود.
 */
public class InputParser {

    /**
     * ورودی را با یک regex مطابقت می‌دهد و Matcher برمی‌گرداند.
     * @param input رشته ورودی
     * @param regex الگوی regex
     * @return Matcher یا null اگر مطابقت نداشت
     */
    public static Matcher match(String input, CommandRegex regex) {
        Pattern p = Pattern.compile(regex.getPattern(), Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(input.trim());
        return m.matches() ? m : null;
    }

    /**
     * بررسی می‌کند آیا ورودی با regex مطابقت دارد.
     * @param input ورودی
     * @param regex regex
     * @return true اگر مطابقت داشت
     */
    public static boolean matches(String input, CommandRegex regex) {
        return match(input, regex) != null;
    }

    /**
     * گروه n-ام را از matcher برمی‌گرداند.
     * @param matcher matcher
     * @param group شماره گروه
     * @return مقدار گروه یا null
     */
    public static String getGroup(Matcher matcher, int group) {
        if (matcher == null) return null;
        try {
            return matcher.group(group);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * عدد صحیح از گروه matcher می‌گیرد.
     * @param matcher matcher
     * @param group شماره گروه
     * @return عدد یا -1 در صورت خطا
     */
    public static int getInt(Matcher matcher, int group) {
        String s = getGroup(matcher, group);
        try {
            return s != null ? Integer.parseInt(s) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
