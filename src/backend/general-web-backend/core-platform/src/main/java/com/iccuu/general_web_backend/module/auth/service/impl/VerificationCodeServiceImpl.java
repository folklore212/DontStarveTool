package com.iccuu.general_web_backend.module.auth.service.impl;

import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.enums.IdentityType;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.common.util.SecureRandomUtil;
import com.iccuu.general_web_backend.module.auth.service.VerificationCodeService;
import com.iccuu.general_web_backend.module.auth.service.VerifyResult;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeServiceImpl.class);

    private final RedisUtil redisUtil;
    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final MessageSource mailMessageSource;

    public VerificationCodeServiceImpl(
            RedisUtil redisUtil,
            JavaMailSender mailSender,
            ITemplateEngine templateEngine,
            @Qualifier("mailMessageSource") MessageSource mailMessageSource) {
        this.redisUtil = redisUtil;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailMessageSource = mailMessageSource;
    }

    @Value("${spring.mail.host:smtp.example.com}")
    private String mailHost;

    @Value("${spring.mail.username:noreply@example.com}")
    private String mailFrom;

    @Override
    public void send(String identifier, String identityType, String purpose, String locale) {
        String normalizedPurpose = normalizePurpose(purpose);
        String rlKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC_RL + ":%s", normalizedPurpose, identifier);
        if (redisUtil.exists(rlKey)) {
            log.warn("Verification code rate limit exceeded for identifier: {}", identifier);
            return;
        }

        String code = generateCode();

        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, normalizedPurpose, identifier);
        redisUtil.set(codeKey, code, Constants.VERIFICATION_CODE_TTL_SECONDS, TimeUnit.SECONDS);

        redisUtil.set(rlKey, "1", 60, TimeUnit.SECONDS);

        if (IdentityType.EMAIL.getValue().equalsIgnoreCase(identityType) && !isMailPlaceholder()) {
            try {
                Locale userLocale = parseLocale(locale);
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(mailFrom);
                helper.setTo(identifier);
                String subject = resolveSubject(normalizedPurpose, userLocale);
                String html = resolveHtml(code, normalizedPurpose, userLocale);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(mimeMessage);
                log.info("Verification code email sent to: {} (locale={})", identifier, userLocale);
            } catch (Exception e) {
                log.error("Failed to send verification code email to: {}", identifier, e);
            }
        } else if (IdentityType.PHONE.getValue().equalsIgnoreCase(identityType)) {
            log.info("[SMS] Verification code sent to: {} (purpose: {})", identifier, normalizedPurpose);
        } else {
            log.info("[DEV] Verification code generated for: {} (purpose: {})", identifier, normalizedPurpose);
        }
    }

    private static final Locale FALLBACK_LOCALE = Locale.forLanguageTag("zh-CN");

    private String resolveHtml(String code, String purpose, Locale locale) {
        return tryWithFallback(locale, (l) -> {
            Context ctx = new Context(l);
            ctx.setVariable("code", code);
            ctx.setVariable("purpose", resolvePurposeText(purpose, l));
            ctx.setVariable("minutes", String.valueOf(Constants.VERIFICATION_CODE_TTL_SECONDS / 60));
            String templateName = "email/verification-code_" + l.toLanguageTag();
            return templateEngine.process(templateName, ctx);
        });
    }

    private String resolveSubject(String purpose, Locale locale) {
        return tryWithFallback(locale, (l) -> {
            String purposeText = resolvePurposeText(purpose, l);
            return mailMessageSource.getMessage("mail.subject", new Object[]{purposeText}, l);
        });
    }

    private String resolvePurposeText(String purpose, Locale locale) {
        return tryWithFallback(locale, (l) ->
                mailMessageSource.getMessage("mail.purpose." + purpose, null, purpose, l));
    }

    /**
     * Try the action with the given locale; on any exception, retry with zh-CN.
     */
    private String tryWithFallback(Locale locale, java.util.function.Function<Locale, String> action) {
        try {
            return action.apply(locale);
        } catch (Exception e) {
            if (!FALLBACK_LOCALE.equals(locale)) {
                log.warn("Failed to resolve mail content for locale {}, falling back to zh-CN: {}",
                        locale, e.getMessage());
                return action.apply(FALLBACK_LOCALE);
            }
            throw e;
        }
    }

    private static final String RESET_PASSWORD = "reset_password";

    @Override
    public VerifyResult verify(String identifier, String code, String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, normalizedPurpose, identifier);
        String storedCode = redisUtil.get(codeKey);
        if (storedCode == null) {
            return VerifyResult.EXPIRED;
        }
        if (storedCode.equals(code)) {
            if (!RESET_PASSWORD.equals(normalizedPurpose)) {
                redisUtil.delete(codeKey);
            }
            return VerifyResult.VALID;
        }
        return VerifyResult.INVALID;
    }

    public void consume(String identifier, String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, normalizedPurpose, identifier);
        String rlKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC_RL + ":%s", normalizedPurpose, identifier);
        redisUtil.delete(codeKey);
        redisUtil.delete(rlKey);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(Constants.VERIFICATION_CODE_LENGTH);
        for (int i = 0; i < Constants.VERIFICATION_CODE_LENGTH; i++) {
            sb.append(SecureRandomUtil.INSTANCE.nextInt(10));
        }
        return sb.toString();
    }

    private boolean isMailPlaceholder() {
        return mailHost == null || mailHost.isBlank() || "smtp.example.com".equals(mailHost);
    }

    private static String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "register";
        }
        return purpose.trim().toLowerCase();
    }

    /**
     * Parse locale string to java.util.Locale, normalized to a supported locale.
     * Accept-Language header can be complex ("zh-CN,zh;q=0.9,en;q=0.8"),
     * bare language codes ("zh", "en"), or null.
     * Always normalizes to one of: zh-CN, en.
     */
    private static Locale parseLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            return Locale.forLanguageTag("zh-CN");
        }
        String firstTag = raw.split(",")[0].split(";")[0].trim().toLowerCase();
        if (firstTag.isEmpty()) {
            return Locale.forLanguageTag("zh-CN");
        }
        // Normalize bare language codes
        if ("zh".equals(firstTag)) return Locale.forLanguageTag("zh-CN");
        if ("en".equals(firstTag)) return Locale.forLanguageTag("en");
        try {
            return Locale.forLanguageTag(firstTag);
        } catch (Exception e) {
            return Locale.forLanguageTag("zh-CN");
        }
    }
}
