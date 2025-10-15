package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder;

@ServoyDocumented(scriptingName = "OpenAiEmbeddingBuilder")
public class OpenAiEmbeddingBuilder {

	private final IClientPluginAccess access;
	private final OpenAiEmbeddingModelBuilder builder;

	public OpenAiEmbeddingBuilder(IClientPluginAccess access) {
		this.access = access;
		builder = OpenAiEmbeddingModel.builder();
	}

	@JSFunction
	public OpenAiEmbeddingBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}
	
	@JSFunction
	public OpenAiEmbeddingBuilder modelName(String modelName) {
		builder.modelName(modelName);
		return this;
	}
	
	@JSFunction
	public EmbeddingClient build() {
		OpenAiEmbeddingModel model = builder.build();
		return new EmbeddingClient(model, access);
	}
}
