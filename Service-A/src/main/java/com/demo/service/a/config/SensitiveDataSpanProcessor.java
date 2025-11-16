package com.demo.service.a.config;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpanProcessor để mask sensitive data trong span attributes
 */
public class SensitiveDataSpanProcessor implements SpanProcessor {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataSpanProcessor.class);

    // Sensitive attribute keys cần mask
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "http.request.body",
            "http.request.header.authorization",
            "http.request.header.cookie",
            "http.request.header.x-api-key",
            "http.request.header.x-auth-token",
            "http.response.header.set-cookie",
            "db.statement",  // SQL queries
            "http.url",  // URLs có thể chứa passwords
            "http.target",  // Query params có thể chứa tokens
            "messaging.message.payload",  // Message payloads
            "rpc.request.body",
            "rpc.response.body"


    ));

    // Patterns để detect sensitive data trong values
    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
            // Password patterns
            Pattern.compile("(email)\\s*[=:]\\s*['\"]?([^'\"\\s&]+)", Pattern.CASE_INSENSITIVE),

            Pattern.compile("(password|passwd|pwd)\\s*[=:]\\s*['\"]?([^'\"\\s&]+)", Pattern.CASE_INSENSITIVE),
            // Token patterns
            Pattern.compile("(token|secret|apikey|api_key)\\s*[=:]\\s*['\"]?([^'\"\\s&]+)", Pattern.CASE_INSENSITIVE),
            // Credit card patterns
            Pattern.compile("(credit_?card|card_?number)\\s*[=:]\\s*['\"]?([^'\"\\s&]+)", Pattern.CASE_INSENSITIVE),
            // Authorization header
            Pattern.compile("(Bearer|Basic)\\s+([A-Za-z0-9\\-._~+/]+=*)", Pattern.CASE_INSENSITIVE),
            // JWT tokens
            Pattern.compile("eyJ[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]*"),
            // API keys (common formats)
            Pattern.compile("(sk_|pk_|api_)[a-zA-Z0-9]{20,}"),
            // Credit card numbers
            Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b")
    );

    private static final String MASK = "***MASKED***";

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // Mask attributes khi span start
        try {
            Attributes originalAttributes = span.toSpanData().getAttributes();
            Attributes maskedAttributes = maskAttributes(originalAttributes);

            // Update span với masked attributes
            if (!maskedAttributes.equals(originalAttributes)) {
                // Set lại attributes
                maskedAttributes.forEach((key, value) -> {
                    span.setAttribute((AttributeKey<Object>) key, value);
                });

                log.debug("Masked sensitive data in span: {}", span.getName());
            }
        } catch (Exception e) {
            log.error("Error masking span attributes on start: {}", e.getMessage());
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // Mask attributes khi span end (để đảm bảo)
        try {
            Attributes attributes = span.toSpanData().getAttributes();

            // Check nếu có sensitive data
            boolean hasSensitiveData = false;
            for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
                String key = entry.getKey().getKey();
                if (isSensitiveKey(key) || containsSensitiveData(String.valueOf(entry.getValue()))) {
                    hasSensitiveData = true;
                    break;
                }
            }

            if (hasSensitiveData) {
                log.debug("Span contains sensitive data - already masked: {}", span.getName());
            }
        } catch (Exception e) {
            log.error("Error checking span attributes on end: {}", e.getMessage());
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    /**
     * Mask sensitive attributes
     */
    private Attributes maskAttributes(Attributes attributes) {
        AttributesBuilder builder = Attributes.builder();

        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
            AttributeKey<?> key = entry.getKey();
            Object value = entry.getValue();

            String keyStr = key.getKey();
            String valueStr = String.valueOf(value);

            // Check nếu key hoặc value chứa sensitive data
            if (isSensitiveKey(keyStr)) {
                // Mask toàn bộ value
                builder.put((AttributeKey<String>) key, MASK);
                log.trace("Masked attribute key: {}", keyStr);
            } else if (containsSensitiveData(valueStr)) {
                // Mask sensitive parts trong value
                String maskedValue = maskSensitiveDataInValue(valueStr);
                builder.put((AttributeKey<String>) key, maskedValue);
                log.trace("Masked sensitive data in attribute: {}", keyStr);
            } else {
                // Keep original
                builder.put((AttributeKey<Object>) key, value);
            }
        }

        return builder.build();
    }

    /**
     * Kiểm tra xem key có phải sensitive không
     */
    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }

        String lowerKey = key.toLowerCase();

        // Check exact match
        if (SENSITIVE_KEYS.contains(lowerKey)) {
            return true;
        }

        // Check partial match
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return true;
            }
        }

        // Check common sensitive keywords
        return lowerKey.contains("password") ||
                lowerKey.contains("secret") ||
                lowerKey.contains("token") ||
                lowerKey.contains("apikey") ||
                lowerKey.contains("authorization") ||
                lowerKey.contains("auth") ||
                lowerKey.contains("email") ||
                lowerKey.contains("credential");
    }

    /**
     * Kiểm tra xem value có chứa sensitive data không
     */
    private boolean containsSensitiveData(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Mask sensitive data trong value
     */
    private String maskSensitiveDataInValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String masked = value;

        // Apply all patterns
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            Matcher matcher = pattern.matcher(masked);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String replacement;

                if (matcher.groupCount() >= 2) {
                    // Pattern có group cho field name và value
                    String fieldName = matcher.group(1);
                    replacement = fieldName + "=" + MASK;
                } else {
                    // Pattern chỉ match value
                    replacement = MASK;
                }

                matcher.appendReplacement(sb, replacement);
            }
            matcher.appendTail(sb);
            masked = sb.toString();
        }

        return masked;
    }

    /**
     * Mask SQL statements
     */
    private String maskSqlStatement(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        String masked = sql;

        // Mask password values in SQL
        masked = masked.replaceAll(
                "(?i)(password|passwd|pwd)\\s*=\\s*'([^']+)'",
                "$1 = '" + MASK + "'"
        );

        // Mask token values
        masked = masked.replaceAll(
                "(?i)(token|secret|api_?key)\\s*=\\s*'([^']+)'",
                "$1 = '" + MASK + "'"
        );

        return masked;
    }

    /**
     * Mask URL query parameters
     */
    private String maskUrlParameters(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        String masked = url;

        // Mask password in query params
        masked = masked.replaceAll(
                "(?i)([?&])(password|passwd|pwd|token|secret|apikey)=([^&]+)",
                "$1$2=" + MASK
        );

        return masked;
    }
}