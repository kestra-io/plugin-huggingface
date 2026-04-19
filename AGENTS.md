# Kestra HuggingFace Plugin

## What

- Provides plugin components under `io.kestra.plugin.huggingface`.
- Includes classes such as `Inference`.

## Why

- What user problem does this solve? Teams need to call Hugging Face Inference APIs from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Huggingface steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Huggingface.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `huggingface`

### Key Plugin Classes

- `io.kestra.plugin.huggingface.Inference`

### Project Structure

```
plugin-huggingface/
├── src/main/java/io/kestra/plugin/huggingface/
├── src/test/java/io/kestra/plugin/huggingface/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
