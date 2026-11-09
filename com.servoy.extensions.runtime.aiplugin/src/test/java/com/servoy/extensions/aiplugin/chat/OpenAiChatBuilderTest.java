package com.servoy.extensions.aiplugin.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openai.models.ReasoningEffort;
import com.servoy.j2db.plugins.IClientPluginAccess;

import com.servoy.extensions.aiplugin.chat.openai.OpenAiFilesResponsesStreamingChatModel;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAiChatBuilder")
class OpenAiChatBuilderTest
{
	@Mock
	private IClientPluginAccess access;

	private OpenAiChatBuilder builder;

	@BeforeEach
	void setUp()
	{
		builder = new OpenAiChatBuilder(access);
	}

	@Nested
	@DisplayName("shouldUseResponsesApi()")
	class ShouldUseResponsesApi
	{
		@Test
		@DisplayName("returns true when no baseUrl is set")
		void returnsTrueWhenNoBaseUrl() throws Exception
		{
			assertTrue(invokeShouldUseResponsesApi(builder));
		}

		@Test
		@DisplayName("returns true when baseUrl starts with https://api.openai.com")
		void returnsTrueWhenBaseUrlIsOpenAi() throws Exception
		{
			builder.baseUrl("https://api.openai.com/v1");
			assertTrue(invokeShouldUseResponsesApi(builder));
		}

		@Test
		@DisplayName("returns false when baseUrl is a custom endpoint")
		void returnsFalseWhenCustomBaseUrl() throws Exception
		{
			builder.baseUrl("https://ionos.example.com/v1");
			assertFalse(invokeShouldUseResponsesApi(builder));
		}

		@Test
		@DisplayName("returns true when useResponsesApi(true) overrides custom baseUrl")
		void returnsTrueWhenExplicitOverrideWithCustomBaseUrl() throws Exception
		{
			builder.baseUrl("https://ionos.example.com/v1");
			builder.useResponsesApi(true);
			assertTrue(invokeShouldUseResponsesApi(builder));
		}

		@Test
		@DisplayName("returns false when useResponsesApi(false) overrides no baseUrl")
		void returnsFalseWhenExplicitFalseOverride() throws Exception
		{
			builder.useResponsesApi(false);
			assertFalse(invokeShouldUseResponsesApi(builder));
		}

		@Test
		@DisplayName("returns false when useResponsesApi(false) overrides OpenAI baseUrl")
		void returnsFalseWhenExplicitFalseOverridesOpenAiUrl() throws Exception
		{
			builder.baseUrl("https://api.openai.com/v1");
			builder.useResponsesApi(false);
			assertFalse(invokeShouldUseResponsesApi(builder));
		}
	}

	@Nested
	@DisplayName("Fluent API")
	class FluentApi
	{
		@Test
		@DisplayName("apiKey() returns this")
		void apiKeyReturnsSelf()
		{
			assertSame(builder, builder.apiKey("test-key"));
		}

		@Test
		@DisplayName("modelName() returns this")
		void modelNameReturnsSelf()
		{
			assertSame(builder, builder.modelName("gpt-5"));
		}

		@Test
		@DisplayName("temperature() returns this")
		void temperatureReturnsSelf()
		{
			assertSame(builder, builder.temperature(0.7));
		}

		@Test
		@DisplayName("baseUrl() returns this")
		void baseUrlReturnsSelf()
		{
			assertSame(builder, builder.baseUrl("https://example.com"));
		}

		@Test
		@DisplayName("reasoningEffort() returns this")
		void reasoningEffortReturnsSelf()
		{
			assertSame(builder, builder.reasoningEffort("high"));
		}

		@Test
		@DisplayName("useResponsesApi() returns this")
		void useResponsesApiReturnsSelf()
		{
			assertSame(builder, builder.useResponsesApi(true));
		}

		@Test
		@DisplayName("methods can be chained fluently")
		void methodsCanBeChained()
		{
			OpenAiChatBuilder result = builder
				.apiKey("key")
				.modelName("gpt-5")
				.temperature(0.5)
				.baseUrl("https://api.openai.com/v1")
				.reasoningEffort("medium")
				.useResponsesApi(true);
			assertSame(builder, result);
		}
	}

