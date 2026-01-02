package com.servoy.extensions.aiplugin.chat;

import java.util.Map;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IJavaScriptType;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;

/**
 * OpenAiChatBuilder is a builder for configuring and creating OpenAI chat
 * clients. Allows setting API key, model name, temperature, and memory token
 * limits for the OpenAI model.
 */
@ServoyDocumented
public class OpenAiChatBuilder extends BaseChatBuilder<OpenAiChatBuilder> implements IJavaScriptType {
	/**
	 * The builder for the OpenAI streaming chat model.
	 */
	private final OpenAiStreamingChatModelBuilder builder;

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
	 * 
	 * @param access The client plugin access instance.
	 */
	public OpenAiChatBuilder(IClientPluginAccess access) {
		super(access);
		builder = OpenAiStreamingChatModel.builder().modelName(modelName);
	}

	/**
	 * Sets the OpenAI API key.
	 * 
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
	 * 
	 * @param modelName The model name.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder modelName(String modelName) {
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
	public OpenAiChatBuilder temperature(Double temperature) {
		builder.temperature(temperature);
		return this;
	}

	/**
	 * Sets the maximum number of memory tokens for chat history.
	 * 
	 * @param tokens The maximum number of tokens.
	 * @return This builder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder maxMemoryTokens(Integer tokens) {
		this.tokens = tokens;
		return this;
	}

	/**
	 * Builds and returns a ChatClient configured with the specified OpenAI model
	 * settings.
	 * 
	 * @return A configured ChatClient instance.
	 */
	@JSFunction
	public ChatClient build() {

		ToolSpecification webSearchTool = ToolSpecification.builder().name("web_search_preview")
				.description("Search the web for current information")
				// Voeg parameters toe als de specifieke tool dat vereist
				.build();

		AiServices<Assistant> assistant = createAssistantBuilder();
		assistant.streamingChatModel(builder.build());
		assistant.tools(Map.of(webSearchTool, new ToolExecutor() {

			@Override
			public String execute(ToolExecutionRequest request, Object memoryId) {
				System.err.println("Web search tool executed with input: " + request);
				return null;
			}
		}));
		if (tokens != null) {
			OpenAiTokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator(modelName);
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder()
					.maxTokens(tokens, tokenCountEstimator).build();
			assistant.chatMemory(tokenWindowChatMemory);
		}
		return new ChatClient(assistant.build(), access);
	}
}