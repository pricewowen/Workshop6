// Contributor(s): Robbie
// Main: Robbie - Bakery list filtering and sort for map and location search.

package com.example.workshop6.util;

import com.example.workshop6.data.model.BakeryLocationDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds a rich lowercase haystack for location list search (address tokens, province names,
 * postal variants, product names) and matches multi-word queries (all tokens must match).
 */
public final class LocationSearchHelper {

    private LocationSearchHelper() {
    }

    private static final Map<String, String> PROVINCE_ALIASES = new HashMap<>();

    static {
        addProvince("on", "ontario");
        addProvince("ont", "ontario");
        addProvince("ontario", "ontario");
        addProvince("ab", "alberta");
        addProvince("alta", "alberta");
        addProvince("alberta", "alberta");
        addProvince("bc", "british columbia");
        addProvince("b.c.", "british columbia");
        addProvince("british columbia", "british columbia");
        addProvince("mb", "manitoba");
        addProvince("man", "manitoba");
        addProvince("manitoba", "manitoba");
        addProvince("nb", "new brunswick");
        addProvince("new brunswick", "new brunswick");
        addProvince("nl", "newfoundland and labrador");
        addProvince("nf", "newfoundland and labrador");
        addProvince("newfoundland", "newfoundland and labrador");
        addProvince("labrador", "newfoundland and labrador");
        addProvince("ns", "nova scotia");
        addProvince("nova scotia", "nova scotia");
        addProvince("nt", "northwest territories");
        addProvince("northwest territories", "northwest territories");
        addProvince("nu", "nunavut");
        addProvince("nunavut", "nunavut");
        addProvince("pe", "prince edward island");
        addProvince("pei", "prince edward island");
        addProvince("p.e.i.", "prince edward island");
        addProvince("prince edward island", "prince edward island");
        addProvince("qc", "quebec");
        addProvince("pq", "quebec");
        addProvince("que", "quebec");
        addProvince("quebec", "quebec");
        addProvince("québec", "quebec");
        addProvince("sk", "saskatchewan");
        addProvince("sask", "saskatchewan");
        addProvince("saskatchewan", "saskatchewan");
        addProvince("yt", "yukon");
        addProvince("yk", "yukon");
        addProvince("yukon", "yukon");
    }

    private static void addProvince(String key, String canonicalLower) {
        PROVINCE_ALIASES.put(key.toLowerCase(Locale.ROOT), canonicalLower);
    }

    /**
     * Extra searchable text from the province field (example ON maps to ontario).
     */
    public static String provinceExpansion(String provinceRaw) {
        if (provinceRaw == null || provinceRaw.trim().isEmpty()) {
            return "";
        }
        String p = provinceRaw.trim().toLowerCase(Locale.ROOT);
        String canonical = PROVINCE_ALIASES.get(p);
        if (canonical != null) {
            return canonical;
        }
        return p;
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(part.trim());
    }

    /**
     * Postal code with and without spaces for partial matches.
     */
    private static String postalVariants(String postal) {
        if (postal == null || postal.isEmpty()) {
            return "";
        }
        String compact = postal.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        String spaced = postal.trim().toLowerCase(Locale.ROOT);
        if (compact.equals(spaced)) {
            return compact;
        }
        return compact + " " + spaced;
    }

    /**
     * Full lowercase haystack for one location.
     *
     * @param productNamesLower space-separated product names (already lowercased), from active batches
     */
    public static String buildHaystack(BakeryLocationDetails loc, String productNamesLower) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, loc.name);
        appendPart(sb, loc.address);
        appendPart(sb, loc.city);
        appendPart(sb, loc.province);
        appendPart(sb, provinceExpansion(loc.province));
        appendPart(sb, loc.postalCode);
        appendPart(sb, postalVariants(loc.postalCode));
        appendPart(sb, loc.phone);
        appendPart(sb, loc.email);
        appendPart(sb, loc.status);
        if (productNamesLower != null && !productNamesLower.isEmpty()) {
            appendPart(sb, productNamesLower);
        }
        if (loc.averageRating != null && !loc.averageRating.isNaN()) {
            double r = loc.averageRating;
            appendPart(sb, String.format(Locale.US, "%.1f", r));
            appendPart(sb, String.format(Locale.US, "%.2f", r));
            appendPart(sb, "stars");
            appendPart(sb, "star");
            appendPart(sb, "rating");
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /** Result of parseQuery with optional minimum average rating and remaining text tokens. */
    public static final class ParsedLocationQuery {
        public final Double minRating;
        public final String textQuery;

        public ParsedLocationQuery(Double minRating, String textQuery) {
            this.minRating = minRating;
            this.textQuery = textQuery != null ? textQuery : "";
        }
    }

    /**
     * Splits the query into an optional minimum rating from zero to five and remaining text. Max rating wins.
     * Ignores standalone star stars and star glyph tokens.
     */
    public static ParsedLocationQuery parseQuery(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ParsedLocationQuery(null, "");
        }
        List<Double> ratingTokens = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        for (String t : raw.trim().split("\\s+")) {
            if (t.isEmpty()) {
                continue;
            }
            String lower = t.toLowerCase(Locale.ROOT);
            if ("star".equals(lower) || "stars".equals(lower) || "★".equals(t)) {
                continue;
            }
            Double r = parseRatingNumberToken(t);
            if (r != null && r >= 0.0 && r <= 5.0) {
                ratingTokens.add(r);
            } else {
                textParts.add(t);
            }
        }
        Double minRating = ratingTokens.isEmpty() ? null : Collections.max(ratingTokens);
        String text = String.join(" ", textParts);
        return new ParsedLocationQuery(minRating, text);
    }

    private static Double parseRatingNumberToken(String t) {
        String s = t.trim();
        if (s.endsWith("★")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * When minRating is set the location average rating must be greater than or equal to that value.
     */
    public static boolean ratingSatisfies(Double averageRating, Double minRating) {
        if (minRating == null) {
            return true;
        }
        if (averageRating == null || averageRating.isNaN()) {
            return false;
        }
        return averageRating + 1e-9 >= minRating;
    }

    /**
     * Every whitespace-separated token in the query must appear in haystackLower.
     * Short tokens use whole-word matching so on does not match inside london. Longer tokens use substring match.
     */
    public static boolean matchesTokens(String queryRaw, String haystackLower) {
        if (queryRaw == null || queryRaw.trim().isEmpty()) {
            return true;
        }
        if (haystackLower == null || haystackLower.isEmpty()) {
            return false;
        }
        String[] tokens = queryRaw.trim().toLowerCase(Locale.ROOT).split("\\s+");
        for (String t : tokens) {
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() <= 2) {
                if (!containsWholeWord(haystackLower, t)) {
                    return false;
                }
            } else if (!haystackLower.contains(t)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsWholeWord(String haystackLower, String tokenLower) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(tokenLower) + "\\b");
        return p.matcher(haystackLower).find();
    }
}
