package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

@ServoyDocumented(publicName = "GeminiBuilder", scriptingName = "GeminiBuilder")
public class OpenAiBuilder {
	
	private String apiKey;
	private String modelName = "gemini-2.5-flash";
	private Double temperature;

	private IClientPluginAccess access;
	
	OpenAiBuilder(IClientPluginAccess access) {
		this.access = access;
	}
	
	@JSFunction
	public OpenAiBuilder apiKey(String key) {
		this.apiKey = key;
		return this;
	}
	
	@JSFunction
	public OpenAiBuilder modelName(String modelName) {
		this.modelName = modelName;
		return this;
	}
	
	@JSFunction
	public OpenAiBuilder temperature(Double temperature) {
		this.temperature = temperature;
		return this;
	}

	@JSFunction
	public AIClient build() {
		OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder().apiKey(apiKey).modelName(modelName).temperature(temperature).build();
		return new AIClient(model, access);
	}
}
