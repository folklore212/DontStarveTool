package com.iccuu.general_web_backend.module.auth.service;

public interface VerificationCodeService {
    void send(String identifier, String identityType, String purpose, String locale);

    /**
     * Verifies a code for the given identifier and purpose.
     * Returns VALID (correct), INVALID (wrong code), or EXPIRED (no code found).
     * For reset_password purpose, the code is NOT consumed — caller must call consume() after use.
     */
    VerifyResult verify(String identifier, String code, String purpose);

    /**
     * Explicitly consumes (deletes) the verification code for the given purpose and identifier.
     * Must be called after verify() for reset_password flow once the password has been reset.
     */
    void consume(String identifier, String purpose);
}
