package com.servoy.extensions.aiplugin;

import static com.servoy.extensions.aiplugin.AiPluginService.AIPLUGIN_SERVICE;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDatabaseManager;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;

import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;

/**
 * AIProvider class that provides access to create AI chat and embedding
 * clients/builders.
 */
@ServoyDocumented(publicName = "ai", scriptingName = "plugins.ai")
public class AIProvider implements IReturnedTypesProvider, IScriptable {
	private final IClientPluginAccess access;
	private AiPluginService aiPluginService;

	AiPluginService getAiPluginService() throws Exception {
		if (aiPluginService == null) {
			aiPluginService = (AiPluginService) access.getRemoteService(AIPLUGIN_SERVICE);
		}
		return aiPluginService;
	}

	IApplication getApplication() {
		return (((ClientPluginAccessProvider) access).getApplication());
	}

	IDatabaseManager getDatabaseManager() {
		return (access.getDatabaseManager());
	}

	public String getClientID() {
		return access.getClientID();
	}

	/**
	 * Constructor for AIProvider.
	 *
	 * @param access The client plugin access instance.
	 */
	public AIProvider(IClientPluginAccess access) {
		this.access = access;
	}

	/**
	 * Returns all types that can be returned by this provider for scripting.
	 *
	 * @return An array of classes representing all returned types.
	 */
	@Override
	public Class<?>[] getAllReturnedTypes() {
		return new Class[] { ChatClient.class, GeminiChatBuilder.class, OpenAiChatBuilder.class, EmbeddingClient.class,
				GeminiEmbeddingBuilder.class, OpenAiEmbeddingBuilder.class, EmbeddingStore.class, ChatResponse.class };
	}

	/**
	 * Creates a builder for Gemini embeddings.
	 *
	 * @return GeminiEmbeddingBuilder instance.
	 */
	@JSFunction
	public GeminiEmbeddingBuilder createGeminiEmbeddedBuilder() {
		return new GeminiEmbeddingBuilder(this);
	}

	/**
	 * Creates a builder for OpenAI embeddings.
	 *
	 * @return OpenAiEmbeddingBuilder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingBuilder createOpenAiEmbeddedBuilder() {
		return new OpenAiEmbeddingBuilder(this);
	}

	/**
	 * Creates a builder for Gemini chat models.
	 *
	 * @return GeminiChatBuilder instance.
	 */
	@JSFunction
	public GeminiChatBuilder createGeminiChatBuilder() {
		return new GeminiChatBuilder(access);
	}

	/**
	 * Creates a builder for OpenAI chat models.
	 *
	 * @return OpenAiChatBuilder instance.
	 */
	@JSFunction
	public OpenAiChatBuilder createOpenAiChatBuilder() {
		return new OpenAiChatBuilder(access);
	}

	/**
	 * Creates a Gemini chat client using the provided API key and model name. This
	 * is a quick way to create a client without using the builder.
	 *
	 * @param apiKey    The Gemini API key.
	 * @param modelName The Gemini model name.
	 * @return ChatClient instance for Gemini.
	 */
	@JSFunction
	public ChatClient createGeminiClient(String apiKey, String modelName) {
		GoogleAiGeminiStreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder().temperature(null)
				.apiKey(apiKey).modelName(modelName).build();
		AiServices<Assistant> builder = AiServices.builder(Assistant.class);
		builder.streamingChatModel(model);
		return new ChatClient(builder.build(), access);
	}

	/**
	 * Creates an OpenAI chat client using the provided API key and model name. This
	 * is a quick way to create a client without using the builder.
	 *
	 * @param apiKey    The OpenAI API key.
	 * @param modelName The OpenAI model name.
	 * @return ChatClient instance for OpenAI.
	 */
	@JSFunction
	public ChatClient createOpenAIClient(String apiKey, String modelName) {
		OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder().apiKey(apiKey).modelName(modelName).build();
		AiServices<Assistant> builder = AiServices.builder(Assistant.class);
		builder.streamingChatModel(model);
		return new ChatClient(builder.build(), access);
	}
}