	@Nested
	@DisplayName("build()")
	class Build
	{
		@Test
		@DisplayName("builds a ChatClient using Responses API when no baseUrl is set")
		void buildWithResponsesApiNoBaseUrl()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5").build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("builds a ChatClient using Chat Completions when custom baseUrl is set")
		void buildWithChatCompletionsCustomBaseUrl()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.baseUrl("https://ionos.example.com/v1").build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("builds a ChatClient using Responses API when baseUrl is api.openai.com")
		void buildWithResponsesApiOpenAiBaseUrl()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.baseUrl("https://api.openai.com/v1").build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("builds a ChatClient with reasoningEffort when in Responses mode")
		void buildWithReasoningEffort()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.reasoningEffort("high").build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("builds a ChatClient with temperature set")
		void buildWithTemperature()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.temperature(0.7).build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("builds a ChatClient forcing Responses API with custom baseUrl")
		void buildForcingResponsesApiWithCustomBaseUrl()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.baseUrl("https://custom.example.com/v1")
				.useResponsesApi(true).build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("builds a ChatClient forcing Chat Completions with no baseUrl")
		void buildForcingChatCompletionsNoBaseUrl()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.useResponsesApi(false).build();
			assertNotNull(client);
		}
	}

	@Nested
	@DisplayName("Inherited methods")
	class InheritedMethods
	{
		@Test
		@DisplayName("addSystemMessage succeeds in Responses mode")
		void addSystemMessageResponsesMode()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.addSystemMessage("You are a helpful assistant").build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("addSystemMessage succeeds in Chat Completions mode")
		void addSystemMessageChatCompletionsMode()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.baseUrl("https://ionos.example.com/v1")
				.addSystemMessage("You are a helpful assistant").build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("maxMemoryTokens succeeds in Responses mode")
		void maxMemoryTokensResponsesMode()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.maxMemoryTokens(1000).build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("maxMemoryTokens succeeds in Chat Completions mode")
		void maxMemoryTokensChatCompletionsMode()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.baseUrl("https://ionos.example.com/v1")
				.maxMemoryTokens(1000).build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("useBuiltInTools succeeds in Responses mode")
		void useBuiltInToolsResponsesMode()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.useBuiltInTools(true).build();
			assertNotNull(client);
		}

		@Test
		@DisplayName("useBuiltInTools succeeds in Chat Completions mode")
		void useBuiltInToolsChatCompletionsMode()
		{
			ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
				.baseUrl("https://ionos.example.com/v1")
				.useBuiltInTools(true).build();
			assertNotNull(client);
		}
	}

	@Nested
	@DisplayName("build() verifies model path")
	class BuildVerifiesModelPath
	{
		@Test
		@DisplayName("Responses mode invokes OpenAiFilesResponsesStreamingChatModel.builder()")
		void responseModeUsesResponsesModel()
		{
			OpenAiFilesResponsesStreamingChatModel.Builder mockBuilder = mock(OpenAiFilesResponsesStreamingChatModel.Builder.class, RETURNS_SELF);
			OpenAiFilesResponsesStreamingChatModel mockModel = mock(OpenAiFilesResponsesStreamingChatModel.class);
			when(mockBuilder.build()).thenReturn(mockModel);

			try (MockedStatic<OpenAiFilesResponsesStreamingChatModel> mocked = mockStatic(OpenAiFilesResponsesStreamingChatModel.class))
			{
				mocked.when(OpenAiFilesResponsesStreamingChatModel::builder).thenReturn(mockBuilder);

				ChatClient client = builder.apiKey("test-key").modelName("gpt-5").build();

				assertNotNull(client);
				mocked.verify(OpenAiFilesResponsesStreamingChatModel::builder);
			}
		}

		@Test
		@DisplayName("Chat Completions mode invokes OpenAiStreamingChatModel.builder()")
		void chatCompletionsModeUsesChatCompletionsModel()
		{
			OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder mockBuilder = mock(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder.class,
				RETURNS_SELF);
			OpenAiStreamingChatModel mockModel = mock(OpenAiStreamingChatModel.class);
			when(mockBuilder.build()).thenReturn(mockModel);

			try (MockedStatic<OpenAiStreamingChatModel> mocked = mockStatic(OpenAiStreamingChatModel.class))
			{
				mocked.when(OpenAiStreamingChatModel::builder).thenReturn(mockBuilder);

				ChatClient client = builder.apiKey("test-key").modelName("gpt-5")
					.baseUrl("https://ionos.example.com/v1").build();

				assertNotNull(client);
				mocked.verify(OpenAiStreamingChatModel::builder);
			}
		}
	}

	@Nested
	@DisplayName("reasoningEffort validation list")
	class ReasoningEffortValidationList
	{
		/**
		 * The valid reasoning efforts are kept as a plain literal list in OpenAiChatBuilder on purpose: that class is
		 * loaded by the Servoy documentation loader even when the OpenAI provider JARs are absent, so it must not touch
		 * an OpenAI SDK type during class initialization. This guard test lives in the test module (where the OpenAI SDK
		 * is on the classpath) and fails if the SDK's ReasoningEffort.Known enum drifts away from the literal list, so
		 * the hard-coded values stay in sync without reintroducing the runtime dependency.
		 */
		@Test
		@DisplayName("matches the OpenAI SDK's ReasoningEffort.Known enum")
		void matchesSdkEnum()
		{
			List<String> expectedFromSdk = Arrays.stream(ReasoningEffort.Known.values())
				.map(known -> known.name().toLowerCase()).sorted().toList();
			List<String> literal = OpenAiChatBuilder.VALID_REASONING_EFFORTS.stream().sorted().toList();

			assertEquals(expectedFromSdk, literal,
				"OpenAiChatBuilder.VALID_REASONING_EFFORTS is out of sync with the OpenAI SDK's ReasoningEffort.Known enum. " +
					"Update the literal list in OpenAiChatBuilder to match (keep it a literal; do not reference the SDK type there).");
		}
	}

	private boolean invokeShouldUseResponsesApi(OpenAiChatBuilder target) throws Exception
	{
		Method method = OpenAiChatBuilder.class.getDeclaredMethod("shouldUseResponsesApi");
		method.setAccessible(true);
		return (boolean)method.invoke(target);
	}
}
