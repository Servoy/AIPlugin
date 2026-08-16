package com.servoy.extensions.aiplugin.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.servoy.j2db.plugins.IClientPluginAccess;


import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import org.mozilla.javascript.Function;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnthropicChatBuilder")
class AnthropicChatBuilderTest
{
	@Mock
	private IClientPluginAccess access;

	private AnthropicChatBuilder builder;

	@BeforeEach
	void setUp()
	{
		builder = new AnthropicChatBuilder(access);
	}

	@Nested
	@DisplayName("Fluent API")
	class FluentApi
	{
		@Test
		@DisplayName("apiKey() returns this builder instance")
		void apiKeyReturnsSelf()
		{
			AnthropicChatBuilder result = builder.apiKey("test-key");
			assertSame(builder, result);
		}

		@Test
		@DisplayName("modelName() returns this builder instance")
		void modelNameReturnsSelf()
		{
			AnthropicChatBuilder result = builder.modelName("claude-opus-4");
			assertSame(builder, result);
		}

		@Test
		@DisplayName("temperature() returns this builder instance")
		void temperatureReturnsSelf()
		{
			AnthropicChatBuilder result = builder.temperature(0.7);
			assertSame(builder, result);
		}

		@Test
		@DisplayName("fluent chaining works across all setters")
		void fluentChainingWorks()
		{
			AnthropicChatBuilder result = builder
				.apiKey("key")
				.modelName("claude-sonnet-5")
				.temperature(0.5);
			assertSame(builder, result);
		}
	}

	@Nested
	@DisplayName("Default values")
	class DefaultValues
	{
		@Test
		@DisplayName("default model name is claude-sonnet-5")
		void defaultModelName() throws Exception
		{
			Field modelNameField = AnthropicChatBuilder.class.getDeclaredField("modelName");
			modelNameField.setAccessible(true);
			assertEquals("claude-sonnet-5", modelNameField.get(builder));
		}

		@Test
		@DisplayName("default temperature is null")
		void defaultTemperatureIsNull() throws Exception
		{
			Field temperatureField = AnthropicChatBuilder.class.getDeclaredField("temperature");
			temperatureField.setAccessible(true);
			assertNull(temperatureField.get(builder));
		}

		@Test
		@DisplayName("default apiKey is null")
		void defaultApiKeyIsNull() throws Exception
		{
			Field apiKeyField = AnthropicChatBuilder.class.getDeclaredField("apiKey");
			apiKeyField.setAccessible(true);
			assertNull(apiKeyField.get(builder));
		}
	}

	@Nested
	@DisplayName("Field assignment")
	class FieldAssignment
	{
		@Test
		@DisplayName("apiKey() sets the apiKey field")
		void apiKeySetsField() throws Exception
		{
			builder.apiKey("my-secret-key");
			Field apiKeyField = AnthropicChatBuilder.class.getDeclaredField("apiKey");
			apiKeyField.setAccessible(true);
			assertEquals("my-secret-key", apiKeyField.get(builder));
		}

		@Test
		@DisplayName("modelName() sets the modelName field")
		void modelNameSetsField() throws Exception
		{
			builder.modelName("claude-opus-4");
			Field modelNameField = AnthropicChatBuilder.class.getDeclaredField("modelName");
			modelNameField.setAccessible(true);
			assertEquals("claude-opus-4", modelNameField.get(builder));
		}

		@Test
		@DisplayName("temperature() sets the temperature field")
		void temperatureSetsField() throws Exception
		{
			builder.temperature(0.9);
			Field temperatureField = AnthropicChatBuilder.class.getDeclaredField("temperature");
			temperatureField.setAccessible(true);
			assertEquals(0.9, temperatureField.get(builder));
		}

		@Test
		@DisplayName("apiKey() accepts null")
		void apiKeyAcceptsNull() throws Exception
		{
			builder.apiKey("key").apiKey(null);
			Field apiKeyField = AnthropicChatBuilder.class.getDeclaredField("apiKey");
			apiKeyField.setAccessible(true);
			assertNull(apiKeyField.get(builder));
		}

