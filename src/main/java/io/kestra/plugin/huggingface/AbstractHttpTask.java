package io.kestra.plugin.huggingface;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.http.client.configurations.TimeoutConfiguration;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractHttpTask extends Task {

    @Schema(
        title = "Options",
        description = "The options to set to customize the HTTP client"
    )
    @PluginProperty(dynamic = true, group = "advanced")
    protected RequestOptions options;

    @Getter
    @Builder
    public static class RequestOptions {
        @Schema(title = "The time allowed to establish a connection to the server before failing")
        @PluginProperty(group = "execution")
        private final Property<Duration> connectTimeout;

        @Deprecated
        @Schema(
            title = "The maximum time allowed for reading data from the server before failing",
            description = """
                Deprecated, this option has no effect: Kestra's HTTP client has no separate read timeout. Use `readIdleTimeout` instead.
                """
        )
        @Builder.Default
        @PluginProperty(group = "deprecated")
        private final Property<Duration> readTimeout = Property.ofValue(Duration.ofSeconds(10));

        @Schema(title = "The time allowed for a read connection to remain idle before closing it")
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> readIdleTimeout = Property.ofValue(Duration.of(5, ChronoUnit.MINUTES));

        @Deprecated
        @Schema(
            title = "The time an idle connection can remain in the client's connection pool before being closed",
            description = """
                Deprecated, this option has no effect: Kestra's HTTP client does not support a connection pool idle timeout.
                """
        )
        @Builder.Default
        @PluginProperty(group = "deprecated")
        private final Property<Duration> connectionPoolIdleTimeout = Property.ofValue(Duration.ofSeconds(0));

        @Deprecated
        @Schema(
            title = "The maximum content length of the response",
            description = """
                Deprecated, this option has no effect: Kestra's HTTP client does not limit the response size.
                """
        )
        @Builder.Default
        @PluginProperty(group = "deprecated")
        private final Property<Integer> maxContentLength = Property.ofValue(1024 * 1024 * 10);

        @Schema(title = "The default charset for the request")
        @Builder.Default
        @PluginProperty(group = "advanced")
        private final Property<Charset> defaultCharset = Property.ofValue(StandardCharsets.UTF_8);
    }

    public HttpConfiguration httpClientConfigurationWithOptions() {
        HttpConfiguration.HttpConfigurationBuilder httpConfigurationBuilder = HttpConfiguration.builder();
        if (this.options != null) {
            return httpConfigurationBuilder
                .timeout(
                    TimeoutConfiguration.builder()
                        .connectTimeout(this.options.getConnectTimeout())
                        .readIdleTimeout(this.options.getReadIdleTimeout())
                        .build()
                )
                .defaultCharset(this.options.getDefaultCharset()).build();
        }
        return httpConfigurationBuilder.build();
    }
}
