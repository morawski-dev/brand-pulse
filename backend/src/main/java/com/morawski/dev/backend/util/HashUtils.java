package com.morawski.dev.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for hashing operations.
 * Provides SHA-256 hashing for content comparison and deduplication.
 */
public final class HashUtils {

    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private HashUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates SHA-256 hash of a string.
     * Used for review content deduplication.
     *
     * @param content the content to hash
     * @return hex-encoded SHA-256 hash
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public static String sha256(String content) {
        if (content == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates content hash for review deduplication.
     * Normalizes content before hashing (trim, lowercase, remove extra whitespace).
     *
     * @param reviewContent the review content
     * @return normalized SHA-256 hash
     */
    public static String generateContentHash(String reviewContent) {
        if (reviewContent == null || reviewContent.trim().isEmpty()) {
            return sha256("");
        }

        // Normalize: trim, lowercase, collapse whitespace
        String normalized = reviewContent.trim()
            .toLowerCase()
            .replaceAll("\\s+", " ");

        return sha256(normalized);
    }

    /**
     * Verifies if content matches a given hash.
     *
     * @param content      the content to verify
     * @param expectedHash the expected hash value
     * @return true if content matches hash
     */
    public static boolean verifyContentHash(String content, String expectedHash) {
        if (content == null || expectedHash == null) {
            return false;
        }
        String actualHash = generateContentHash(content);
        return actualHash.equals(expectedHash);
    }

    /**
     * Checks if two content strings are identical (ignoring whitespace/case normalization).
     *
     * @param content1 first content string
     * @param content2 second content string
     * @return true if normalized contents match
     */
    public static boolean areContentsEqual(String content1, String content2) {
        String hash1 = generateContentHash(content1);
        String hash2 = generateContentHash(content2);
        return hash1.equals(hash2);
    }

    /**
     * Generates a unique compound key hash for duplicate detection.
     * Example: reviewSourceId + externalReviewId -> unique hash.
     *
     * @param parts the parts to combine and hash
     * @return SHA-256 hash of combined parts
     */
    public static String generateCompoundHash(String... parts) {
        if (parts == null || parts.length == 0) {
            return sha256("");
        }

        StringBuilder combined = new StringBuilder();
        for (String part : parts) {
            combined.append(part != null ? part : "").append("|");
        }

        return sha256(combined.toString());
    }
}
