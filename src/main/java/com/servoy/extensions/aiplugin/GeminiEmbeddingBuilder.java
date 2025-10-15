package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel.GoogleAiEmbeddingModelBuilder;

@ServoyDocumented(scriptingName = "GeminiEmbeddingBuilder")
public class GeminiEmbeddingBuilder {

	private final GoogleAiEmbeddingModelBuilder builder;
	private final IClientPluginAccess access;

	public GeminiEmbeddingBuilder(IClientPluginAccess access) {
		this.access = access;
		builder = GoogleAiEmbeddingModel.builder();
	}
	
	@JSFunction
	public GeminiEmbeddingBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}
	
	@JSFunction
	public GeminiEmbeddingBuilder modelName(String modelName) {
		builder.modelName(modelName);
		return this;
	}
	
	@JSFunction
	public EmbeddingClient build() {
		GoogleAiEmbeddingModel model = builder.build();
		return new EmbeddingClient(model, access);
	}

}
