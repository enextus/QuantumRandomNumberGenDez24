package org.ThreeDotsSierpinski.rng;

/**
 * Structured reason for PSEUDO fallback.
 *
 * <p>The UI can use this enum instead of parsing human-readable status text.</p>
 */
public enum FallbackReason {
    NO_API_KEY("no API key"),
    DEFAULT_LOCAL("manual"),
    MANUAL("manual"),
    RATE_LIMIT("rate limit"),
    NETWORK_DOWN("API unavailable"),
    API_ERROR("API error");

    private final String displayText;

    FallbackReason(String displayText) {
        this.displayText = displayText;
    }

    public String displayText() {
        return displayText;
    }

    public boolean isRateLimit() {
        return this == RATE_LIMIT;
    }

    public boolean allowsReconnect() {
        return this != RATE_LIMIT;
    }
}
