package com.servoy.extensions.aiplugin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

import com.servoy.extensions.aiplugin.chat.Assistant;
import com.servoy.extensions.aiplugin.chat.BedrockChatBuilder;
import com.servoy.extensions.aiplugin.chat.ChatClient;
import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.service.AiServices;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIProvider - Bedrock support")
class AIProviderBedrockTest
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
	@DisplayName("createBedrockChatBuilder()")
	class CreateBedrockChatBuilder
	{
		@Test
		@DisplayName("returns a BedrockChatBuilder instance")
		void returnsBedrockChatBuilder()
		{
			Object result = provider.createBedrockChatBuilder();
			assertInstanceOf(BedrockChatBuilder.class, result);
		}

		@Test
		@DisplayName("returns a new instance each time")
		void returnsNewInstanceEachTime()
		{
			BedrockChatBuilder first = provider.createBedrockChatBuilder();
			BedrockChatBuilder second = provider.createBedrockChatBuilder();
			assertNotSame(first, second);
		}
	}

	@Nested
	@DisplayName("createBedrockClient()")
	class CreateBedrockClient
	{
		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("returns a non-null ChatClient")
		void returnsNonNullChatClient()
		{
			BedrockStreamingChatModel.Builder modelBuilder = mock(BedrockStreamingChatModel.Builder.class, RETURNS_SELF);
			BedrockStreamingChatModel mockModel = mock(BedrockStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);

			try (MockedStatic<BedrockStreamingChatModel> modelStatic = mockStatic(BedrockStreamingChatModel.class);
				MockedStatic<AiServices> aiServicesStatic = mockStatic(AiServices.class))
			{
				modelStatic.when(BedrockStreamingChatModel::builder).thenReturn(modelBuilder);
				aiServicesStatic.when(() -> AiServices.builder(Assistant.class)).thenReturn(mockAiServices);

				ChatClient result = provider.createBedrockClient("us-east-1", "us.anthropic.claude-sonnet-4-20250514-v1:0");
				assertNotNull(result);
			}
		}

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("returns a distinct instance each call")
		void returnsDistinctInstanceEachCall()
		{
			BedrockStreamingChatModel.Builder modelBuilder = mock(BedrockStreamingChatModel.Builder.class, RETURNS_SELF);
			BedrockStreamingChatModel mockModel = mock(BedrockStreamingChatModel.class);
			when(modelBuilder.build()).thenReturn(mockModel);

			AiServices<Assistant> mockAiServices = mock(AiServices.class, RETURNS_SELF);
			Assistant mockAssistant = mock(Assistant.class);
			when(mockAiServices.build()).thenReturn(mockAssistant);

			try (MockedStatic<BedrockStreamingChatModel> modelStatic = mockStatic(BedrockStreamingChatModel.class);
				MockedStatic<AiServices> aiServicesStatic = mockStatic(AiServices.class))
			{
				modelStatic.when(BedrockStreamingChatModel::builder).thenReturn(modelBuilder);
				aiServicesStatic.when(() -> AiServices.builder(Assistant.class)).thenReturn(mockAiServices);

				ChatClient first = provider.createBedrockClient("us-east-1", "us.anthropic.claude-sonnet-4-20250514-v1:0");
				ChatClient second = provider.createBedrockClient("us-east-1", "us.anthropic.claude-sonnet-4-20250514-v1:0");
				assertNotSame(first, second);
			}
		}
	}

	@Nested
	@DisplayName("getAllReturnedTypes()")
	class GetAllReturnedTypes
	{
		@Test
		@DisplayName("includes BedrockChatBuilder in returned types")
		void includesBedrockChatBuilder()
		{
			Class< ? >[] types = provider.getAllReturnedTypes();
			List<Class< ? >> typeList = Arrays.asList(types);
			assertTrue(typeList.contains(BedrockChatBuilder.class));
		}

		@Test
		@DisplayName("returned types array is not null")
		void returnedTypesNotNull()
		{
			assertNotNull(provider.getAllReturnedTypes());
		}

		@Test
		@DisplayName("returned types array is not empty")
		void returnedTypesNotEmpty()
		{
			assertTrue(provider.getAllReturnedTypes().length > 0);
		}
	}
}
