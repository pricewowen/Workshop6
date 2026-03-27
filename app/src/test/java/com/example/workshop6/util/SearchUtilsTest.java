package com.example.workshop6.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchUtilsTest {

    @Test
    public void normalizeUserSearch_escapesLikeWildcards() {
        String normalized = SearchUtils.normalizeUserSearch("%_test\\value");
        assertEquals("\\%\\_test\\\\value", normalized);
    }

    @Test
    public void normalizeUserSearch_limitsLength() {
        String longInput = "a".repeat(Validation.SEARCH_QUERY_MAX_LENGTH + 20);
        String normalized = SearchUtils.normalizeUserSearch(longInput);
        assertEquals(Validation.SEARCH_QUERY_MAX_LENGTH, normalized.length());
    }

    @Test
    public void passwordStrength_requiresMixedCharacterClasses() {
        assertTrue(Validation.isPasswordStrong("BakerySafe!24"));
        assertFalse(Validation.isPasswordStrong("alllowercase123!"));
        assertFalse(Validation.isPasswordStrong("ALLUPPERCASE123!"));
        assertFalse(Validation.isPasswordStrong("NoSymbol123"));
        assertFalse(Validation.isPasswordStrong("NoDigit!Only"));
    }

    @Test
    public void limitLength_truncatesFreeText() {
        String longComment = "x".repeat(Validation.ORDER_COMMENT_MAX_LENGTH + 5);
        String limited = Validation.limitLength(longComment, Validation.ORDER_COMMENT_MAX_LENGTH);
        assertEquals(Validation.ORDER_COMMENT_MAX_LENGTH, limited.length());
    }
}
