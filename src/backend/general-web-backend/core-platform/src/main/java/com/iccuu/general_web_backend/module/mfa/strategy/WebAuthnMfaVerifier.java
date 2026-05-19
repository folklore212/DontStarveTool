package com.iccuu.general_web_backend.module.mfa.strategy;

import com.iccuu.general_web_backend.common.enums.MfaType;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import org.springframework.stereotype.Component;

@Component
public class WebAuthnMfaVerifier implements MfaVerifier {

    @Override
    public MfaType supportedType() {
        return MfaType.WEBAUTHN;
    }

    @Override
    public boolean verify(UserMfa mfaRecord, String code) {
        throw new UnsupportedOperationException("WebAuthn MFA not yet implemented");
    }

    @Override
    public boolean verifyAndConsumeBackupCode(UserMfa mfaRecord, String code) {
        throw new UnsupportedOperationException("WebAuthn MFA backup codes not applicable");
    }
}
