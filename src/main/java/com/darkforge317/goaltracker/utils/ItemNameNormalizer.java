package com.darkforge317.goaltracker.utils;

import java.util.regex.Pattern;

/**
 * Normalizes item variant names for equivalent item counting and display.
 */
public final class ItemNameNormalizer
{
    private static final Pattern TRAILING_CHARGES_PATTERN = Pattern.compile("\\s*\\([0-9]+\\)$");
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("\\s[0-9]{1,3}$");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    private ItemNameNormalizer()
    {
    }

    /**
     * Strip numeric charge and degradation suffixes while preserving other parts of the item name.
     * Examples: "Amulet of glory (6)" becomes "Amulet of glory", and
     * "Torag's platelegs 100" becomes "Torag's platelegs".
     *
     * @param raw raw RuneLite item name
     * @return normalized base name, or {@code null} when {@code raw} is null
     */
    public static String normalizeItemNameVariants(final String raw)
    {
        if (raw == null)
        {
            return null;
        }

        String normalized = TRAILING_CHARGES_PATTERN.matcher(raw).replaceAll("").trim();
        normalized = TRAILING_NUMBER_PATTERN.matcher(normalized).replaceAll("").trim();
        return MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ");
    }
}