		@Test
		@DisplayName("modelName() accepts null")
		void modelNameAcceptsNull() throws Exception
		{
			builder.modelName(null);
			Field modelNameField = AnthropicChatBuilder.class.getDeclaredField("modelName");
			modelNameField.setAccessible(true);
			assertNull(modelNameField.get(builder));
		}

		@Test
		@DisplayName("temperature() accepts null")
		void temperatureAcceptsNull() throws Exception
		{
			builder.temperature(0.5).temperature(null);
			Field temperatureField = AnthropicChatBuilder.class.getDeclaredField("temperature");
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
			AnthropicChatBuilder result = builder.maxMemoryTokens(4096);
			assertSame(builder, result);
		}

		@Test
		@DisplayName("useBuiltInTools() is inherited and returns correct type")
		void useBuiltInToolsInherited()
		{
			AnthropicChatBuilder result = builder.useBuiltInTools(true);
			assertSame(builder, result);
		}

		@Test
		@DisplayName("addSystemMessage() is inherited and returns correct type")
		void addSystemMessageInherited()
		{
			AnthropicChatBuilder result = builder.addSystemMessage("You are a helpful assistant.");
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
			ToolBuilder<AnthropicChatBuilder> result = builder.createTool(mockFunction, "testTool", "A test tool");
			assertNotNull(result);
			assertInstanceOf(ToolBuilder.class, result);
		}

		@Test
		@DisplayName("createMCPClient() is inherited and returns correct type")
		void createMCPClientInherited()
		{
			MCPClientBuilder<AnthropicChatBuilder> result = builder.createMCPClient();
			assertNotNull(result);
			assertInstanceOf(MCPClientBuilder.class, result);
		}
	}

	@Nested
	@DisplayName("build()")
	class Build
	{
		@Test
		@DisplayName("throws when apiKey is null")
		void throwsWhenApiKeyIsNull()
		{
			builder.modelName("claude-sonnet-5");
			assertThrows(Exception.class, () -> builder.build());
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("build() returns a non-null ChatClient")
		void buildReturnsNonNullChatClient()
		{
			AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder modelBuilder = mock(
				AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder.class, RETURNS_SELF);
			AnthropicStreamingChatModel mockModel = mock(AnthropicStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);

			try (MockedStatic<AnthropicStreamingChatModel> modelStatic = mockStatic(AnthropicStreamingChatModel.class);
				MockedStatic<AiServices> aiServicesStatic = mockStatic(AiServices.class))
			{
				modelStatic.when(AnthropicStreamingChatModel::builder).thenReturn(modelBuilder);
				aiServicesStatic.when(() -> AiServices.builder(Assistant.class)).thenReturn(mockAiServices);

				builder.apiKey("sk-ant-test-key-1234567890").modelName("claude-sonnet-5");
				ChatClient result = builder.build();

				assertNotNull(result);
				assertInstanceOf(ChatClient.class, result);
			}
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("build() with maxMemoryTokens wires AnthropicTokenCountEstimator")
		void buildWithMaxMemoryTokensWiresTokenEstimator()
		{
			AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder modelBuilder = mock(
				AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder.class, RETURNS_SELF);
			AnthropicStreamingChatModel mockModel = mock(AnthropicStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);

			try (MockedStatic<AnthropicStreamingChatModel> modelStatic = mockStatic(AnthropicStreamingChatModel.class);
				MockedStatic<AiServices> aiServicesStatic = mockStatic(AiServices.class);
				MockedStatic<AnthropicTokenCountEstimator> estimatorStatic = mockStatic(AnthropicTokenCountEstimator.class, RETURNS_DEEP_STUBS);
				MockedStatic<TokenWindowChatMemory> memoryStatic = mockStatic(TokenWindowChatMemory.class, RETURNS_DEEP_STUBS))
			{
				modelStatic.when(AnthropicStreamingChatModel::builder).thenReturn(modelBuilder);
				aiServicesStatic.when(() -> AiServices.builder(Assistant.class)).thenReturn(mockAiServices);

				builder.apiKey("sk-ant-test-key-1234567890").modelName("claude-sonnet-5").maxMemoryTokens(4096);
				ChatClient result = builder.build();

				assertNotNull(result);
				estimatorStatic.verify(AnthropicTokenCountEstimator::builder);
			}
		}
	}
}
