package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

@ServoyDocumented(publicName = "GeminiBuilder", scriptingName = "GeminiBuilder")
public class GeminiBuilder {
	
	private String apiKey;
	private String modelName = "gemini-2.5-flash";
	private Double temperature;

	private IClientPluginAccess access;
	private Integer tokens;
	
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
	public GeminiBuilder maxMemoryTokens(Integer tokens) {
		this.tokens = tokens;
		return this;
	}

	@JSFunction
	public AIClient build() {
		GoogleAiGeminiStreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder().temperature(temperature).apiKey(apiKey)
				.modelName(modelName).build();
		
		AiServices<Assistant> builder = AiServices.builder(Assistant.class);
		builder.streamingChatModel(model);
		if (tokens != null) {
			
			GoogleAiGeminiTokenCountEstimator tokenCountEstimator = GoogleAiGeminiTokenCountEstimator.builder().apiKey(apiKey).modelName(modelName).build();
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder().maxTokens(tokens, tokenCountEstimator).build();
			builder.chatMemory(tokenWindowChatMemory);
		}
		Assistant assistant = builder.build();
		return new AIClient(assistant, access);
	}
}
