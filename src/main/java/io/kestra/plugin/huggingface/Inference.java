package io.kestra.plugin.huggingface;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Invoke Hugging Face Inference API",
    description = """
        Sends rendered inputs to a Hugging Face model repository using the Serverless Inference API. Requires a Bearer `apiKey`; defaults to https://api-inference.huggingface.co/models. 
        Sets `x-use-cache` to true by default (disable for nondeterministic models) and `x-wait-for-model` to false (cold models may return HTTP 503 unless enabled).
        """
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Use inference for text classification",
            full = true,
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
            full = true,
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
public class Inference extends AbstractHttpTask implements RunnableTask<Inference.Output> {
    public static final String HUGGINGFACE_BASE_ENDPOINT = "https://api-inference.huggingface.co/models";
    public static final String WAIT_HEADER = "x-wait-for-model";
    public static final String CACHE_HEADER = "x-use-cache";

    @Schema(title = "API Key", description = "Hugging Face token sent as Bearer auth (ex: hf_********)")
    @NotNull
    private Property<String> apiKey;

    @Schema(title = "Model", description = "Model repository path appended to the endpoint (ex: cardiffnlp/twitter-roberta-base-sentiment-latest)")
    @NotNull
    private Property<String> model;

    @Schema(title = "Inputs", description = "Rendered payload sent as `inputs` (text, JSON string, or base64 content expected by the model)")
    @NotNull
    private Property<String> inputs;

    @Schema(title = "Parameters", description = "Optional rendered parameters map added when not empty; content depends on the model")
    private Property<Map<String, Object>> parameters;

    @Schema(title = "API endpoint", description = "Inference base URL; defaults to https://api-inference.huggingface.co/models")
    @Builder.Default
    private Property<String> endpoint = Property.ofValue(HUGGINGFACE_BASE_ENDPOINT);

    @Schema(
        title = "Use cache",
        description = """
            Enables the inference cache via `x-use-cache`; default true for deterministic models. Disable for nondeterministic models to force fresh inference.
            """
    )
    @Builder.Default
    private Property<Boolean> useCache = Property.ofValue(true);

    @Schema(
        title = "Wait for model",
        description = """
            Adds `x-wait-for-model` header; default false. When false, cold models return HTTP 503 instead of waiting to load.
            """
    )
    @Builder.Default
    private Property<Boolean> waitForModel = Property.ofValue(false);

    @Override
    public Inference.Output run(RunContext runContext) throws Exception {
        final String renderedEndpoint = runContext.render(this.endpoint).as(String.class).orElseThrow();
        final String renderedModels = runContext.render(this.model).as(String.class).orElseThrow();
        final String renderedApiKey = runContext.render(this.apiKey).as(String.class).orElseThrow();
        final String url = String.join("/", renderedEndpoint, renderedModels);

        try (HttpClient client = new HttpClient(runContext,super.httpClientConfigurationWithOptions())) {
            var payload = new HashMap<String, Object>();
            var inputsRendered = runContext.render(this.inputs).as(String.class).orElseThrow();
            var parametersRendered = runContext.render(this.parameters).asMap(String.class, Object.class);

            payload.put("inputs", inputsRendered);

            if(!parametersRendered.isEmpty()) {
                payload.put("parameters", parametersRendered);
            }

            HttpRequest request = HttpRequest.builder()
                .addHeader("Authorization", "Bearer " + renderedApiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader(CACHE_HEADER, runContext.render(this.useCache).as(Boolean.class).orElseThrow().toString())
                .addHeader(WAIT_HEADER, runContext.render(this.waitForModel).as(Boolean.class).orElseThrow().toString())
                .uri(URI.create(url))
                .method("POST")
                .body(HttpRequest.JsonRequestBody.builder()
                    .content(payload)
                    .build())
                .build();

            runContext.logger().debug("Use Huggingface Inference API with input: {}", payload);

            HttpResponse<Object> response = client.request(request, Object.class);

            runContext.logger().debug("Response: {}", response.getBody());

            return Output.builder()
                .output(response.getBody())
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
            title = "Output returned by the Hugging Face API",
            description = "Raw response body from the inference request"
        )
        private final Object output;
    }
}
