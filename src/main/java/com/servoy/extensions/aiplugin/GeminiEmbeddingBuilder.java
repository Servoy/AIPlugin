package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel.GoogleAiEmbeddingModelBuilder;

/**
 * GeminiEmbeddingBuilder is a builder for configuring and creating Gemini embedding clients.
 * Allows setting API key and model name for the Gemini embedding model.
 */
@ServoyDocumented(scriptingName = "GeminiEmbeddingBuilder")
public class GeminiEmbeddingBuilder {

	/**
	 * The builder for the Gemini embedding model.
	 */
	private final GoogleAiEmbeddingModelBuilder builder;
	/**
	 * The ai provider plugin.
	 */
	private final AIProvider provider;

	/**
	 * Constructs a GeminiEmbeddingBuilder with the given plugin access.
	 * @param provider  ai provider plugin.
	 */
	public GeminiEmbeddingBuilder(AIProvider provider) {
		this.provider = provider;
		builder = GoogleAiEmbeddingModel.builder();
	}
	
	/**
	 * Sets the Gemini API key for the embedding model.
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiEmbeddingBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}
	
	/**
	 * Sets the Gemini model name for the embedding model.
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiEmbeddingBuilder modelName(String modelName) {
		builder.modelName(modelName);
		return this;
	}
	
	/**
	 * Builds and returns an EmbeddingClient configured with the specified Gemini embedding model settings.
	 * @return A configured EmbeddingClient instance.
	 */
	@JSFunction
	public EmbeddingClient build() {
		GoogleAiEmbeddingModel model = builder.build();
		return new EmbeddingClient(model, provider);
	}

}