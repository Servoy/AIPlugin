package com.servoy.extensions.aiplugin.chat;

import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Pair;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

/**
 * OpenAiChatBuilder is a builder for configuring and creating OpenAI chat
 * clients. Allows setting API key, model name, temperature, and memory token
 * limits for the OpenAI model.
 */
@ServoyDocumented
public class OpenAiChatBuilder extends BaseChatBuilder<OpenAiChatBuilder> implements IJavaScriptType
{
	/**
	 * The builder for the OpenAI streaming chat model.
	 */
	private final OpenAiStreamingChatModelBuilder builder;

	/**
	 * The OpenAI model name (default: "gpt-5").
	 */
	private String modelName = "gpt-5";

	/**
	 * Constructs an OpenAiChatBuilder with the given plugin access.
	 *
	 * @param access The client plugin access instance.
	 */
	public OpenAiChatBuilder(IClientPluginAccess access)
	{
		super(access);
		builder = OpenAiStreamingChatModel.builder().modelName(modelName);
	}

	/**
	 * Sets base url for an api like IONOS that is compatible with OpenAI API.
	 *
	 * @param url The base URL.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder baseUrl(String url)
	{
		builder.baseUrl(url);
		return this;
	}

	/**
	 * Sets the OpenAI API key.
	 *
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder apiKey(String key)
	{
		builder.apiKey(key);
		return this;
	}

	/**
	 * Sets the OpenAI model name.
	 *
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder modelName(@SuppressWarnings("hiding") String modelName)
	{
		this.modelName = modelName;
		builder.modelName(modelName);
		return this;
	}

	/**
	 * Sets the temperature for the OpenAI model.
	 *
	 * @param temperature The temperature value.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder temperature(Double temperature)
	{
		builder.temperature(temperature);
		return this;
	}

	/**
	 * Builds and returns a ChatClient configured with the specified OpenAI model
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
		AiServices<Assistant> assistantBuilder = assistantBuilderAndUsedCloseables.getLeft();
		assistantBuilder.streamingChatModel(builder.build());
		if (tokens != null)
		{
			OpenAiTokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator(modelName);
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder()
				.maxTokens(tokens, tokenCountEstimator).build();
			assistantBuilder.chatMemory(tokenWindowChatMemory);
		}
		return new ChatClient(assistantBuilder.build(), access, assistantBuilderAndUsedCloseables.getRight());
	}
}