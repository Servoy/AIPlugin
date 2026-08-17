package com.servoy.extensions.aiplugin.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mozilla.javascript.Function;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.Pair;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.service.AiServices;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;

@ExtendWith(MockitoExtension.class)
@DisplayName("BedrockChatBuilder")
class BedrockChatBuilderTest
{
	@Mock
	private IClientPluginAccess access;

	private BedrockChatBuilder builder;

	@BeforeEach
	void setUp()
	{
		builder = new BedrockChatBuilder(access);
	}

	@Nested
	@DisplayName("Fluent API")
	class FluentApi
	{
		@Test
		@DisplayName("region() returns this builder instance")
		void regionReturnsSelf()
		{
			BedrockChatBuilder result = builder.region("us-east-1");
			assertSame(builder, result);
		}

		@Test
		@DisplayName("modelId() returns this builder instance")
		void modelIdReturnsSelf()
		{
			BedrockChatBuilder result = builder.modelId("us.anthropic.claude-sonnet-4-20250514-v1:0");
			assertSame(builder, result);
		}

		@Test
		@DisplayName("accessKeyId() returns this builder instance")
		void accessKeyIdReturnsSelf()
		{
			BedrockChatBuilder result = builder.accessKeyId("AKIAIOSFODNN7EXAMPLE");
			assertSame(builder, result);
		}

		@Test
		@DisplayName("secretAccessKey() returns this builder instance")
		void secretAccessKeyReturnsSelf()
		{
			BedrockChatBuilder result = builder.secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
			assertSame(builder, result);
		}

		@Test
		@DisplayName("temperature() returns this builder instance")
		void temperatureReturnsSelf()
		{
			BedrockChatBuilder result = builder.temperature(0.7);
			assertSame(builder, result);
		}

		@Test
		@DisplayName("fluent chaining works across all setters")
		void fluentChainingWorks()
		{
			BedrockChatBuilder result = builder
				.region("us-east-1")
				.modelId("us.anthropic.claude-sonnet-4-20250514-v1:0")
				.accessKeyId("AKIAIOSFODNN7EXAMPLE")
				.secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
				.temperature(0.5);
			assertSame(builder, result);
		}
	}

	@Nested
	@DisplayName("Default values")
	class DefaultValues
	{
		@Test
		@DisplayName("default modelId is us.anthropic.claude-sonnet-4-20250514-v1:0")
		void defaultModelId() throws Exception
		{
			Field modelIdField = BedrockChatBuilder.class.getDeclaredField("modelId");
			modelIdField.setAccessible(true);
			assertEquals("us.anthropic.claude-sonnet-4-20250514-v1:0", modelIdField.get(builder));
		}

		@Test
		@DisplayName("default region is null")
		void defaultRegionIsNull() throws Exception
		{
			Field regionField = BedrockChatBuilder.class.getDeclaredField("region");
			regionField.setAccessible(true);
			assertNull(regionField.get(builder));
		}

		@Test
		@DisplayName("default accessKeyId is null")
		void defaultAccessKeyIdIsNull() throws Exception
		{
			Field accessKeyIdField = BedrockChatBuilder.class.getDeclaredField("accessKeyId");
			accessKeyIdField.setAccessible(true);
			assertNull(accessKeyIdField.get(builder));
		}

		@Test
		@DisplayName("default secretAccessKey is null")
		void defaultSecretAccessKeyIsNull() throws Exception
		{
			Field secretAccessKeyField = BedrockChatBuilder.class.getDeclaredField("secretAccessKey");
			secretAccessKeyField.setAccessible(true);
			assertNull(secretAccessKeyField.get(builder));
		}

		@Test
		@DisplayName("default temperature is null")
		void defaultTemperatureIsNull() throws Exception
		{
			Field temperatureField = BedrockChatBuilder.class.getDeclaredField("temperature");
			temperatureField.setAccessible(true);
			assertNull(temperatureField.get(builder));
		}
	}

	@Nested
	@DisplayName("Field assignment")
	class FieldAssignment
	{
		@Test
		@DisplayName("region() sets the region field")
		void regionSetsField() throws Exception
		{
			builder.region("eu-west-1");
			Field regionField = BedrockChatBuilder.class.getDeclaredField("region");
			regionField.setAccessible(true);
			assertEquals("eu-west-1", regionField.get(builder));
		}

