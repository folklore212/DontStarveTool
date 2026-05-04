package com.iccuu.general_web_backend.common.exception;

import com.iccuu.general_web_backend.common.constant.ErrorCode;

public class AuthorizationException extends BusinessException {

    public AuthorizationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthorizationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
