package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder;

/**
 * OpenAiEmbeddingBuilder is a builder for configuring and creating OpenAI embedding clients.
 * Allows setting API key and model name for the OpenAI embedding model.
 */
@ServoyDocumented(scriptingName = "OpenAiEmbeddingBuilder")
public class OpenAiEmbeddingBuilder {

	/**
	 * The  ai provider plugin.
	 */
	private final AIProvider provider;
	/**
	 * The builder for the OpenAI embedding model.
	 */
	private final OpenAiEmbeddingModelBuilder builder;

	/**
	 * Constructs an OpenAiEmbeddingBuilder with the given plugin access.
	 * @param provider ai provider plugin.
	 */
	public OpenAiEmbeddingBuilder(AIProvider provider) {
		this.provider = provider;
		builder = OpenAiEmbeddingModel.builder();
	}

	/**
	 * Sets the OpenAI API key for the embedding model.
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}
	
	/**
	 * Sets the OpenAI model name for the embedding model.
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingBuilder modelName(String modelName) {
		builder.modelName(modelName);
		return this;
	}
	
	/**
	 * Builds and returns an EmbeddingClient configured with the specified OpenAI embedding model settings.
	 * @return A configured EmbeddingClient instance.
	 */
	@JSFunction
	public EmbeddingClient build() {
		OpenAiEmbeddingModel model = builder.build();
		return new EmbeddingClient(model, provider);
	}
}