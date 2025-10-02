package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

@ServoyDocumented(publicName = "GeminiBuilder", scriptingName = "GeminiBuilder")
public class GeminiBuilder {
	
	private String apiKey;
	private String modelName = "gemini-2.5-flash";
	private Double temperature;

	private IClientPluginAccess access;
	
	GeminiBuilder(IClientPluginAccess access) {
		this.access = access;
	}
	
	@JSFunction
	public GeminiBuilder apiKey(String key) {
		this.apiKey = key;
		return this;
	}
	
	@JSFunction
	public GeminiBuilder modelName(String modelName) {
		this.modelName = modelName;
		return this;
	}
	
	@JSFunction
	public GeminiBuilder temperature(Double temperature) {
		this.temperature = temperature;
		return this;
	}

	@JSFunction
	public AIClient build() {
		GoogleAiGeminiStreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder().temperature(temperature).apiKey(apiKey)
				.modelName(modelName).build();
		return new AIClient(model, access);
	}
}
