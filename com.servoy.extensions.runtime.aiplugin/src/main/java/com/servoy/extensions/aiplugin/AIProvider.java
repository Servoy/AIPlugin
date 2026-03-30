package com.servoy.extensions.aiplugin;

import static com.servoy.extensions.aiplugin.AIPlugin.PLUGIN_NAME;
import static com.servoy.extensions.aiplugin.AiPluginService.AIPLUGIN_SERVICE;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.servoy.extensions.aiplugin.embedding.*;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.chat.Assistant;
import com.servoy.extensions.aiplugin.chat.ChatClient;
import com.servoy.extensions.aiplugin.chat.ChatResponse;
import com.servoy.extensions.aiplugin.chat.GeminiChatBuilder;
import com.servoy.extensions.aiplugin.chat.OpenAiChatBuilder;
import com.servoy.extensions.aiplugin.chat.ToolBuilder;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDatabaseManager;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.Deferred;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.util.Debug;

import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;

import dev.toonformat.jtoon.JToon;

/**
 * AIProvider class that provides access to create AI chat and embedding
 * clients/builders.
 */
@ServoyDocumented(publicName = PLUGIN_NAME, scriptingName = "plugins." + PLUGIN_NAME)
public class AIProvider implements IReturnedTypesProvider, IScriptable {
	private final IClientPluginAccess access;
	private AiPluginService aiPluginService;

	/**
	 * Executor for running embedding operations asynchronously using virtual
	 * threads.
	 */
	private static ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

	public AiPluginService getAiPluginService() throws Exception {
		if (aiPluginService == null) {
			aiPluginService = (AiPluginService) access.getRemoteService(AIPLUGIN_SERVICE);
		}
		return aiPluginService;
	}

	IApplication getApplication() {
		return (((ClientPluginAccessProvider) access).getApplication());
	}

	public IDatabaseManager getDatabaseManager() {
		return access.getDatabaseManager();
	}

	public String getClientID() {
		return access.getClientID();
	}

	/**
	 * Default Constructor for AIProvider, this is for documentation purposes. Don't
	 * call this normally
	 *
	 * @param access The client plugin access instance.
	 */
	public AIProvider() {
		this.access = null;
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
		return new Class[] { ChatClient.class, GeminiChatBuilder.class, OpenAiChatBuilder.class,
				GeminiEmbeddingModelBuilder.class, OpenAiEmbeddingModelBuilder.class, ServoyEmbeddingStoreBuilder.class,
				EmbeddingStore.class, EmbeddingModel.class, ChatResponse.class, SearchResult.class, ToolBuilder.class };
	}

	/**
	 * Creates a builder for Gemini embeddings.
	 *
	 * @return GeminiEmbeddingModelBuilder instance.
	 */
	@JSFunction
	public GeminiEmbeddingModelBuilder createGeminiEmbeddingModelBuilder() {
		return new GeminiEmbeddingModelBuilder(this);
	}

	/**
	 * Creates a builder for OpenAI store.
	 *
	 * @return OpenAiEmbeddingModelBuilder instance.
	 */
	@JSFunction
	public OpenAiEmbeddingModelBuilder createOpenAiEmbeddingModelBuilder() {
		return new OpenAiEmbeddingModelBuilder(this);
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

	public NativePromise async(Callable<?> callable) {
		Deferred deferred = new Deferred(getApplication());
		if (callable == null) {
			deferred.resolve(null);
		} else {
			virtualThreadExecutor.submit(() -> {
				try {
					deferred.resolve(callable.call());
				} catch (Exception ex) {
					Debug.error(ex);
					deferred.reject(ex);
				}
			});
		}
		return deferred.getPromise();
	}
	
	/**
	 * Encode a JSON string to TOON format.
	 *
	 * @sample
	 * var json = '{"user":{"id":123,"name":"Ada","tags":["reading","gaming"]}}';
	 * var toon = plugins.ai.encodeToonFromJson(json);
	 *
	 * @param json JSON string to encode
	 * @return TOON formatted string, or null on error
	 */
	@JSFunction
	public String encodeToonFromJson(String json) {
	    if (json == null || json.trim().isEmpty()) return null;
	    try {
	        return JToon.encodeJson(json);
	    } catch (Exception e) {
	        Debug.error("Failed to encode JSON to TOON format: " + e.getMessage());
	        return null;
	    }
	}

	/**
	 * Decode a TOON formatted string to JSON.
	 *
	 * @sample
	 * var toon = 'user:\n  id: 123\n  name: Ada\n  tags[2]: reading,gaming';
	 * var json = plugins.ai.decodeToonToJson(toon);
	 *
	 * @param toon TOON formatted string
	 * @return JSON string, or null on error
	 */
	@JSFunction
	public String decodeToonToJson(String toon) {
	    if (toon == null || toon.trim().isEmpty()) return null;
	    try {
	        return JToon.decodeToJson(toon);
	    } catch (Exception e) {
	        Debug.error("Failed to decode TOON to JSON format: " + e.getMessage());
	        return null;
	    }
	}
}