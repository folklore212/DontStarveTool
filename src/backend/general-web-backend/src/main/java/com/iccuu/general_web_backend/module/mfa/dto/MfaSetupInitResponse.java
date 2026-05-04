package com.iccuu.general_web_backend.module.mfa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupInitResponse {
    private String secret;
    private String qrCodeUri;
    private List<String> backupCodes;
}
