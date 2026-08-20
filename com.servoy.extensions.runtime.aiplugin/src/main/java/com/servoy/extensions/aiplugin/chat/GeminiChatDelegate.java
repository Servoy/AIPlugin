package com.servoy.extensions.aiplugin.chat;

import java.util.List;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.Pair;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator;
import dev.langchain4j.service.AiServices;

class GeminiChatDelegate {

	static ChatClient build(IClientPluginAccess access, Pair<AiServices<Assistant>, List< ? extends AutoCloseable>> assistantBuilderAndUsedCloseables,
		String apiKey, String modelName, Double temperature, Integer tokens)
	{
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
