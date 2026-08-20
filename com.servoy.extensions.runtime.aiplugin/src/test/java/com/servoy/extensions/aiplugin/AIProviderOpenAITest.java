package com.servoy.extensions.aiplugin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.servoy.extensions.aiplugin.chat.ChatClient;
import com.servoy.extensions.aiplugin.chat.OpenAiChatBuilder;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.openaiofficial.OpenAiOfficialResponsesStreamingChatModel;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIProvider - OpenAI Responses API support")
class AIProviderOpenAITest
{
	@Mock
	private IClientPluginAccess access;

	private AIProvider provider;

	@BeforeEach
	void setUp()
	{
		provider = new AIProvider(access);
	}

	@Nested
	@DisplayName("createOpenAIClient()")
	class CreateOpenAIClient
	{
		@Test
		@DisplayName("returns a non-null ChatClient")
		void returnsNonNullChatClient()
		{
			ChatClient result = provider.createOpenAIClient("test-api-key", "gpt-5");
			assertNotNull(result);
		}

		@Test
		@DisplayName("returns a distinct instance each call")
		void returnsDistinctInstanceEachCall()
		{
			ChatClient first = provider.createOpenAIClient("test-api-key", "gpt-5");
			ChatClient second = provider.createOpenAIClient("test-api-key", "gpt-5");
			assertNotSame(first, second);
		}

		@Test
		@DisplayName("uses OpenAiOfficialResponsesStreamingChatModel (Responses API path)")
		void usesResponsesApiModel()
		{
			OpenAiOfficialResponsesStreamingChatModel.Builder mockBuilder = mock(OpenAiOfficialResponsesStreamingChatModel.Builder.class, RETURNS_SELF);
			OpenAiOfficialResponsesStreamingChatModel mockModel = mock(OpenAiOfficialResponsesStreamingChatModel.class);
			when(mockBuilder.build()).thenReturn(mockModel);

			try (MockedStatic<OpenAiOfficialResponsesStreamingChatModel> mocked = mockStatic(OpenAiOfficialResponsesStreamingChatModel.class))
			{
				mocked.when(OpenAiOfficialResponsesStreamingChatModel::builder).thenReturn(mockBuilder);

				ChatClient result = provider.createOpenAIClient("test-api-key", "gpt-5");

				assertNotNull(result);
				mocked.verify(OpenAiOfficialResponsesStreamingChatModel::builder);
				verify(mockBuilder).apiKey("test-api-key");
				verify(mockBuilder).modelName("gpt-5");
				verify(mockBuilder).build();
			}
		}
	}

	@Nested
	@DisplayName("createOpenAiChatBuilder()")
	class CreateOpenAiChatBuilder
	{
		@Test
		@DisplayName("returns an OpenAiChatBuilder instance")
		void returnsOpenAiChatBuilder()
		{
			Object result = provider.createOpenAiChatBuilder();
			assertInstanceOf(OpenAiChatBuilder.class, result);
		}

		@Test
		@DisplayName("returns a new instance each time")
		void returnsNewInstanceEachTime()
		{
			OpenAiChatBuilder first = provider.createOpenAiChatBuilder();
			OpenAiChatBuilder second = provider.createOpenAiChatBuilder();
			assertNotSame(first, second);
		}
	}

	@Nested
	@DisplayName("getAllReturnedTypes()")
	class GetAllReturnedTypes
	{
		@Test
		@DisplayName("includes OpenAiChatBuilder in returned types")
		void includesOpenAiChatBuilder()
		{
			Class< ? >[] types = provider.getAllReturnedTypes();
			boolean found = false;
			for (Class< ? > type : types)
			{
				if (type == OpenAiChatBuilder.class)
				{
					found = true;
					break;
				}
			}
			assertTrue(found);
		}
	}
}
