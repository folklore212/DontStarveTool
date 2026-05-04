package com.iccuu.general_web_backend.common.util;

import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateUtil {
    private DateUtil() {}

    public static LocalDateTime parseDateTimeParam(String value, boolean isStart) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Date parameter must not be blank");
        }
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                return isStart ? date.atStartOfDay() : date.atTime(23, 59, 59);
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Invalid date format: '" + value + "'. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss");
        }
    }
}
