package com.iccuu.general_web_backend.module.mfa.strategy;

import com.iccuu.general_web_backend.common.enums.MfaType;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import org.springframework.stereotype.Component;

@Component
public class EmailMfaVerifier implements MfaVerifier {

    @Override
    public MfaType supportedType() {
        return MfaType.EMAIL;
    }

    @Override
    public boolean verify(UserMfa mfaRecord, String code) {
        throw new UnsupportedOperationException("Email MFA not yet implemented");
    }

    @Override
    public boolean verifyAndConsumeBackupCode(UserMfa mfaRecord, String code) {
        throw new UnsupportedOperationException("Email MFA backup codes not applicable");
    }
}
