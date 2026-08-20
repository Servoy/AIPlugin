package com.servoy.extensions.aiplugin.chat;

import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.Pair;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters;
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

class BedrockChatDelegate {

	static ChatClient build(IClientPluginAccess access, Pair<AiServices<Assistant>, List< ? extends AutoCloseable>> assistantBuilderAndUsedCloseables,
		String region, String modelId, String accessKeyId, String secretAccessKey, Double temperature, Integer tokens)
	{
		AwsCredentialsProvider credentialsProvider;
		if (accessKeyId != null && secretAccessKey != null)
		{
			credentialsProvider = StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKeyId, secretAccessKey));
		}
		else
		{
			credentialsProvider = DefaultCredentialsProvider.create();
		}

		BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider)
			.build();

		BedrockStreamingChatModel model = BedrockStreamingChatModel.builder()
			.region(Region.of(region))
			.modelId(modelId)
			.defaultRequestParameters(temperature != null
				? BedrockChatRequestParameters.builder().temperature(temperature).build()
				: null)
			.client(client)
			.build();

		AiServices<Assistant> assistantBuilder = assistantBuilderAndUsedCloseables.getLeft();
		assistantBuilder.streamingChatModel(model);
		if (tokens != null)
		{
			OpenAiTokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator("gpt-4o");
			TokenWindowChatMemory tokenWindowChatMemory = TokenWindowChatMemory.builder()
				.maxTokens(tokens, tokenCountEstimator).build();
			assistantBuilder.chatMemory(tokenWindowChatMemory);
		}
		Assistant assistant = assistantBuilder.build();
		List<AutoCloseable> closeables = assistantBuilderAndUsedCloseables.getRight() != null
			? new ArrayList<>(assistantBuilderAndUsedCloseables.getRight())
			: new ArrayList<>();
		closeables.add(client);
		return new ChatClient(assistant, access, closeables);
	}
}
