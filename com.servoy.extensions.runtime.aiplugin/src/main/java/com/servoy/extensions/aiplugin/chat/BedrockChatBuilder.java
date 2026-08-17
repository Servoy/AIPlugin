package com.servoy.extensions.aiplugin.chat;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IJavaScriptType;
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

/**
 * BedrockChatBuilder is a builder for configuring and creating Amazon Bedrock chat
 * clients. Allows setting AWS region, model ID, credentials, and temperature for
 * Bedrock-hosted models.
 */
@ServoyDocumented
public class BedrockChatBuilder extends BaseChatBuilder<BedrockChatBuilder> implements IJavaScriptType
{
	private String region;
	private String modelId = "us.anthropic.claude-sonnet-4-20250514-v1:0";
	private String accessKeyId;
	private String secretAccessKey;
	private Double temperature;

	/**
	 * Constructs a BedrockChatBuilder with the given plugin access.
	 *
	 * @param access The client plugin access instance.
	 */
	public BedrockChatBuilder(IClientPluginAccess access)
	{
		super(access);
	}

	/**
	 * Sets the AWS region for the Bedrock model.
	 *
	 * @param region The AWS region (e.g. "us-east-1").
	 * @return This builder instance.
	 */
	@JSFunction
	public BedrockChatBuilder region(@SuppressWarnings("hiding") String region)
	{
		this.region = region;
		return this;
	}

	/**
	 * Sets the Bedrock model ID.
	 *
	 * @param modelId The model ID (e.g. "us.anthropic.claude-sonnet-4-20250514-v1:0").
	 * @return This builder instance.
	 */
	@JSFunction
	public BedrockChatBuilder modelId(@SuppressWarnings("hiding") String modelId)
	{
		this.modelId = modelId;
		return this;
	}

	/**
	 * Sets the AWS access key ID.
	 *
	 * @param accessKeyId The AWS access key ID.
	 * @return This builder instance.
	 */
	@JSFunction
	public BedrockChatBuilder accessKeyId(@SuppressWarnings("hiding") String accessKeyId)
	{
		this.accessKeyId = accessKeyId;
		return this;
	}

	/**
	 * Sets the AWS secret access key.
	 *
	 * @param secretAccessKey The AWS secret access key.
	 * @return This builder instance.
	 */
	@JSFunction
	public BedrockChatBuilder secretAccessKey(@SuppressWarnings("hiding") String secretAccessKey)
	{
		this.secretAccessKey = secretAccessKey;
		return this;
	}

	/**
	 * Sets the temperature for the Bedrock model.
	 *
	 * @param temperature The temperature value.
	 * @return This builder instance.
	 */
	@JSFunction
	public BedrockChatBuilder temperature(@SuppressWarnings("hiding") Double temperature)
	{
		this.temperature = temperature;
		return this;
	}

	/**
	 * Builds and returns a ChatClient configured with the specified Bedrock model
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
		if (region == null)
		{
			throw new RuntimeException("Region is required for Bedrock. Call .region(\"us-east-1\") before .build().");
		}

		if ((accessKeyId == null) != (secretAccessKey == null))
		{
			throw new RuntimeException(
				"Both accessKeyId and secretAccessKey must be provided together, or neither (to use the default AWS credentials chain).");
		}

		Pair<AiServices<Assistant>, List< ? extends AutoCloseable>> assistantBuilderAndUsedCloseables = createAssistantBuilder();

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
