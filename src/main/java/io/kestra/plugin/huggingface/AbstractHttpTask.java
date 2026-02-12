package io.kestra.plugin.huggingface;

import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.http.client.configurations.TimeoutConfiguration;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractHttpTask extends Task {

    @Schema(
        title = "HTTP client options",
        description = "Optional overrides for timeouts, charset, and response size. Defaults: readTimeout 10s, readIdleTimeout 5m, connectionPoolIdleTimeout 0s, maxContentLength 10MB, charset UTF-8."
    )
    @PluginProperty(dynamic = true)
    protected RequestOptions options;

    @Getter
    @Builder
    public static class RequestOptions {
        @Schema(title = "Connect timeout", description = "Time allowed to establish the connection before failing; uses client default when unset")
        private final Property<Duration> connectTimeout;

        @Schema(title = "Read timeout", description = "Maximum time to read data before failing; default 10 seconds")
        @Builder.Default
        private final Property<Duration> readTimeout = Property.ofValue(Duration.ofSeconds(10));

        @Schema(title = "Read idle timeout", description = "Idle time allowed on a read connection before closing; default 5 minutes")
        @Builder.Default
        private final Property<Duration> readIdleTimeout = Property.ofValue(Duration.of(5, ChronoUnit.MINUTES));

        @Schema(title = "Pool idle timeout", description = "Idle lifetime for pooled connections; default 0 seconds")
        @Builder.Default
        private final Property<Duration> connectionPoolIdleTimeout = Property.ofValue(Duration.ofSeconds(0));

        @Schema(title = "Max content length", description = "Maximum response size in bytes; default 10 MB")
        @Builder.Default
        private final Property<Integer> maxContentLength = Property.ofValue(1024 * 1024 * 10);

        @Schema(title = "Default charset", description = "Charset used for requests when unspecified; default UTF-8")
        @Builder.Default
        private final Property<Charset> defaultCharset = Property.ofValue(StandardCharsets.UTF_8);
    }

    public HttpConfiguration httpClientConfigurationWithOptions() {
        HttpConfiguration.HttpConfigurationBuilder httpConfigurationBuilder = HttpConfiguration.builder();
        if (this.options != null) {
            return httpConfigurationBuilder
                .timeout(TimeoutConfiguration.builder()
                    .connectTimeout(this.options.getConnectTimeout())
                    .readIdleTimeout(this.options.getReadIdleTimeout())
                    .build())
                .defaultCharset(this.options.getDefaultCharset()).build();
        }
        return httpConfigurationBuilder.build();
    }
}
