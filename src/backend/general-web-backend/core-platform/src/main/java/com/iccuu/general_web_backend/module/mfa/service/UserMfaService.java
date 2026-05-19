package com.iccuu.general_web_backend.module.mfa.service;

import com.iccuu.general_web_backend.module.mfa.dto.*;
import java.util.List;

public interface UserMfaService {
    List<MfaStatusVO> getStatus(Long userId);
    MfaSetupInitResponse setupInit(Long userId, MfaSetupInitRequest request);
    void setupVerify(Long userId, MfaEnableRequest request);
    void disable(Long userId, MfaDisableRequest request);
    List<String> getBackupCodes(Long userId);
    boolean verifyTotp(Long userId, String code);
    boolean verifyAndConsumeBackupCode(Long userId, String code);
}
