package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

@ServoyDocumented(publicName = "OpenAIBuilder", scriptingName = "OpenAIBuilder")
public class OpenAiBuilder {
	
	private String apiKey;
	private String modelName = "gpt-5";
	private Double temperature;

	private IClientPluginAccess access;
	private Integer tokens;
	
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
	public OpenAiBuilder maxMemoryTokens(Integer tokens) {
		this.tokens = tokens;
		return this;
	}

	@JSFunction
	public AIClient build() {
		OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder().apiKey(apiKey).modelName(modelName).temperature(temperature).build();
		AiServices<Assistant> builder = AiServices.builder(Assistant.class);
		builder.streamingChatModel(model);
		if (tokens != null) {
			
			OpenAiTokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator(modelName);
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder().maxTokens(tokens, tokenCountEstimator).build();
			builder.chatMemory(tokenWindowChatMemory);
		}
		Assistant assistant = builder.build();
		return new AIClient(assistant, access);
	}
}
