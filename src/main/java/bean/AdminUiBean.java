package bean;

import entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Named("adminUi")
@ApplicationScoped
public class AdminUiBean {

    private static final ZoneId TZ = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR")).withZone(TZ);

    public String initials(AppUser user) {
        if (user == null) {
            return "?";
        }
        String n = trim(user.getName());
        String s = trim(user.getSurname());
        if (!n.isEmpty() && !s.isEmpty()) {
            return (firstLetter(n) + firstLetter(s)).toUpperCase(Locale.ROOT);
        }
        if (!n.isEmpty()) {
            String up = n.toUpperCase(Locale.ROOT);
            return up.length() >= 2 ? up.substring(0, 2) : up + up;
        }
        String email = trim(user.getEmail());
        if (email.length() >= 2) {
            return email.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return "?";
    }

    private static String firstLetter(String word) {
        return word.isEmpty() ? "" : word.substring(0, 1);
    }

    private static String trim(String x) {
        return x == null ? "" : x.trim();
    }

    public String formatShortDate(Instant instant) {
        if (instant == null) {
            return "—";
        }
        return SHORT_DATE.format(instant);
    }
}
