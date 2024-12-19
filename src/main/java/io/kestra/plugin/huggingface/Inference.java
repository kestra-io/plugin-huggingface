package io.kestra.plugin.huggingface;

import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.serializers.JacksonMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.client.netty.DefaultHttpClientBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import org.slf4j.Logger;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;
import static io.micronaut.http.HttpHeaders.CONTENT_TYPE;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Short description for this task",
    description = "Full description of this task"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Use inference for text classification",
            code = """
                    id: huggingface_inference_text
                    namespace: company.team

                    tasks:
                    - id: huggingface_inference
                        type: io.kestra.plugin.huggingface.Inference
                        model: cardiffnlp/twitter-roberta-base-sentiment-latest
                        apiKey: "{{ secret('HUGGINGFACE_API_KEY') }}"
                        inputs: "I want a refund"
                """
        ),
        @io.kestra.core.models.annotations.Example(
            title = "Use inference for image classification.",
            code = """
                    id: huggingface_inference
                    namespace: company.team

                    tasks:
                    - id: huggingface_inference_image
                        type: io.kestra.plugin.huggingface.Inference
                        model: google/vit-base-patch16-224
                        apiKey: "{{ secret('HUGGINGFACE_API_KEY') }}"
                        inputs: "{{ read('my-base64-image.txt') }}"
                        parameters:
                            function_to_apply: sigmoid,
                            top_k: 3
                        waitForModel: true
                        useCache: false
                """
        )
    }
)
public class Inference extends Task implements RunnableTask<Inference.Output> {
    public static final String HUGGINGFACE_BASE_ENDPOINT = "https://api-inference.huggingface.co/models";
    public static final String WAIT_HEADER = "x-wait-for-model";
    public static final String CACHE_HEADER = "x-use-cache";

    @Schema(title = "API Key", description = "Huggingface API key (ex: hf_********)")
    @NotNull
    private Property<String> apiKey;

    @Schema(title = "Model", description = "Model used for the Inference api (ex: cardiffnlp/twitter-roberta-base-sentiment-latest, google/gemma-2-2b-it)")
    @NotNull
    private Property<String> model;

    @Schema(title = "Inputs", description = "Inputs required for the specific model")
    @NotNull
    private Property<String> inputs;

    @Schema(title = "Parameters", description = "Map of optional parameters depending on the model")
    private Property<Map<String, Object>> parameters;

    @Schema(title = "API endpoint", description = "Default value of the Huggingface API is https://api-inference.huggingface.co/models")
    @Builder.Default
    private Property<String> endpoint = Property.of(HUGGINGFACE_BASE_ENDPOINT);

    @Schema(
        title = "Use cache",
        description = """
            There is a cache layer on the inference API to speed up requests when the inputs are exactly the same.
            Many models, such as classifiers and embedding models, can use those results as is if they are deterministic, meaning the results will be the same.
            However, if you use a nondeterministic model, you can disable the cache mechanism from being used, resulting in a real new query.
            """
    )
    @Builder.Default
    private Property<Boolean> useCache = Property.of(true);

    @Schema(
        title = "Wait for model",
        description = """
            When a model is warm, it is ready to be used and you will get a response relatively quickly.
            However, some models are cold and need to be loaded before they can be used. In that case, you will get a 503 error.
            """
    )
    @Builder.Default
    private Property<Boolean> waitForModel = Property.of(false);

    @Schema(
        title = "Options",
        description = "The options to set to customize the HTTP client"
    )
    @PluginProperty(dynamic = true)
    protected RequestOptions options;


    @Override
    public Inference.Output run(RunContext runContext) throws Exception {
        final String renderedEndpoint = runContext.render(this.endpoint).as(String.class).orElseThrow();
        final String renderedModels = runContext.render(this.model).as(String.class).orElseThrow();
        final String renderedApiKey = runContext.render(this.apiKey).as(String.class).orElseThrow();
        final String url = String.join("/", renderedEndpoint, renderedModels);

        DefaultHttpClientBuilder clientBuilder = DefaultHttpClient.builder()
            .configuration(RequestOptions.httpClientConfigurationWithOptions(runContext, this.options))
            .uri(URI.create(url));

        try (DefaultHttpClient client = clientBuilder.build()) {
            var payload = new HashMap<String, Object>();
            var inputsRendered = runContext.render(this.inputs).as(String.class).orElseThrow();
            var parametersRendered = runContext.render(this.parameters).asMap(String.class, Object.class);

            payload.put("inputs", inputsRendered);
            if(!parametersRendered.isEmpty()) {
                payload.put("parameters", parametersRendered);
            }

            runContext.logger().debug("Use Huggingface Inference API with input: {}", payload);

            Object output = client.toBlocking().retrieve(HttpRequest.POST(url, payload)
                .body(payload)
                .headers(Map.of(
                    AUTHORIZATION, String.join(" ", "Bearer", renderedApiKey),
                    CONTENT_TYPE, "application/json",
                    CACHE_HEADER, runContext.render(this.useCache).as(Boolean.class).orElseThrow().toString(),
                    WAIT_HEADER, runContext.render(this.waitForModel).as(Boolean.class).orElseThrow().toString()
                ))
            );

            return Output.builder()
                .output(output)
                .build();
        }

    }

    /**
     * Input or Output can be nested as you need
     */
    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Output returned by the Huggingface API"
        )
        private final Object output;
    }
}
