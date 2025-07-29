package org.example.userService.entity;

import java.util.Date;

public class TokenInfo {
    private final String userId;
    private final String tokenType;
    private final Date issuedAt;
    private final Date expiresAt;

    private TokenInfo(Builder builder) {
        this.userId = builder.userId;
        this.tokenType = builder.tokenType;
        this.issuedAt = builder.issuedAt;
        this.expiresAt = builder.expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getTokenType() { return tokenType; }
    public Date getIssuedAt() { return issuedAt; }
    public Date getExpiresAt() { return expiresAt; }

    public static class Builder {
        private String userId;
        private String tokenType;
        private Date issuedAt;
        private Date expiresAt;

        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder issuedAt(Date issuedAt) { this.issuedAt = issuedAt; return this; }
        public Builder expiresAt(Date expiresAt) { this.expiresAt = expiresAt; return this; }

        public TokenInfo build() { return new TokenInfo(this); }
    }
}