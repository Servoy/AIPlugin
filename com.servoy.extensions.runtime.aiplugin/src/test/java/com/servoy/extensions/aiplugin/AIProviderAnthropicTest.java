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
import org.mockito.junit.jupiter.MockitoExtension;

import com.servoy.extensions.aiplugin.chat.AnthropicChatBuilder;
import com.servoy.extensions.aiplugin.chat.ChatClient;
import com.servoy.j2db.plugins.IClientPluginAccess;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIProvider - Anthropic support")
class AIProviderAnthropicTest
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
	@DisplayName("createAnthropicChatBuilder()")
	class CreateAnthropicChatBuilder
	{
		@Test
		@DisplayName("returns an AnthropicChatBuilder instance")
		void returnsAnthropicChatBuilder()
		{
			Object result = provider.createAnthropicChatBuilder();
			assertInstanceOf(AnthropicChatBuilder.class, result);
		}

		@Test
		@DisplayName("returns a new instance each time")
		void returnsNewInstanceEachTime()
		{
			AnthropicChatBuilder first = provider.createAnthropicChatBuilder();
			AnthropicChatBuilder second = provider.createAnthropicChatBuilder();
			assertNotSame(first, second);
		}
	}

	@Nested
	@DisplayName("createAnthropicClient()")
	class CreateAnthropicClient
	{
		@Test
		@DisplayName("returns a non-null ChatClient")
		void returnsNonNullChatClient()
		{
			ChatClient result = provider.createAnthropicClient("test-api-key", "claude-sonnet-5");
			assertNotNull(result);
		}

		@Test
		@DisplayName("returns a distinct instance each call")
		void returnsDistinctInstanceEachCall()
		{
			ChatClient first = provider.createAnthropicClient("test-api-key", "claude-sonnet-5");
			ChatClient second = provider.createAnthropicClient("test-api-key", "claude-sonnet-5");
			assertNotSame(first, second);
		}
	}

	@Nested
	@DisplayName("getAllReturnedTypes()")
	class GetAllReturnedTypes
	{
		@Test
		@DisplayName("includes AnthropicChatBuilder in returned types")
		void includesAnthropicChatBuilder()
		{
			Class< ? >[] types = provider.getAllReturnedTypes();
			List<Class< ? >> typeList = Arrays.asList(types);
			assertTrue(typeList.contains(AnthropicChatBuilder.class));
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
