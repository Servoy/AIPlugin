package com.servoy.extensions.aiplugin.chat;

import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.ProviderLoader;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Pair;

import dev.langchain4j.service.AiServices;

/**
 * AnthropicChatBuilder is a builder for configuring and creating Anthropic chat
 * clients. Allows setting API key, model name, temperature, and memory token
 * limits for the Anthropic model.
 */
@ServoyDocumented
public class AnthropicChatBuilder extends BaseChatBuilder<AnthropicChatBuilder> implements IJavaScriptType
{
	/**
	 * The Anthropic API key.
	 */
	private String apiKey;
	/**
	 * The Anthropic model name (default: "claude-sonnet-5").
	 */
	private String modelName = "claude-sonnet-5";
	/**
	 * The temperature for the Anthropic model (controls randomness).
	 */
	private Double temperature;

	/**
	 * Constructs an AnthropicChatBuilder with the given plugin access.
	 *
	 * @param access The client plugin access instance.
	 */
	public AnthropicChatBuilder(IClientPluginAccess access)
	{
		super(access);
	}

	/**
	 * Sets the Anthropic API key.
	 *
	 * @param key The API key.
	 * @return This builder instance.
	 */
	@JSFunction
	public AnthropicChatBuilder apiKey(String key)
	{
		this.apiKey = key;
		return this;
	}

	/**
	 * Sets the Anthropic model name.
	 *
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public AnthropicChatBuilder modelName(@SuppressWarnings("hiding") String modelName)
	{
		this.modelName = modelName;
		return this;
	}

	/**
	 * Sets the temperature for the Anthropic model.
	 *
	 * @param temperature The temperature value.
	 * @return This builder instance.
	 */
	@JSFunction
	public AnthropicChatBuilder temperature(@SuppressWarnings("hiding") Double temperature)
	{
		this.temperature = temperature;
		return this;
	}

	/**
	 * Builds and returns a ChatClient configured with the specified Anthropic model
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
		ProviderLoader.ensureAvailable(
			"dev.langchain4j.model.anthropic.AnthropicStreamingChatModel",
			"Anthropic",
			"anthropic");
		Pair<AiServices<Assistant>, List< ? extends AutoCloseable>> assistantBuilderAndUsedCloseables = createAssistantBuilder();
		return AnthropicChatDelegate.build(access, assistantBuilderAndUsedCloseables,
			apiKey, modelName, temperature, tokens);
	}
}
