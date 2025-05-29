package io.kestra.plugin.huggingface;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.serializers.JacksonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

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

    private static final String apiKey = "";

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

        assertThat(runOutput.getOutput(), notNullValue());
        ArrayNode resultArray = ((ArrayNode) JacksonMapper.ofJson().readTree((String) runOutput.getOutput()));
        assertThat(resultArray.size(), is(5));
        assertThat(resultArray.get(0).get("score").asDouble(), is(0.1976442039012909d));
        assertThat(resultArray.get(0).get("token").asInt(), is(2009));
        assertThat(resultArray.get(0).get("token_str").asText(), is("it"));
        assertThat(resultArray.get(0).get("sequence").asText(), is("i love to eat it."));
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
    @Disabled("Integration test with Huggingface API - requires an API key")
    void testHuggingFaceInferenceWithRealApi() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());

        Inference task = Inference.builder()
            .apiKey(Property.ofValue(this.apiKey))
            .model(Property.ofValue("cardiffnlp/twitter-roberta-base-sentiment-latest"))
            .inputs(Property.ofValue("I am hungry"))
            .build();

        Inference.Output runOutput = task.run(runContext);

        assertThat(runOutput.getOutput(), notNullValue());
    }
}
