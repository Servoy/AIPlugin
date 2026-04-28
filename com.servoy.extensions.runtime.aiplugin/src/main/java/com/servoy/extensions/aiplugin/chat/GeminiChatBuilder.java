package com.servoy.extensions.aiplugin.chat;

import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Pair;

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
public class GeminiChatBuilder extends BaseChatBuilder<GeminiChatBuilder> implements IJavaScriptType
{
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
	 * Constructs a GeminiChatBuilder with the given plugin access.
	 *
	 * @param access The client plugin access instance.
	 */
	public GeminiChatBuilder(IClientPluginAccess access)
	{
		super(access);
	}

	/**
	 * Sets the Gemini API key.
	 *
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public GeminiChatBuilder apiKey(String key)
	{
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
	public GeminiChatBuilder modelName(@SuppressWarnings("hiding") String modelName)
	{
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
	public GeminiChatBuilder temperature(@SuppressWarnings("hiding") Double temperature)
	{
		this.temperature = temperature;
		return this;
	}

	/**
	 * Builds and returns a ChatClient configured with the specified Gemini model
	 * settings.
	 *
	 * <p>
	 * IMPORTANT: When you no longer use the ChatClient returned by .build(), do call .close() on it in order to release
	 * resources like MCP server connections or processes.
	 * </p>
	 *
	 * @return A configured ChatClient instance.
	 */
	@Override
	@JSFunction
	public ChatClient build()
	{
		Pair<AiServices<Assistant>, List< ? extends AutoCloseable>> assistantBuilderAndUsedCloseables = createAssistantBuilder();
		GoogleAiGeminiStreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder().temperature(temperature)
			.apiKey(apiKey).modelName(modelName).build();

		AiServices<Assistant> assistantBuilder = assistantBuilderAndUsedCloseables.getLeft();
		assistantBuilder.streamingChatModel(model);
		if (tokens != null)
		{
			GoogleAiGeminiTokenCountEstimator tokenCountEstimator = GoogleAiGeminiTokenCountEstimator.builder()
				.apiKey(apiKey).modelName(modelName).build();
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder()
				.maxTokens(tokens, tokenCountEstimator).build();
			assistantBuilder.chatMemory(tokenWindowChatMemory);
		}
		Assistant assistant = assistantBuilder.build();
		return new ChatClient(assistant, access, assistantBuilderAndUsedCloseables.getRight());
	}
}