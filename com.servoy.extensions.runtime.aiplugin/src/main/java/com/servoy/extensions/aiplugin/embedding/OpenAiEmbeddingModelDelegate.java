package com.servoy.extensions.aiplugin.embedding;

import com.servoy.extensions.aiplugin.AIProvider;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

class OpenAiEmbeddingModelDelegate {

	static EmbeddingModel build(String apiKey, String modelName, String baseUrl, AIProvider provider) {
		OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder();
		if (apiKey != null) builder.apiKey(apiKey);
		if (modelName != null) builder.modelName(modelName);
		if (baseUrl != null) builder.baseUrl(baseUrl);
		return new EmbeddingModel(builder.build(), provider);
	}
}
