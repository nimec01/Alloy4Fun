package pt.haslab.alloy4fun.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Text {

    String tag_regex = "//SECRET";

    String options_regex = "TARGETS\\s+?((?:\\w+)+)";
    String tag_opt_regex = tag_regex + "\\s+" + options_regex;

    String pgs = "sig|fact|assert|check|fun|pred|run";
    String pgp = "var|one|abstract|lone|some";
    String pgd = "(?:(?:" + pgp + ")\\s+)?" + pgs;
    String comment = "/\\*(?:.|\\n)*?\\*/\\s*|//.*\\n|--.*\\n";
    String secret_prefix = "(" + tag_regex + "\\s*?\\n\\s*(?:" + comment + ")*?\\s*)(?: " + pgd + ")";

    String block_end = "(?:" + tag_regex + "\\s*?\\n\\s*)?(?:(?:" + comment + ")*?\\s*(?:" + pgd + ")\\s|$)";
    String secret = tag_regex + "\\s*?\\n\\s*(?:" + comment + ")*?\\s*((?:" + pgd + ")(?:.|\\n)*?)" + block_end;

    static List<Integer> getSecretPositions(String code) {
        List<Integer> result = new ArrayList<>();
        Pattern p = Pattern.compile(secret_prefix);
        Matcher m = p.matcher(code);

        for (int i = 0; m.find(i); i = m.end(1)) {
            result.add(m.end(1));
        }

        return result;
    }

    static String extractSecrets(String code) {
        Pattern p = Pattern.compile(secret);
        Matcher m = p.matcher(code);
        StringBuilder result = new StringBuilder();

        for (int i = 0; m.find(i); i = m.end(1))
            result.append(m.group(1).strip()).append("\n");

        return result.toString();
    }

    static int sortByFirstNumber(String a, String b) {
        Pattern p = Pattern.compile("\\d+");
        Matcher m1 = p.matcher(a), m2 = p.matcher(b);
        try {
            if (m1.find() && m2.find())
                return Integer.valueOf(m1.group()).compareTo(Integer.valueOf(m2.group()));
        } catch (NumberFormatException ignored) {
        }
        return a.compareTo(b);
    }

    static LocalDateTime parseDate(String dateString) {
        dateString = dateString.replaceAll(",", "").replaceAll("/", "-").strip();
        if (dateString.matches(".*?(?i:pm|am)")) {
            if (dateString.matches("^\\d{4}.*")) { //Start With Year
                return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-M-d h:m:s a").withLocale(Locale.ENGLISH));
            } else if (dateString.matches("\\d{1,2}.*")) { //Start With Month
                return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("M-d-yyyy h:m:s a").withLocale(Locale.ENGLISH));
            }
        } else {
            if (dateString.matches("^\\d{4}.*")) { //Start With Year
                return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"));
            } else if (dateString.matches("\\d{1,2}.*")) { //Start With day
                return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("M-d-yyyy H:m:s"));
            }
        }
        throw new IllegalArgumentException("Invalid date format: " + dateString);

    }

    static String lineCSV(String sep, List<String> strings) {
        if (strings == null || strings.isEmpty())
            return "";
        StringBuilder res = new StringBuilder();
        String last = strings.get(strings.size() - 1);
        for (int i = 0; i < strings.size() - 1; i++) res.append(strings.get(i)).append(sep);

        return res.append(last).toString();
    }
}
