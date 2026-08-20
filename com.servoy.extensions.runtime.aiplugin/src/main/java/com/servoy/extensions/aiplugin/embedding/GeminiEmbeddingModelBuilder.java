package com.servoy.extensions.aiplugin.embedding;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.AIProvider;
import com.servoy.extensions.aiplugin.ProviderLoader;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;

/**
 * GeminiEmbeddingModelBuilder is a builder for configuring and creating Gemini
 * embedding models. Allows setting API key and model name for the Gemini
 * embedding model.
 */
@ServoyDocumented
public class GeminiEmbeddingModelBuilder implements IJavaScriptType {

	private final AIProvider provider;
	private String apiKey;
	private String modelName;

	/**
	 * Constructs a GeminiEmbeddingModelBuilder with the given plugin access.
	 *
	 * @param provider ai provider plugin.
	 */
	public GeminiEmbeddingModelBuilder(AIProvider provider) {
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
		this.apiKey = key;
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
		this.modelName = modelName;
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
		ProviderLoader.ensureAvailable(
			"dev.langchain4j.model.googleai.GoogleAiEmbeddingModel",
			"Gemini",
			"gemini");
		return GeminiEmbeddingModelDelegate.build(apiKey, modelName, provider);
	}
}
