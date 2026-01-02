package com.servoy.extensions.aiplugin.chat;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IJavaScriptType;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

/**
 * GeminiChatBuilder is a builder for configuring and creating Gemini chat
 * clients. Allows setting API key, model name, temperature, and memory token
 * limits for the Gemini model.
 */
@ServoyDocumented
public class GeminiChatBuilder extends BaseChatBuilder<GeminiChatBuilder> implements IJavaScriptType {
	/**
	 * The Gemini API key.
	 */
	private String apiKey;
	/**
	 * The Gemini model name (default: "gemini-2.5-flash").
	 */
	private String modelName = "gemini-2.5-flash";
	/**
	 * The temperature for the Gemini model (controls randomness).
	 */
	private Double temperature;
	/**
	 * The maximum number of memory tokens for chat history.
	 */
	private Integer tokens;

	/**
	 * Constructs a GeminiChatBuilder with the given plugin access.
	 * 
	 * @param access The client plugin access instance.
	 */
	public GeminiChatBuilder(IClientPluginAccess access) {
		super(access);
	}

	/**
	 * Sets the Gemini API key.
	 * 
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiChatBuilder apiKey(String key) {
		this.apiKey = key;
		return this;
	}

	/**
	 * Sets the Gemini model name.
	 * 
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiChatBuilder modelName(String modelName) {
		this.modelName = modelName;
		return this;
	}

	/**
	 * Sets the temperature for the Gemini model.
	 * 
	 * @param temperature The temperature value.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiChatBuilder temperature(Double temperature) {
		this.temperature = temperature;
		return this;
	}

	/**
	 * Sets the maximum number of memory tokens for chat history.
	 * 
	 * @param tokens The maximum number of tokens.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiChatBuilder maxMemoryTokens(Integer tokens) {
		this.tokens = tokens;
		return this;
	}

	/**
	 * Builds and returns a ChatClient configured with the specified Gemini model
	 * settings.
	 * 
	 * @return A configured ChatClient instance.
	 */
	@JSFunction
	public ChatClient build() {
		AiServices<Assistant> builder = createAssistantBuilder();
		GoogleAiGeminiStreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder().temperature(temperature)
				.apiKey(apiKey).modelName(modelName).build();

		builder.streamingChatModel(model);
		if (tokens != null) {
			GoogleAiGeminiTokenCountEstimator tokenCountEstimator = GoogleAiGeminiTokenCountEstimator.builder()
					.apiKey(apiKey).modelName(modelName).build();
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder()
					.maxTokens(tokens, tokenCountEstimator).build();
			builder.chatMemory(tokenWindowChatMemory);
		}
		Assistant assistant = builder.build();
		return new ChatClient(assistant, access);
	}
}