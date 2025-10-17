package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

/**
 * OpenAiChatBuilder is a builder for configuring and creating OpenAI chat clients.
 * Allows setting API key, model name, temperature, and memory token limits for the OpenAI model.
 */
@ServoyDocumented(publicName = "OpenAiChatBuilder", scriptingName = "OpenAiChatBuilder")
public class OpenAiChatBuilder {
	/**
	 * The client plugin access instance for Servoy scripting context.
	 */
	private final IClientPluginAccess access;
	/**
	 * The builder for the OpenAI streaming chat model.
	 */
	private final OpenAiStreamingChatModelBuilder builder;
	/**
	 * The AI services builder for the Assistant interface.
	 */
	private final AiServices<Assistant> assistent;
	/**
	 * The OpenAI model name (default: "gpt-5").
	 */
	private String modelName = "gpt-5";
	/**
	 * The maximum number of memory tokens for chat history.
	 */
	private Integer tokens;
	
	/**
	 * Constructs an OpenAiChatBuilder with the given plugin access.
	 * @param access The client plugin access instance.
	 */
	OpenAiChatBuilder(IClientPluginAccess access) {
		this.access = access;
		builder = OpenAiStreamingChatModel.builder().modelName(modelName);
		assistent = AiServices.builder(Assistant.class);

	}
	
	/**
	 * Sets the OpenAI API key.
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder apiKey(String key) {
		builder.apiKey(key);
		return this;
	}
	
	/**
	 * Sets the OpenAI model name.
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder modelName(String modelName) {
		this.modelName= modelName;
		builder.modelName(modelName);
		return this;
	}
	
	/**
	 * Sets the temperature for the OpenAI model.
	 * @param temperature The temperature value.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder temperature(Double temperature) {
		builder.temperature(temperature);
		return this;
	}
	
	/**
	 * Sets the maximum number of memory tokens for chat history.
	 * @param tokens The maximum number of tokens.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder maxMemoryTokens(Integer tokens) {
		this.tokens = tokens;
		return this;
	}

	/**
	 * Builds and returns a ChatClient configured with the specified OpenAI model settings.
	 * @return A configured ChatClient instance.
	 */
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