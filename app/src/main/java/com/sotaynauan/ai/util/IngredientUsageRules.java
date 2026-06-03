package com.sotaynauan.ai.util;

import java.util.Locale;

public final class IngredientUsageRules {
    private IngredientUsageRules() {
    }

    public static boolean isPresenceOnly(String name, String category, String note, String unit) {
        String text = normalize(join(name, category, note, unit));
        String normalizedCategory = normalize(category);
        if (normalizedCategory.equals("spice")
                || normalizedCategory.equals("sauce")
                || normalizedCategory.equals("oil")
                || normalizedCategory.equals("frozen")) {
            return true;
        }
        return containsAny(text,
                "gia vi", "muoi", "duong", "mat ong", "tieu", "nuoc mam", "nuoc tuong",
                "dau an", "sa te", "tuong ot", "bot nem", "hat nem", "nuoc loc",
                "nuoc soi", "da vien", "da lanh", "tuy thich", "vua du", "khau vi",
                "neu muon", "mot nhum", "nhum nho");
    }

    public static String presenceUnit() {
        return "có";
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String join(String name, String category, String note, String unit) {
        return safe(name) + " " + safe(category) + " " + safe(note) + " " + safe(unit);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        return UnitConverter.removeVietnameseAccent(value)
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
