package com.sotaynauan.ai.util;

import java.text.Normalizer;
import java.util.Locale;

public class UnitConverter {
    private static final double EPSILON = 0.0001d;

    public double toBase(double amount, String inputUnit, String expectedBaseUnit) {
        String unit = normalizeUnit(inputUnit);
        String base = normalizeUnit(expectedBaseUnit);
        if (amount < 0d) {
            throw new IllegalArgumentException("Amount must not be negative.");
        }
        if (unit.isEmpty()) {
            unit = base;
        }
        if (base.isEmpty()) {
            base = baseUnitFor(unit);
        }
        String sourceBase = baseUnitFor(unit);
        if (!sourceBase.equals(base)) {
            throw new IllegalArgumentException("Unit " + inputUnit + " cannot convert to " + expectedBaseUnit);
        }
        if ("kg".equals(unit)) {
            return amount * 1000d;
        }
        if ("l".equals(unit)) {
            return amount * 1000d;
        }
        if ("tsp".equals(unit)) {
            return amount * 5d;
        }
        if ("tbsp".equals(unit)) {
            return amount * 15d;
        }
        return amount;
    }

    public String baseUnitFor(String unit) {
        String normalized = normalizeUnit(unit);
        if ("kg".equals(normalized) || "g".equals(normalized) || "gram".equals(normalized)) {
            return "g";
        }
        if ("l".equals(normalized) || "ml".equals(normalized)
                || "tsp".equals(normalized) || "tbsp".equals(normalized)) {
            return "ml";
        }
        return "piece";
    }

    public String normalizeUnit(String unit) {
        if (unit == null) {
            return "";
        }
        String value = removeVietnameseAccent(unit)
                .toLowerCase(Locale.US)
                .replace(".", "")
                .trim();
        if (value.isEmpty()) {
            return "";
        }
        if ("gram".equals(value) || "gr".equals(value)) {
            return "g";
        }
        if ("lit".equals(value) || "liter".equals(value) || "lít".equals(value)) {
            return "l";
        }
        if (value.contains("muong canh") || value.contains("thia canh")) {
            return "tbsp";
        }
        if (value.contains("muong ca phe") || value.contains("thia ca phe")) {
            return "tsp";
        }
        if ("qua".equals(value) || "cai".equals(value) || "tep".equals(value)
                || "mieng".equals(value) || "cu".equals(value) || "nhanh".equals(value)
                || "o".equals(value) || "phan".equals(value) || "chen".equals(value)
                || "bat".equals(value) || "goi".equals(value) || "chai".equals(value)
                || "nam".equals(value) || "bo".equals(value)) {
            return "piece";
        }
        return value;
    }

    public boolean sameBaseUnit(String firstUnit, String secondUnit) {
        return baseUnitFor(firstUnit).equals(baseUnitFor(secondUnit));
    }

    public boolean isZero(double value) {
        return Math.abs(value) <= EPSILON;
    }

    public static String removeVietnameseAccent(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.replace('đ', 'd').replace('Đ', 'D');
    }
}
