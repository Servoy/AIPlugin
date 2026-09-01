package com.servoy.extensions.aiplugin.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

@DisplayName("ChatClient file routing")
class ChatClientFileStoreTest
{
	private static final byte[] PDF_BYTES = "%PDF-1.4 test".getBytes();

	private final IClientPluginAccess access = mock(IClientPluginAccess.class);
	private final Assistant assistant = mock(Assistant.class);

	private ChatClient client(FileStore fileStore)
	{
		return new ChatClient(assistant, access, null, fileStore);
	}

	@Nested
	@DisplayName("with a FileStore")
	class WithFileStore
	{
		@Test
		@DisplayName("uses the file-store reference (not inline base64) for a supported type")
		void usesFileStoreReference()
		{
			URI ref = URI.create("gemini://files/abc");
			FileStore store = new StubFileStore(ct -> ct.startsWith("application/pdf"), PdfFileContent.from(ref));

			ChatClient chatClient = client(store);
			chatClient.addBytes(PDF_BYTES, "application/pdf");

			UserMessage msg = chatClient.getUserMessage("hello");
			PdfFileContent pdf = firstOfType(msg, PdfFileContent.class);
			assertEquals(ref, pdf.pdfFile().url(), "should reference the uploaded file");
			assertNull(pdf.pdfFile().base64Data(), "should not be inlined as base64");
			assertTrue(hasText(msg, "hello"));
		}

		@Test
		@DisplayName("falls back to inline base64 when the store returns null")
		void fallsBackWhenStoreReturnsNull()
		{
			FileStore store = new StubFileStore(ct -> ct.startsWith("application/pdf"), null);

			ChatClient chatClient = client(store);
			chatClient.addBytes(PDF_BYTES, "application/pdf");

			UserMessage msg = chatClient.getUserMessage("hello");
			PdfFileContent pdf = firstOfType(msg, PdfFileContent.class);
			assertNotNull(pdf.pdfFile().base64Data(), "should be inlined as base64");
			assertNull(pdf.pdfFile().url());
		}

		@Test
		@DisplayName("keeps text content inline even when a store is present")
		void textStaysInline()
		{
			FileStore store = new StubFileStore(ct -> true, PdfFileContent.from(URI.create("gemini://x")));

			ChatClient chatClient = client(store);
			chatClient.addBytes("just some text".getBytes(), "text/plain");

			UserMessage msg = chatClient.getUserMessage("hello");
			assertNull(firstOfTypeOrNull(msg, PdfFileContent.class), "text must not be routed through the store");
			assertTrue(hasText(msg, "just some text"));
			assertTrue(hasText(msg, "hello"));
		}

		@Test
		@DisplayName("falls back to inline base64 when the store does not support the type")
		void fallsBackWhenUnsupported()
		{
			FileStore store = new StubFileStore(ct -> false, PdfFileContent.from(URI.create("gemini://x")));

			ChatClient chatClient = client(store);
			chatClient.addBytes(PDF_BYTES, "application/pdf");

			UserMessage msg = chatClient.getUserMessage("hello");
			PdfFileContent pdf = firstOfType(msg, PdfFileContent.class);
			assertNotNull(pdf.pdfFile().base64Data(), "should be inlined as base64");
			assertNull(pdf.pdfFile().url());
		}
	}

	@Nested
	@DisplayName("without a FileStore")
	class WithoutFileStore
	{
		@Test
		@DisplayName("inlines a pdf as base64 (unchanged inline behaviour)")
		void inlinesWhenNoStore()
		{
			ChatClient chatClient = client(null);
			chatClient.addBytes(PDF_BYTES, "application/pdf");

			UserMessage msg = chatClient.getUserMessage("hello");
			PdfFileContent pdf = firstOfType(msg, PdfFileContent.class);
			assertNotNull(pdf.pdfFile().base64Data());
			assertNull(pdf.pdfFile().url());
		}

		@Test
		@DisplayName("3-arg constructor produces a client with no file store (inline)")
		void threeArgConstructorInlines()
		{
			ChatClient chatClient = new ChatClient(assistant, access, null);
			chatClient.addBytes(PDF_BYTES, "application/pdf");

			UserMessage msg = chatClient.getUserMessage("hello");
			assertNotNull(firstOfType(msg, PdfFileContent.class).pdfFile().base64Data());
		}

		@Test
		@DisplayName("plain user message with no files has a single text content")
		void plainMessage()
		{
			ChatClient chatClient = client(null);
			UserMessage msg = chatClient.getUserMessage("hello");
			assertTrue(hasText(msg, "hello"));
		}
	}

	private static <T extends Content> T firstOfType(UserMessage msg, Class<T> type)
	{
		T found = firstOfTypeOrNull(msg, type);
		assertNotNull(found, "expected a " + type.getSimpleName() + " in the message");
		return found;
	}

	private static <T extends Content> T firstOfTypeOrNull(UserMessage msg, Class<T> type)
	{
		for (Content c : msg.contents())
		{
			if (type.isInstance(c)) return type.cast(c);
		}
		return null;
	}

	private static boolean hasText(UserMessage msg, String text)
	{
		return msg.contents().stream()
			.filter(c -> c.type() == ContentType.TEXT)
			.map(TextContent.class::cast)
			.anyMatch(t -> text.equals(t.text()));
	}

	private static final class StubFileStore implements FileStore
	{
		private final java.util.function.Predicate<String> supports;
		private final Content result;

		StubFileStore(java.util.function.Predicate<String> supports, Content result)
		{
			this.supports = supports;
			this.result = result;
		}

		@Override
		public Content upload(byte[] bytes, String contentType, String fileName)
		{
			return result;
		}

		@Override
		public boolean supports(String contentType)
		{
			return contentType != null && supports.test(contentType);
		}
	}
}
