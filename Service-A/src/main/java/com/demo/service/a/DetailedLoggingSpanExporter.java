package com.demo.service.a;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Custom LoggingSpanExporter hiển thị CHI TIẾT tất cả thông tin trong SpanData
 */
@Slf4j
public class DetailedLoggingSpanExporter implements SpanExporter {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String LINE = "-".repeat(80);

    private volatile boolean isShutdown = false;

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (isShutdown) {
            return CompletableResultCode.ofFailure();
        }

        log.info("\n{}\n📦 EXPORTING {} SPANS\n{}", SEPARATOR, spans.size(), SEPARATOR);

        int index = 1;
        for (SpanData span : spans) {
            logSpanDetails(span, index++);
        }

        log.info("{}\n✅ Export completed successfully\n{}\n", SEPARATOR, SEPARATOR);

        return CompletableResultCode.ofSuccess();
    }

    /**
     * Log tất cả chi tiết của một Span
     */
    private void logSpanDetails(SpanData span, int index) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n").append(LINE).append("\n");
        sb.append(String.format("🔹 SPAN #%d\n", index));
        sb.append(LINE).append("\n");

        // ========================================
        // 1. BASIC INFORMATION
        // ========================================
        sb.append("\n📋 BASIC INFORMATION:\n");
        sb.append(String.format("  Trace ID       : %s\n", span.getTraceId()));
        sb.append(String.format("  Span ID        : %s\n", span.getSpanId()));
        sb.append(String.format("  Parent Span ID : %s\n", span.getParentSpanId()));
        sb.append(String.format("  Name           : %s\n", span.getName()));
        sb.append(String.format("  Kind           : %s\n", span.getKind()));

        // ========================================
        // 2. TIMING INFORMATION
        // ========================================
        long startNanos = span.getStartEpochNanos();
        long endNanos = span.getEndEpochNanos();
        long durationNanos = endNanos - startNanos;

        Instant startTime = Instant.ofEpochSecond(0, startNanos);
        Instant endTime = Instant.ofEpochSecond(0, endNanos);

        sb.append("\n⏱️  TIMING:\n");
        sb.append(String.format("  Start Time     : %s (%d ns)\n", startTime, startNanos));
        sb.append(String.format("  End Time       : %s (%d ns)\n", endTime, endNanos));
        sb.append(String.format("  Duration       : %d ms (%.3f seconds)\n",
                TimeUnit.NANOSECONDS.toMillis(durationNanos),
                durationNanos / 1_000_000_000.0));

        // ========================================
        // 3. STATUS
        // ========================================
        StatusData status = span.getStatus();
        sb.append("\n📊 STATUS:\n");
        sb.append(String.format("  Status Code    : %s\n", status.getStatusCode()));
        sb.append(String.format("  Description    : %s\n",
                status.getDescription() != null ? status.getDescription() : "N/A"));

        String statusEmoji = switch (status.getStatusCode()) {
            case OK -> "✅";
            case ERROR -> "❌";
            case UNSET -> "⚪";
        };
        sb.append(String.format("  Visual         : %s %s\n", statusEmoji, status.getStatusCode()));

        // ========================================
        // 4. ATTRIBUTES
        // ========================================
        Attributes attributes = span.getAttributes();
        sb.append("\n🏷️  ATTRIBUTES (").append(attributes.size()).append("):\n");

        if (attributes.isEmpty()) {
            sb.append("  (no attributes)\n");
        } else {
            attributes.forEach((key, value) -> {
                sb.append(String.format("  %-30s : %s (type: %s)\n",
                        key.getKey(),
                        formatValue(value),
                        key.getType()));
            });
        }

        // ========================================
        // 5. RESOURCE ATTRIBUTES
        // ========================================
        Attributes resourceAttributes = span.getResource().getAttributes();
        sb.append("\n🌍 RESOURCE ATTRIBUTES (").append(resourceAttributes.size()).append("):\n");

        if (resourceAttributes.isEmpty()) {
            sb.append("  (no resource attributes)\n");
        } else {
            resourceAttributes.forEach((key, value) -> {
                sb.append(String.format("  %-30s : %s\n", key.getKey(), formatValue(value)));
            });
        }

        // ========================================
        // 6. EVENTS
        // ========================================
        sb.append("\n📅 EVENTS (").append(span.getEvents().size()).append("):\n");

        if (span.getEvents().isEmpty()) {
            sb.append("  (no events)\n");
        } else {
            int eventIndex = 1;
            for (EventData event : span.getEvents()) {
                Instant eventTime = Instant.ofEpochSecond(0, event.getEpochNanos());
                long eventOffsetMs = TimeUnit.NANOSECONDS.toMillis(
                        event.getEpochNanos() - startNanos
                );

                sb.append(String.format("  [%d] %s (at +%dms)\n",
                        eventIndex++,
                        event.getName(),
                        eventOffsetMs));
                sb.append(String.format("      Time: %s\n", eventTime));
                sb.append(String.format("      Total Attributes: %d\n",
                        event.getAttributes().size()));

                if (!event.getAttributes().isEmpty()) {
                    event.getAttributes().forEach((key, value) -> {
                        sb.append(String.format("        - %-25s : %s\n",
                                key.getKey(), formatValue(value)));
                    });
                }
            }
        }

        // ========================================
        // 7. LINKS
        // ========================================
        sb.append("\n🔗 LINKS (").append(span.getLinks().size()).append("):\n");

        if (span.getLinks().isEmpty()) {
            sb.append("  (no links)\n");
        } else {
            int linkIndex = 1;
            for (LinkData link : span.getLinks()) {
                sb.append(String.format("  [%d] Link to Trace: %s\n",
                        linkIndex++,
                        link.getSpanContext().getTraceId()));
                sb.append(String.format("      Span ID: %s\n",
                        link.getSpanContext().getSpanId()));
                sb.append(String.format("      Attributes: %d\n",
                        link.getAttributes().size()));

                if (!link.getAttributes().isEmpty()) {
                    link.getAttributes().forEach((key, value) -> {
                        sb.append(String.format("        - %-25s : %s\n",
                                key.getKey(), formatValue(value)));
                    });
                }
            }
        }

        // ========================================
        // 8. INSTRUMENTATION SCOPE
        // ========================================
        sb.append("\n🔧 INSTRUMENTATION SCOPE:\n");
        sb.append(String.format("  Name           : %s\n",
                span.getInstrumentationScopeInfo().getName()));
        sb.append(String.format("  Version        : %s\n",
                span.getInstrumentationScopeInfo().getVersion() != null ?
                        span.getInstrumentationScopeInfo().getVersion() : "N/A"));
        sb.append(String.format("  Schema URL     : %s\n",
                span.getInstrumentationScopeInfo().getSchemaUrl() != null ?
                        span.getInstrumentationScopeInfo().getSchemaUrl() : "N/A"));

        // ========================================
        // 9. SPAN CONTEXT FLAGS
        // ========================================
        sb.append("\n🚩 SPAN CONTEXT:\n");
        sb.append(String.format("  Trace Flags    : %s\n",
                span.getSpanContext().getTraceFlags()));
        sb.append(String.format("  Trace State    : %s\n",
                span.getSpanContext().getTraceState()));
        sb.append(String.format("  Is Remote      : %s\n",
                span.getSpanContext().isRemote()));
        sb.append(String.format("  Is Valid       : %s\n",
                span.getSpanContext().isValid()));
        sb.append(String.format("  Is Sampled     : %s\n",
                span.getSpanContext().isSampled()));

        // ========================================
        // 10. SUMMARY
        // ========================================
        sb.append("\n📈 SUMMARY:\n");
        sb.append(String.format("  Total Attributes       : %d\n", attributes.size()));
        sb.append(String.format("  Total Resource Attrs   : %d\n", resourceAttributes.size()));
        sb.append(String.format("  Total Events           : %d\n", span.getEvents().size()));
        sb.append(String.format("  Total Links            : %d\n", span.getLinks().size()));
        sb.append(String.format("  Has Ended              : %s\n", span.hasEnded()));

        sb.append(LINE).append("\n");

        log.info(sb.toString());
    }

    /**
     * Format value dựa trên type
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            return "\"" + value + "\"";
        }

        if (value instanceof Long || value instanceof Integer) {
            return value.toString();
        }

        if (value instanceof Double || value instanceof Float) {
            return String.format("%.2f", value);
        }

        if (value instanceof Boolean) {
            return value.toString();
        }

        // Array handling
        if (value.getClass().isArray()) {
            if (value instanceof String[]) {
                return "[" + String.join(", ", (String[]) value) + "]";
            }
            if (value instanceof long[]) {
                long[] arr = (long[]) value;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(arr[i]);
                }
                sb.append("]");
                return sb.toString();
            }
            if (value instanceof boolean[]) {
                boolean[] arr = (boolean[]) value;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(arr[i]);
                }
                sb.append("]");
                return sb.toString();
            }
            if (value instanceof double[]) {
                double[] arr = (double[]) value;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(String.format("%.2f", arr[i]));
                }
                sb.append("]");
                return sb.toString();
            }
        }

        return value.toString();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        if (!isShutdown) {
            log.info("Shutting down DetailedLoggingSpanExporter");
            isShutdown = true;
        }
        return CompletableResultCode.ofSuccess();
    }
}
