package com.iccuu.general_web_backend.common.exception;

import com.iccuu.general_web_backend.common.constant.ErrorCode;

public class RateLimitException extends BusinessException {

    public RateLimitException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RateLimitException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
