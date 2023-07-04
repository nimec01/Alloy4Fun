package pt.haslab.alloy4fun.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

public class MultiDateDeserializer {

    private static final Map<String, String> replaceRegexMutators = Map.of(
            "H+", "H",
            "m+", "m",
            "s+", "s",
            "a", "",
            "M+", "M",
            "d+", "d",
            "\\-", "\\/",
            "\\,", ""
    );

    public static Set<String> cycleFormats(String full) {
        Set<String> result = new HashSet<>(Set.of(full));
        List<Map.Entry<String, String>> entrySet = List.copyOf(replaceRegexMutators.entrySet());

        int ssize = replaceRegexMutators.size();
        for (int i = 0; i < ssize; i++) {
            for (int j = i; j < ssize; j++) {
                String s = full;
                for (int k = i; k <= j; k++) {
                    s = s.replaceAll(entrySet.get(k).getKey(), entrySet.get(k).getValue());
                }
                result.add(s.strip());
            }
        }
        return result;
    }

    public static List<SimpleDateFormat> mkFormats(String full) {
        return cycleFormats(full)
                .stream()
                .sorted((a, b) -> b.length() - a.length()) //Reversed Length Sorting
                .map(SimpleDateFormat::new).toList();
    }

    public static final List<SimpleDateFormat> yyyyMMddHHmmssaFormats = mkFormats("yyyy-MM-dd, HH:mm:ss a");
    public static final List<SimpleDateFormat> MMddyyyyHHmmssaFormats = mkFormats("MM-dd-yyyy, HH:mm:ss a");

    public static final Date minDate = Date.from(Instant.ofEpochMilli(946684800L)); // 946684800L == 1st of January 2000 (some weird dates can get parsed as the year 1)
    public static final Date maxDate = Date.from(Instant.now());

    public static boolean isValid(Date d) {
        return d != null && d.before(maxDate) && d.after(minDate);
    }


    public static Date deserialize(final String date, Date default_) {
        Date result;

        result = tryParsers(yyyyMMddHHmmssaFormats, date);
        if (isValid(result))
            return result;
        result = tryParsers(MMddyyyyHHmmssaFormats, date);
        if (isValid(result))
            return result;
        return default_;
    }

    private static Date tryParsers(List<SimpleDateFormat> formats, String date) {
        Date result = null;
        for (SimpleDateFormat formatter : formats) {
            try {
                result = formatter.parse(date);
            } catch (ParseException ignored) {
            }
        }
        return result;
    }
}
