/*
 * File purpose: Carries a safe OAuth failure and its intended HTTP status.
 */
package com.nexora.user.oauth;

public class OAuthFlowException extends RuntimeException {

    private final int status;

    public OAuthFlowException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