		@Test
		@DisplayName("modelId() sets the modelId field")
		void modelIdSetsField() throws Exception
		{
			builder.modelId("us.meta.llama3-70b-instruct-v1:0");
			Field modelIdField = BedrockChatBuilder.class.getDeclaredField("modelId");
			modelIdField.setAccessible(true);
			assertEquals("us.meta.llama3-70b-instruct-v1:0", modelIdField.get(builder));
		}

		@Test
		@DisplayName("accessKeyId() sets the accessKeyId field")
		void accessKeyIdSetsField() throws Exception
		{
			builder.accessKeyId("AKIAIOSFODNN7EXAMPLE");
			Field accessKeyIdField = BedrockChatBuilder.class.getDeclaredField("accessKeyId");
			accessKeyIdField.setAccessible(true);
			assertEquals("AKIAIOSFODNN7EXAMPLE", accessKeyIdField.get(builder));
		}

		@Test
		@DisplayName("secretAccessKey() sets the secretAccessKey field")
		void secretAccessKeySetsField() throws Exception
		{
			builder.secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
			Field secretAccessKeyField = BedrockChatBuilder.class.getDeclaredField("secretAccessKey");
			secretAccessKeyField.setAccessible(true);
			assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", secretAccessKeyField.get(builder));
		}

		@Test
		@DisplayName("temperature() sets the temperature field")
		void temperatureSetsField() throws Exception
		{
			builder.temperature(0.9);
			Field temperatureField = BedrockChatBuilder.class.getDeclaredField("temperature");
			temperatureField.setAccessible(true);
			assertEquals(0.9, temperatureField.get(builder));
		}

		@Test
		@DisplayName("temperature() accepts null")
		void temperatureAcceptsNull() throws Exception
		{
			builder.temperature(0.5).temperature(null);
			Field temperatureField = BedrockChatBuilder.class.getDeclaredField("temperature");
			temperatureField.setAccessible(true);
			assertNull(temperatureField.get(builder));
		}
	}

	@Nested
	@DisplayName("Inheritance")
	class Inheritance
	{
		@Test
		@DisplayName("extends BaseChatBuilder")
		void extendsBaseChatBuilder()
		{
			assertInstanceOf(BaseChatBuilder.class, builder);
		}

		@Test
		@DisplayName("maxMemoryTokens() is inherited and returns correct type")
		void maxMemoryTokensInherited()
		{
			BedrockChatBuilder result = builder.maxMemoryTokens(4096);
			assertSame(builder, result);
		}

		@Test
		@DisplayName("useBuiltInTools() is inherited and returns correct type")
		void useBuiltInToolsInherited()
		{
			BedrockChatBuilder result = builder.useBuiltInTools(true);
			assertSame(builder, result);
		}

		@Test
		@DisplayName("addSystemMessage() is inherited and returns correct type")
		void addSystemMessageInherited()
		{
			BedrockChatBuilder result = builder.addSystemMessage("You are a helpful assistant.");
			assertSame(builder, result);
		}

		@Test
		@DisplayName("maxMemoryTokens sets tokens field in base class")
		void maxMemoryTokensSetsField() throws Exception
		{
			builder.maxMemoryTokens(8192);
			Field tokensField = BaseChatBuilder.class.getDeclaredField("tokens");
			tokensField.setAccessible(true);
			assertEquals(8192, tokensField.get(builder));
		}

		@Test
		@DisplayName("createTool() is inherited and returns correct type")
		void createToolInherited()
		{
			Function mockFunction = mock(Function.class);
			ToolBuilder<BedrockChatBuilder> result = builder.createTool(mockFunction, "testTool", "A test tool");
			assertNotNull(result);
			assertInstanceOf(ToolBuilder.class, result);
		}

		@Test
		@DisplayName("createMCPClient() is inherited and returns correct type")
		void createMCPClientInherited()
		{
			MCPClientBuilder<BedrockChatBuilder> result = builder.createMCPClient();
			assertNotNull(result);
			assertInstanceOf(MCPClientBuilder.class, result);
		}
	}

