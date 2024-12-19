package io.kestra.plugin.huggingface;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClientConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class RequestOptions {
    @Schema(title = "The time allowed to establish a connection to the server before failing.")
    private final Property<Duration> connectTimeout;

    @Schema(title = "The maximum time allowed for reading data from the server before failing.")
    @Builder.Default
    private final Property<Duration> readTimeout = Property.of(Duration.ofSeconds(HttpClientConfiguration.DEFAULT_READ_TIMEOUT_SECONDS));

    @Schema(title = "The time allowed for a read connection to remain idle before closing it.")
    @Builder.Default
    private final Property<Duration> readIdleTimeout = Property.of(Duration.of(HttpClientConfiguration.DEFAULT_READ_IDLE_TIMEOUT_MINUTES, ChronoUnit.MINUTES));

    @Schema(title = "The time an idle connection can remain in the client's connection pool before being closed.")
    @Builder.Default
    private final Property<Duration> connectionPoolIdleTimeout = Property.of(Duration.ofSeconds(HttpClientConfiguration.DEFAULT_CONNECTION_POOL_IDLE_TIMEOUT_SECONDS));

    @Schema(title = "The maximum content length of the response.")
    @Builder.Default
    private final Property<Integer> maxContentLength = Property.of(HttpClientConfiguration.DEFAULT_MAX_CONTENT_LENGTH);

    @Schema(title = "The default charset for the request.")
    @Builder.Default
    private final Property<Charset> defaultCharset = Property.of(StandardCharsets.UTF_8);

    public static DefaultHttpClientConfiguration httpClientConfigurationWithOptions(RunContext runContext, RequestOptions options) throws IllegalVariableEvaluationException {
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();

        if (options != null) {
            runContext.render(options.getConnectTimeout()).as(Duration.class)
                .ifPresent(configuration::setConnectTimeout);

            runContext.render(options.getReadTimeout()).as(Duration.class)
                .ifPresent(configuration::setReadTimeout);

            runContext.render(options.getReadIdleTimeout()).as(Duration.class)
                .ifPresent(configuration::setReadIdleTimeout);

            runContext.render(options.getConnectionPoolIdleTimeout()).as(Duration.class)
                .ifPresent(configuration::setConnectionPoolIdleTimeout);

            runContext.render(options.getMaxContentLength()).as(Integer.class)
                .ifPresent(configuration::setMaxContentLength);

            runContext.render(options.getDefaultCharset()).as(Charset.class)
                .ifPresent(configuration::setDefaultCharset);

        }

        return configuration;
    }
}
