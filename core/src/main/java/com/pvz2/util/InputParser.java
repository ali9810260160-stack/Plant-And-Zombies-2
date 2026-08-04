package com.pvz2.util;

import com.pvz2.model.enums.CommandRegex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * پارس‌کننده مرکزی دستورات ورودی با Regex.
 */
public class InputParser {

    public static Matcher match(String input, CommandRegex regex) {
        Pattern p = Pattern.compile(regex.getPattern(), Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(input.trim());
        return m.matches() ? m : null;
    }

    public static boolean matches(String input, CommandRegex regex) {
        return match(input, regex) != null;
    }

    public static String getGroup(Matcher matcher, int group) {
        if (matcher == null) {
            return null;
        }
        try {
            return matcher.group(group);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getInt(Matcher matcher, int group) {
        String s = getGroup(matcher, group);
        try {
            return s != null ? Integer.parseInt(s) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
