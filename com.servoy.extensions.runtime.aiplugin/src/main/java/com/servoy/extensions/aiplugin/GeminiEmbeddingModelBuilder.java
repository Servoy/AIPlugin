package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;

import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel.GoogleAiEmbeddingModelBuilder;

/**
 * GeminiEmbeddingModelBuilder is a builder for configuring and creating Gemini
 * embedding models. Allows setting API key and model name for the Gemini
 * embedding model.
 */
@ServoyDocumented
public class GeminiEmbeddingModelBuilder {

	/**
	 * The ai provider plugin.
	 */
	private final AIProvider provider;

	/**
	 * The builder for the Gemini embedding model.
	 */
	private final GoogleAiEmbeddingModelBuilder builder = GoogleAiEmbeddingModel.builder();

	/**
	 * Constructs a GeminiEmbeddingModelBuilder with the given plugin access.
	 *
	 * @param provider ai provider plugin.
	 */
	GeminiEmbeddingModelBuilder(AIProvider provider) {
		this.provider = provider;
	}

	/**
	 * Sets the Gemini API key for the embedding model.
	 *
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiEmbeddingModelBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}

	/**
	 * Sets the Gemini model name for the embedding model.
	 *
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiEmbeddingModelBuilder modelName(String modelName) {
		builder.modelName(modelName);
		return this;
	}

	/**
	 * Builds and returns an EmbeddingModel configured with the specified Gemini
	 * embedding model settings.
	 *
	 * @return A configured EmbeddingModel instance.
	 */
	@JSFunction
	public EmbeddingModel build() {
		return new EmbeddingModel(builder.build(), provider);
	}
}