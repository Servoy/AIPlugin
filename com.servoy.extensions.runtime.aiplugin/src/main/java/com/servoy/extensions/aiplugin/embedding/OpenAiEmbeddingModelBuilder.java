package com.servoy.extensions.aiplugin.embedding;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.AIProvider;
import com.servoy.extensions.aiplugin.ProviderLoader;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;

/**
 * OpenAiEmbeddingModelBuilder is a builder for configuring and creating OpenAI
 * embedding models. Allows setting API key and model name for the OpenAI
 * embedding model.
 */
@ServoyDocumented
public class OpenAiEmbeddingModelBuilder implements IJavaScriptType {

	private final AIProvider provider;
	private String apiKey;
	private String modelName;
	private String baseUrl;

	/**
	 * Constructs an OpenAiEmbeddingModelBuilder with the given plugin access.
	 *
	 * @param provider ai provider plugin.
	 */
	public OpenAiEmbeddingModelBuilder(AIProvider provider) {
		this.provider = provider;
	}

	/**
	 * Sets base url for an api like IONOS that is compatible with OpenAI API.
	 *
	 * @param url The base URL.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingModelBuilder baseUrl(String url) {
		this.baseUrl = url;
		return this;
	}

	/**
	 * Sets the OpenAI API key for the embedding model.
	 *
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingModelBuilder apiKey(String key) {
		this.apiKey = key;
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
		this.modelName = modelName;
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
		ProviderLoader.ensureAvailable(
			"dev.langchain4j.model.openai.OpenAiEmbeddingModel",
			"OpenAI",
			"openai");
		return OpenAiEmbeddingModelDelegate.build(apiKey, modelName, baseUrl, provider);
	}
}
