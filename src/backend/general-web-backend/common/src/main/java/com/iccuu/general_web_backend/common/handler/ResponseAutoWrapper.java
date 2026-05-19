package com.iccuu.general_web_backend.common.handler;

import com.iccuu.general_web_backend.common.result.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Auto-wraps controller return values in R.ok().
 * If the return value is already an R, passes through unchanged.
 * Allows controllers to return plain data types without explicit R.ok() wrapping.
 */
@RestControllerAdvice(basePackages = "com.iccuu.general_web_backend")
public class ResponseAutoWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof R) {
            return body;
        }
        return R.ok(body);
    }
}
