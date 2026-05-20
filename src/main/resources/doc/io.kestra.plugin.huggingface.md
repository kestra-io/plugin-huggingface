# How to use the Hugging Face plugin

Call any model hosted on the Hugging Face Inference API from Kestra flows.

## Authentication

Set `token` to your Hugging Face API token. Store it in a [secret](https://kestra.io/docs/concepts/secret).

## Tasks

`Inference` sends a request to a Hugging Face hosted model endpoint and returns the prediction. Set `modelId` to the model's repository path (e.g., `facebook/bart-large-cnn`) and `inputs` to the payload appropriate for that model type — text for NLP tasks, base64-encoded data for vision models. The response shape varies by model and task type; check the model's API documentation on the Hugging Face Hub for the expected input and output format.
