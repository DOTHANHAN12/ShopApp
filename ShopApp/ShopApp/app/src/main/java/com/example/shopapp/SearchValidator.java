package com.example.shopapp;

import java.util.regex.Pattern;

/**
 * Utility class để validate search keywords
 * - Ngăn SQL injection
 * - Validate input format
 * - Sanitize dangerous characters
 */
public class SearchValidator {

    private static final String TAG = "SearchValidator";

    // Min-max length constants
    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_KEYWORD_LENGTH = 100;

    // Pattern để detect SQL injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|onerror|onclick|onload)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern cho dangerous characters
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>\"'%;()&+\\\\]");

    /**
     * Validate search keyword
     * @param keyword Input keyword
     * @return true if valid, false otherwise
     */
    public static boolean isValidKeyword(String keyword) {
        if (keyword == null) {
            return false;
        }

        keyword = keyword.trim();

        // Check empty
        if (keyword.isEmpty()) {
            return false;
        }

        // Check length
        if (keyword.length() < MIN_KEYWORD_LENGTH) {
            return false;
        }

        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            return false;
        }

        // Check for SQL injection patterns
        if (isSuspiciousPattern(keyword)) {
            return false;
        }

        // Check for dangerous characters
        if (hasDangerousCharacters(keyword)) {
            return false;
        }

        return true;
    }

    /**
     * Check if keyword contains suspicious patterns (SQL injection attempts)
     */
    private static boolean isSuspiciousPattern(String keyword) {
        return SQL_INJECTION_PATTERN.matcher(keyword).find();
    }

    /**
     * Check if keyword contains dangerous characters
     */
    private static boolean hasDangerousCharacters(String keyword) {
        return DANGEROUS_CHARS.matcher(keyword).find();
    }

    /**
     * Sanitize keyword by removing dangerous characters
     */
    public static String sanitizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        keyword = keyword.trim();
        // Remove dangerous characters
        keyword = DANGEROUS_CHARS.matcher(keyword).replaceAll("");
        // Replace multiple spaces with single space
        keyword = keyword.replaceAll("\\s+", " ");

        return keyword;
    }

    /**
     * Get error message based on validation failure
     */
    public static String getErrorMessage(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return "Vui lòng nhập từ khóa tìm kiếm";
        }

        keyword = keyword.trim();

        if (keyword.length() < MIN_KEYWORD_LENGTH) {
            return String.format("Từ khóa phải có ít nhất %d ký tự", MIN_KEYWORD_LENGTH);
        }

        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            return String.format("Từ khóa không được quá %d ký tự", MAX_KEYWORD_LENGTH);
        }

        if (isSuspiciousPattern(keyword)) {
            return "Từ khóa chứa ký tự không được phép";
        }

        if (hasDangerousCharacters(keyword)) {
            return "Từ khóa chứa ký tự không hợp lệ";
        }

        return "Từ khóa không hợp lệ";
    }

    /**
     * Validate price range
     */
    public static boolean isValidPriceRange(int minPrice, int maxPrice) {
        return minPrice >= 0 && maxPrice >= minPrice && maxPrice <= 10000000;
    }

    /**
     * Validate rating
     */
    public static boolean isValidRating(int rating) {
        return rating >= 0 && rating <= 5;
    }

    /**
     * Validate sort option
     */
    public static boolean isValidSortOption(String sortOption) {
        return sortOption != null && (
                sortOption.equals("NEWEST") ||
                        sortOption.equals("PRICE_LOW_TO_HIGH") ||
                        sortOption.equals("PRICE_HIGH_TO_LOW") ||
                        sortOption.equals("RATING") ||
                        sortOption.equals("POPULAR")
        );
    }
}