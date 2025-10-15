package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

@ServoyDocumented(publicName = "OpenAiChatBuilder", scriptingName = "OpenAiChatBuilder")
public class OpenAiChatBuilder {
	
	private final IClientPluginAccess access;
	private final OpenAiStreamingChatModelBuilder builder;
	private final AiServices<Assistant> assistent;
	
	private String modelName = "gpt-5";
	private Integer tokens;
	
	OpenAiChatBuilder(IClientPluginAccess access) {
		this.access = access;
		builder = OpenAiStreamingChatModel.builder().modelName(modelName);
		assistent = AiServices.builder(Assistant.class);

	}
	
	@JSFunction
	public OpenAiChatBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}
	
	@JSFunction
	public OpenAiChatBuilder modelName(String modelName) {
		this.modelName= modelName;
		builder.modelName(modelName);
		return this;
	}
	
	@JSFunction
	public OpenAiChatBuilder temperature(Double temperature) {
		builder.temperature(temperature);
		return this;
	}
	
	@JSFunction
	public OpenAiChatBuilder maxMemoryTokens(Integer tokens) {
		this.tokens = tokens;
		return this;
	}

	@JSFunction
	public ChatClient build() {
		assistent.streamingChatModel(builder.build());
		if (tokens != null) {
			OpenAiTokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator(modelName);
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder().maxTokens(tokens, tokenCountEstimator).build();
			assistent.chatMemory(tokenWindowChatMemory);
		}
		return new ChatClient(assistent.build(), access);
	}
}
