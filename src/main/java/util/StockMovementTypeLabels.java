package util;

import java.util.Locale;

/**
 * Turkish labels for stock movement type codes stored in the database.
 */
public final class StockMovementTypeLabels {

    private StockMovementTypeLabels() {
    }

    public static String tr(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        String t = code.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "IN" -> "Giriş";
            case "OUT" -> "Çıkış";
            case "ADJUSTMENT" -> "Düzeltme";
            case "RETURN_IN" -> "İade girişi";
            case "RETURN_OUT" -> "İade çıkışı";
            default -> code.trim();
        };
    }
}
