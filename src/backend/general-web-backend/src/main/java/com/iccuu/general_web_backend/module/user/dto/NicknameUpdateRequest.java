package com.iccuu.general_web_backend.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NicknameUpdateRequest {
    @NotBlank
    @Size(min = 1, max = 64)
    private String nickname;
}
