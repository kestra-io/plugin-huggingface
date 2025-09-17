package io.kestra.plugin.huggingface;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.Test;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

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
