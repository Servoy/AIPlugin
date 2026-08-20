package com.servoy.extensions.aiplugin.embedding;

import com.servoy.extensions.aiplugin.AIProvider;

import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;

class GeminiEmbeddingModelDelegate {

	static EmbeddingModel build(String apiKey, String modelName, AIProvider provider) {
		GoogleAiEmbeddingModel.GoogleAiEmbeddingModelBuilder builder = GoogleAiEmbeddingModel.builder();
		if (apiKey != null) builder.apiKey(apiKey);
		if (modelName != null) builder.modelName(modelName);
		return new EmbeddingModel(builder.build(), provider);
	}
}
