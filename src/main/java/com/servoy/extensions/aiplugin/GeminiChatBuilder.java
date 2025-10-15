package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

@ServoyDocumented(publicName = "GeminiChatBuilder", scriptingName = "GeminiChatBuilder")
public class GeminiChatBuilder {
	
	private String apiKey;
	private String modelName = "gemini-2.5-flash";
	private Double temperature;
	private Integer tokens;

	private final IClientPluginAccess access;
	
	GeminiChatBuilder(IClientPluginAccess access) {
		this.access = access;
	}
	
	@JSFunction
	public GeminiChatBuilder apiKey(String key) {
		this.apiKey = key;
		return this;
	}
	
	@JSFunction
	public GeminiChatBuilder modelName(String modelName) {
		this.modelName = modelName;
		return this;
	}
	
	@JSFunction
	public GeminiChatBuilder temperature(Double temperature) {
		this.temperature = temperature;
		return this;
	}

	@JSFunction
	public GeminiChatBuilder maxMemoryTokens(Integer tokens) {
		this.tokens = tokens;
		return this;
	}

	@JSFunction
	public ChatClient build() {
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
		return new ChatClient(assistant, access);
	}
}
