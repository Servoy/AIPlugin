package com.servoy.extensions.aiplugin.chat;

import java.util.List;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.Pair;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialResponsesStreamingChatModel;
import dev.langchain4j.service.AiServices;

class OpenAiChatDelegate {

	static ChatClient build(IClientPluginAccess access, Pair<AiServices<Assistant>, List< ? extends AutoCloseable>> assistantBuilderAndUsedCloseables,
		String apiKey, String modelName, String baseUrl, Double temperature, String reasoningEffort, Integer tokens, boolean useResponsesApi)
	{
		AiServices<Assistant> assistantBuilder = assistantBuilderAndUsedCloseables.getLeft();

		if (useResponsesApi)
		{
			OpenAiOfficialResponsesStreamingChatModel.Builder modelBuilder = OpenAiOfficialResponsesStreamingChatModel.builder()
				.apiKey(apiKey).modelName(modelName);
			if (baseUrl != null) modelBuilder.baseUrl(baseUrl);
			if (temperature != null) modelBuilder.temperature(temperature);
			if (reasoningEffort != null) modelBuilder.reasoningEffort(reasoningEffort);
			assistantBuilder.streamingChatModel(modelBuilder.build());
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
		return new ChatClient(assistantBuilder.build(), access, assistantBuilderAndUsedCloseables.getRight());
	}
}