	@Nested
	@DisplayName("build()")
	class Build
	{
		@Test
		@DisplayName("throws when region is null")
		void throwsWhenRegionIsNull()
		{
			builder.modelId("us.anthropic.claude-sonnet-4-20250514-v1:0");
			RuntimeException ex = assertThrows(RuntimeException.class, () -> builder.build());
			assertTrue(ex.getMessage().contains("Region is required"));
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("build() uses StaticCredentialsProvider when both keys are set")
		void buildUsesStaticCredentialsWhenBothKeysSet()
		{
			BedrockChatBuilder spiedBuilder = spy(new BedrockChatBuilder(access));

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);
			doReturn(new Pair<>(mockAiServices, Collections.emptyList())).when(spiedBuilder).createAssistantBuilder();

			BedrockRuntimeAsyncClientBuilder clientBuilder = mock(BedrockRuntimeAsyncClientBuilder.class, RETURNS_SELF);
			BedrockRuntimeAsyncClient mockClient = mock(BedrockRuntimeAsyncClient.class);
			when(clientBuilder.build()).thenReturn(mockClient);

			BedrockStreamingChatModel.Builder modelBuilder = mock(BedrockStreamingChatModel.Builder.class, RETURNS_SELF);
			BedrockStreamingChatModel mockModel = mock(BedrockStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			try (MockedStatic<BedrockRuntimeAsyncClient> clientStatic = mockStatic(BedrockRuntimeAsyncClient.class);
				MockedStatic<BedrockStreamingChatModel> modelStatic = mockStatic(BedrockStreamingChatModel.class);
				MockedStatic<StaticCredentialsProvider> staticCredStatic = mockStatic(StaticCredentialsProvider.class);
				MockedStatic<AwsBasicCredentials> basicCredStatic = mockStatic(AwsBasicCredentials.class))
			{
				AwsBasicCredentials mockCreds = mock(AwsBasicCredentials.class);
				basicCredStatic.when(() -> AwsBasicCredentials.create("AKID", "SECRET")).thenReturn(mockCreds);
				StaticCredentialsProvider mockStaticProvider = mock(StaticCredentialsProvider.class);
				staticCredStatic.when(() -> StaticCredentialsProvider.create(mockCreds)).thenReturn(mockStaticProvider);

				clientStatic.when(BedrockRuntimeAsyncClient::builder).thenReturn(clientBuilder);
				modelStatic.when(BedrockStreamingChatModel::builder).thenReturn(modelBuilder);

				spiedBuilder.region("us-east-1").accessKeyId("AKID").secretAccessKey("SECRET");
				ChatClient result = spiedBuilder.build();

				assertNotNull(result);
				basicCredStatic.verify(() -> AwsBasicCredentials.create("AKID", "SECRET"));
				staticCredStatic.verify(() -> StaticCredentialsProvider.create(mockCreds));
			}
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("build() uses DefaultCredentialsProvider when no keys are set")
		void buildUsesDefaultCredentialsWhenNoKeysSet()
		{
			BedrockChatBuilder spiedBuilder = spy(new BedrockChatBuilder(access));

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);
			doReturn(new Pair<>(mockAiServices, Collections.emptyList())).when(spiedBuilder).createAssistantBuilder();

			BedrockRuntimeAsyncClientBuilder clientBuilder = mock(BedrockRuntimeAsyncClientBuilder.class, RETURNS_SELF);
			BedrockRuntimeAsyncClient mockClient = mock(BedrockRuntimeAsyncClient.class);
			when(clientBuilder.build()).thenReturn(mockClient);

			BedrockStreamingChatModel.Builder modelBuilder = mock(BedrockStreamingChatModel.Builder.class, RETURNS_SELF);
			BedrockStreamingChatModel mockModel = mock(BedrockStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			try (MockedStatic<BedrockRuntimeAsyncClient> clientStatic = mockStatic(BedrockRuntimeAsyncClient.class);
				MockedStatic<BedrockStreamingChatModel> modelStatic = mockStatic(BedrockStreamingChatModel.class);
				MockedStatic<DefaultCredentialsProvider> defaultCredStatic = mockStatic(DefaultCredentialsProvider.class))
			{
				DefaultCredentialsProvider mockDefaultProvider = mock(DefaultCredentialsProvider.class);
				defaultCredStatic.when(DefaultCredentialsProvider::create).thenReturn(mockDefaultProvider);

				clientStatic.when(BedrockRuntimeAsyncClient::builder).thenReturn(clientBuilder);
				modelStatic.when(BedrockStreamingChatModel::builder).thenReturn(modelBuilder);

				spiedBuilder.region("us-east-1");
				ChatClient result = spiedBuilder.build();

				assertNotNull(result);
				defaultCredStatic.verify(DefaultCredentialsProvider::create);
			}
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("build() returns a non-null ChatClient")
		void buildReturnsNonNullChatClient()
		{
			BedrockChatBuilder spiedBuilder = spy(new BedrockChatBuilder(access));

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);
			doReturn(new Pair<>(mockAiServices, Collections.emptyList())).when(spiedBuilder).createAssistantBuilder();

			BedrockRuntimeAsyncClientBuilder clientBuilder = mock(BedrockRuntimeAsyncClientBuilder.class, RETURNS_SELF);
			BedrockRuntimeAsyncClient mockClient = mock(BedrockRuntimeAsyncClient.class);
			when(clientBuilder.build()).thenReturn(mockClient);

			BedrockStreamingChatModel.Builder modelBuilder = mock(BedrockStreamingChatModel.Builder.class, RETURNS_SELF);
			BedrockStreamingChatModel mockModel = mock(BedrockStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			try (MockedStatic<BedrockRuntimeAsyncClient> clientStatic = mockStatic(BedrockRuntimeAsyncClient.class);
				MockedStatic<BedrockStreamingChatModel> modelStatic = mockStatic(BedrockStreamingChatModel.class);
				MockedStatic<DefaultCredentialsProvider> defaultCredStatic = mockStatic(DefaultCredentialsProvider.class))
			{
				DefaultCredentialsProvider mockDefaultProvider = mock(DefaultCredentialsProvider.class);
				defaultCredStatic.when(DefaultCredentialsProvider::create).thenReturn(mockDefaultProvider);

				clientStatic.when(BedrockRuntimeAsyncClient::builder).thenReturn(clientBuilder);
				modelStatic.when(BedrockStreamingChatModel::builder).thenReturn(modelBuilder);

				spiedBuilder.region("us-east-1").modelId("us.anthropic.claude-sonnet-4-20250514-v1:0");
				ChatClient result = spiedBuilder.build();

				assertNotNull(result);
				assertInstanceOf(ChatClient.class, result);
			}
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("build() with maxMemoryTokens wires TokenWindowChatMemory")
		void buildWithMaxMemoryTokensWiresTokenMemory()
		{
			BedrockChatBuilder spiedBuilder = spy(new BedrockChatBuilder(access));

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);
			doReturn(new Pair<>(mockAiServices, Collections.emptyList())).when(spiedBuilder).createAssistantBuilder();

			BedrockRuntimeAsyncClientBuilder clientBuilder = mock(BedrockRuntimeAsyncClientBuilder.class, RETURNS_SELF);
			BedrockRuntimeAsyncClient mockClient = mock(BedrockRuntimeAsyncClient.class);
			when(clientBuilder.build()).thenReturn(mockClient);

			BedrockStreamingChatModel.Builder modelBuilder = mock(BedrockStreamingChatModel.Builder.class, RETURNS_SELF);
			BedrockStreamingChatModel mockModel = mock(BedrockStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			try (MockedStatic<BedrockRuntimeAsyncClient> clientStatic = mockStatic(BedrockRuntimeAsyncClient.class);
				MockedStatic<BedrockStreamingChatModel> modelStatic = mockStatic(BedrockStreamingChatModel.class);
				MockedStatic<DefaultCredentialsProvider> defaultCredStatic = mockStatic(DefaultCredentialsProvider.class);
				MockedStatic<TokenWindowChatMemory> memoryStatic = mockStatic(TokenWindowChatMemory.class, RETURNS_DEEP_STUBS))
			{
				DefaultCredentialsProvider mockDefaultProvider = mock(DefaultCredentialsProvider.class);
				defaultCredStatic.when(DefaultCredentialsProvider::create).thenReturn(mockDefaultProvider);

				clientStatic.when(BedrockRuntimeAsyncClient::builder).thenReturn(clientBuilder);
				modelStatic.when(BedrockStreamingChatModel::builder).thenReturn(modelBuilder);

				spiedBuilder.region("us-east-1").maxMemoryTokens(4096);
				ChatClient result = spiedBuilder.build();

				assertNotNull(result);
				memoryStatic.verify(TokenWindowChatMemory::builder);
			}
		}
	}
}
