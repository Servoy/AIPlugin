package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * OpenAiEmbeddingModelBuilder is a builder for configuring and creating OpenAI
 * embedding models. Allows setting API key and model name for the OpenAI
 * embedding model.
 */
@ServoyDocumented
public class OpenAiEmbeddingModelBuilder {

	/**
	 * The ai provider plugin.
	 */
	private final AIProvider provider;

	/**
	 * The builder for the OpenAI embedding model.
	 */
	private final OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder();

	/**
	 * Constructs an OpenAiEmbeddingModelBuilder with the given plugin access.
	 *
	 * @param provider ai provider plugin.
	 */
	OpenAiEmbeddingModelBuilder(AIProvider provider) {
		this.provider = provider;
	}

	/**
	 * Sets the OpenAI API key for the embedding model.
	 *
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingModelBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}

	/**
	 * Sets the OpenAI model name for the embedding model.
	 *
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingModelBuilder modelName(String modelName) {
		builder.modelName(modelName);
		return this;
	}

	/**
	 * Builds and returns an EmbeddingClient configured with the specified OpenAI
	 * embedding model settings.
	 *
	 * @return A configured EmbeddingClient instance.
	 */
	@JSFunction
	public EmbeddingModel build() {
		return new EmbeddingModel(builder.build(), provider);
	}
}