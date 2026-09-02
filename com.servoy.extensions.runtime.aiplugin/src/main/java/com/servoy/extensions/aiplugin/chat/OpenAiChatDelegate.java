package com.servoy.extensions.aiplugin.chat;

import java.util.ArrayList;
import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ReasoningEffort;
import com.servoy.extensions.aiplugin.chat.openai.OpenAiFilesResponsesStreamingChatModel;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.Pair;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

class OpenAiChatDelegate
{

	static ChatClient build(IClientPluginAccess access, Pair<AiServices<Assistant>, List< ? extends AutoCloseable>> assistantBuilderAndUsedCloseables,
		String apiKey, String modelName, String baseUrl, Double temperature, String reasoningEffort, Integer tokens, boolean useResponsesApi)
	{
		AiServices<Assistant> assistantBuilder = assistantBuilderAndUsedCloseables.getLeft();

		OpenAiFileStore fileStore = null;

		if (useResponsesApi)
		{
			OpenAIClient client = buildOpenAiClient(apiKey, baseUrl);

			OpenAiFilesResponsesStreamingChatModel.Builder modelBuilder = OpenAiFilesResponsesStreamingChatModel.builder()
				.client(client).modelName(modelName);
			if (temperature != null) modelBuilder.temperature(temperature);
			if (reasoningEffort != null) modelBuilder.reasoningEffort(ReasoningEffort.of(reasoningEffort));
			assistantBuilder.streamingChatModel(modelBuilder.build());

			fileStore = new OpenAiFileStore(client);
		}
		else
		{
			var modelBuilder = OpenAiStreamingChatModel.builder()
				.apiKey(apiKey).modelName(modelName);
			if (baseUrl != null) modelBuilder.baseUrl(baseUrl);
			if (temperature != null) modelBuilder.temperature(temperature);
			assistantBuilder.streamingChatModel(modelBuilder.build());
		}

		if (tokens != null)
		{
			OpenAiTokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator(modelName);
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder()
				.maxTokens(tokens, tokenCountEstimator).build();
			assistantBuilder.chatMemory(tokenWindowChatMemory);
		}

		List< ? extends AutoCloseable> closeables = assistantBuilderAndUsedCloseables.getRight();
		if (fileStore != null)
		{
			List<AutoCloseable> withStore = new ArrayList<>();
			if (closeables != null) withStore.addAll(closeables);
			withStore.add(fileStore);
			return new ChatClient(assistantBuilder.build(), access, withStore, fileStore);
		}
		return new ChatClient(assistantBuilder.build(), access, closeables);
	}

	private static OpenAIClient buildOpenAiClient(String apiKey, String baseUrl)
	{
		OpenAIOkHttpClient.Builder clientBuilder = OpenAIOkHttpClient.builder().apiKey(apiKey);
		if (baseUrl != null) clientBuilder.baseUrl(baseUrl);
		return clientBuilder.build();
	}
}
