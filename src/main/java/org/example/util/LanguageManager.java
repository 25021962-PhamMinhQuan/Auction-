package org.example.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LanguageManager {

    private static Locale currentLocale = new Locale("vi"); // mặc định tiếng Việt
    private static ResourceBundle bundle = ResourceBundle.getBundle("i18n/messages", currentLocale);
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(LanguageManager.class);
    public static ResourceBundle getBundle() {
        return bundle;
    }
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "?" + key + "?"; // fallback nếu key không tồn tại
        }
    }

    public static void setEnglish() {
        currentLocale = Locale.ENGLISH;
        bundle = ResourceBundle.getBundle("i18n/messages", currentLocale);
    }

    public static void setVietnamese() {
        currentLocale = new Locale("vi");
        bundle = ResourceBundle.getBundle("i18n/messages", currentLocale);
    }

    public static void toggle() {
        if (currentLocale.getLanguage().equals("vi")) {
            setEnglish();
        } else {
            setVietnamese();
        }
    }

    public static boolean isVietnamese() {
        return currentLocale.getLanguage().equals("vi");
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }
}