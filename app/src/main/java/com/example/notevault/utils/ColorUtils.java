package com.example.notevault.utils;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing color palette and operations.
 */
public class ColorUtils {

    // Beautiful preset color palette for folders and notes
    private static final String[] COLOR_PALETTE = {
            "#FF6B6B", // Red
            "#4ECDC4", // Turquoise
            "#45B7D1", // Blue
            "#FFA07A", // Light Salmon
            "#98D8C8", // Mint
            "#F7DC6F", // Yellow
            "#BB8FCE", // Purple
            "#85C1E2", // Sky Blue
            "#F8B88B", // Peach
            "#96CEB4"  // Green
    };

    public static List<String> getColorPalette() {
        List<String> colors = new ArrayList<>();
        for (String color : COLOR_PALETTE) {
            colors.add(color);
        }
        return colors;
    }

    public static String getDefaultColor() {
        return COLOR_PALETTE[0];
    }

    public static int parseColor(String colorHex) {
        try {
            return Color.parseColor(colorHex);
        } catch (Exception e) {
            return Color.parseColor(getDefaultColor());
        }
    }

    /**
     * Lightens a color by adding transparency
     */
    public static int getLighterColor(String colorHex) {
        int color = parseColor(colorHex);
        int alpha = 100; // About 40% transparency
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
