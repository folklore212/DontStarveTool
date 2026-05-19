package com.iccuu.general_web_backend.module.mfa.strategy;

import com.iccuu.general_web_backend.common.enums.MfaType;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;

public interface MfaVerifier {

    MfaType supportedType();

    boolean verify(UserMfa mfaRecord, String code);

    boolean verifyAndConsumeBackupCode(UserMfa mfaRecord, String code);
}
