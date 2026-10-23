package io.kestra.plugin.huggingface;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.validations.ModelValidator;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.YamlParser;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This test will only test the main task, this allow you to send any input
 * parameters to your task and test the returning behaviour easily.
 */
@KestraTest
@WireMockTest
class InferenceTest {

    private static final String apiKey = System.getenv("HUGGINGFACE_API_KEY");;

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private ModelValidator modelValidator;

    @Test
    void testHuggingFaceInference_fillMask(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String endpoint = "http://localhost:" + wmRuntimeInfo.getHttpPort() + "/models";
        RunContext runContext = runContextFactory.of(Map.of());

        Inference task = Inference.builder()
            .apiKey(Property.ofValue("mock"))
            .model(Property.ofValue("google-bert/bert-base-uncased"))
            .inputs(Property.ofValue("I love to eat [MASK]."))
            .endpoint(Property.ofValue(endpoint))
            .build();

        Inference.Output runOutput = task.run(runContext);

        List<Map<String, Object>> resultList = (List<Map<String, Object>>) runOutput.getOutput();

        assertThat(resultList, notNullValue());
        assertThat(resultList.size(), is(5));

        Map<String, Object> firstResult = resultList.getFirst();
        assertThat(firstResult.get("score"), is(0.1976442039012909d));
        assertThat(firstResult.get("token"), is(2009));
        assertThat(firstResult.get("token_str"), is("it"));
        assertThat(firstResult.get("sequence"), is("i love to eat it."));
    }

    @Test
    void testHuggingFaceInference_textClassification(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String endpoint = "http://localhost:" + wmRuntimeInfo.getHttpPort() + "/models";
        RunContext runContext = runContextFactory.of(Map.of());

        Inference task = Inference.builder()
            .apiKey(Property.ofValue("mock"))
            .model(Property.ofValue("cardiffnlp/twitter-roberta-base-sentiment-latest"))
            .inputs(Property.ofValue("I am hungry"))
            .endpoint(Property.ofValue(endpoint))
            .build();

        Inference.Output runOutput = task.run(runContext);

        assertThat(runOutput.getOutput(), notNullValue());
    }

    @SuppressWarnings("deprecation")
    @Test
    void flowSettingDeprecatedOptionsStillValidatesAndRuns(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        var flow = YamlParser.parse(
            """
                id: huggingface_deprecated_options
                namespace: io.kestra.tests
                tasks:
                  - id: inference
                    type: io.kestra.plugin.huggingface.Inference
                    apiKey: mock
                    model: cardiffnlp/twitter-roberta-base-sentiment-latest
                    inputs: I am hungry
                    endpoint: "http://localhost:%d/models"
                    options:
                      readTimeout: PT30S
                      connectionPoolIdleTimeout: PT10S
                      maxContentLength: 1024
                """.formatted(wmRuntimeInfo.getHttpPort()),
            Flow.class
        );

        assertThat(modelValidator.isValid(flow).isEmpty(), is(true));

        var runContext = runContextFactory.of(Map.of());
        var task = (Inference) flow.getTasks().getFirst();
        var options = task.getOptions();
        assertThat(runContext.render(options.getReadTimeout()).as(Duration.class).orElseThrow(), is(Duration.ofSeconds(30)));
        assertThat(runContext.render(options.getConnectionPoolIdleTimeout()).as(Duration.class).orElseThrow(), is(Duration.ofSeconds(10)));
        assertThat(runContext.render(options.getMaxContentLength()).as(Integer.class).orElseThrow(), is(1024));

        var runOutput = task.run(runContext);

        assertThat(runOutput.getOutput(), notNullValue());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "HUGGINGFACE_API_KEY", matches = ".*")
    void testHuggingFaceInferenceWithRealApi() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());

        Inference task = Inference.builder()
            .apiKey(Property.ofValue(apiKey))
            .model(Property.ofValue("cardiffnlp/twitter-roberta-base-sentiment-latest"))
            .inputs(Property.ofValue("I am hungry"))
            .build();

        Inference.Output runOutput = task.run(runContext);

        assertThat(runOutput.getOutput(), notNullValue());
    }
}
