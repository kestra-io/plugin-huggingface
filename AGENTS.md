# Kestra HuggingFace Plugin

## What

- Provides plugin components under `io.kestra.plugin.huggingface`.
- Includes classes such as `Inference`.

## Why

- This plugin integrates Kestra with Huggingface.
- It provides tasks that call Hugging Face Inference APIs.

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
