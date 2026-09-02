package com.servoy.extensions.aiplugin.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.util.Debug;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.openai.client.OpenAIClient;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.services.blocking.FileService;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.PdfFileContent;

@DisplayName("OpenAiFileStore")
class OpenAiFileStoreTest
{
	private static final byte[] BYTES = new byte[] { 1, 2, 3, 4 };

	private OpenAIClient client;
	private FileService files;
	private OpenAiFileStore store;

	@BeforeEach
	void setUp()
	{
		client = mock(OpenAIClient.class);
		files = mock(FileService.class);
		when(client.files()).thenReturn(files);
		store = new OpenAiFileStore(client);
	}

	private static FileObject fileWithId(String id)
	{
		return FileObject.builder()
			.id(id)
			.bytes(0L)
			.createdAt(0L)
			.filename("file")
			.purpose(FileObject.Purpose.USER_DATA)
			.status(FileObject.Status.PROCESSED)
			.build();
	}

	@Nested
	@DisplayName("supports()")
	class Supports
	{
		@ParameterizedTest
		@ValueSource(strings = { "application/pdf" })
		@DisplayName("returns true for application/pdf")
		void returnsTrueForPdf(String contentType)
		{
			assertTrue(store.supports(contentType));
		}

		@ParameterizedTest
		@ValueSource(strings = { "image/png", "image/jpeg", "audio/mpeg", "audio/wav", "video/mp4", "video/webm", "text/plain", "text/csv", "application/octet-stream", "application/json" })
		@DisplayName("returns false for image/*, audio/*, video/*, text/* and other unsupported types")
		void returnsFalseForUnsupportedTypes(String contentType)
		{
			assertFalse(store.supports(contentType));
		}

		@ParameterizedTest
		@NullSource
		@DisplayName("returns false for null content type")
		void returnsFalseForNull(String contentType)
		{
			assertFalse(store.supports(contentType));
		}
	}

	@Nested
	@DisplayName("upload() success")
	class UploadSuccess
	{
		@Test
		@DisplayName("returns PdfFileContent referencing openai-file://<id> (not base64) for application/pdf")
		void uploadsPdfAsFileReference()
		{
			when(files.create(any(FileCreateParams.class))).thenReturn(fileWithId("file-abc123"));

			Content content = store.upload(BYTES, "application/pdf", "doc.pdf");

			PdfFileContent pdf = assertInstanceOf(PdfFileContent.class, content);
			assertEquals(URI.create("openai-file://file-abc123"), pdf.pdfFile().url());
			assertNull(pdf.pdfFile().base64Data(), "should be a file reference, not inline base64");
		}
	}

	@Nested
	@DisplayName("upload() fallback (returns null, no upload)")
	class UploadFallback
	{
		@ParameterizedTest
		@ValueSource(strings = { "image/png", "audio/mpeg", "video/mp4", "text/plain" })
		@DisplayName("returns null and does not call files().create for unsupported types")
		void unsupportedTypesFallBack(String contentType)
		{
			assertNull(store.upload(BYTES, contentType, "file"));
			verify(files, never()).create(any(FileCreateParams.class));
		}

		@ParameterizedTest
		@NullSource
		@DisplayName("returns null and does not call files().create for null content type")
		void nullTypeFallsBack(String contentType)
		{
			assertNull(store.upload(BYTES, contentType, "file"));
			verify(files, never()).create(any(FileCreateParams.class));
		}

		@Test
		@DisplayName("returns null when files().create throws")
		void createThrowsFallsBack()
		{
			when(files.create(any(FileCreateParams.class))).thenThrow(new RuntimeException("boom"));

			assertNull(store.upload(BYTES, "application/pdf", "doc.pdf"));
		}

		@Test
		@DisplayName("returns null and tracks nothing when a successful create yields an empty id")
		void emptyIdFallsBack()
		{
			when(files.create(any(FileCreateParams.class))).thenReturn(fileWithId(""));

			assertNull(store.upload(BYTES, "application/pdf", "doc.pdf"));

			store.close();
			verify(files, never()).delete(anyString());
		}
	}

	@Nested
	@DisplayName("close()")
	class Close
	{
		@Test
		@DisplayName("deletes each uploaded file by id")
		void deletesUploadedFiles()
		{
			when(files.create(any(FileCreateParams.class)))
				.thenReturn(fileWithId("file-one"))
				.thenReturn(fileWithId("file-two"));

			store.upload(BYTES, "application/pdf", "a.pdf");
			store.upload(BYTES, "application/pdf", "b.pdf");
			store.close();

			verify(files).delete(eq("file-one"));
			verify(files).delete(eq("file-two"));
		}

		@Test
		@DisplayName("does not throw when delete throws")
		void swallowsDeleteFailure()
		{
			when(files.create(any(FileCreateParams.class))).thenReturn(fileWithId("file-one"));
			doThrow(new RuntimeException("delete failed")).when(files).delete(anyString());

			store.upload(BYTES, "application/pdf", "a.pdf");
			assertDoesNotThrow(store::close);
		}

		@Test
		@DisplayName("does nothing when no files were uploaded")
		void noOpWhenEmpty()
		{
			assertDoesNotThrow(store::close);
			verify(files, never()).delete(anyString());
		}

		@Test
		@DisplayName("closes the shared OpenAI client")
		void closesClient()
		{
			store.close();
			verify(client).close();
		}

		@Test
		@DisplayName("closes the client even when a delete throws")
		void closesClientDespiteDeleteFailure()
		{
			when(files.create(any(FileCreateParams.class))).thenReturn(fileWithId("file-one"));
			doThrow(new RuntimeException("delete failed")).when(files).delete(anyString());

			store.upload(BYTES, "application/pdf", "a.pdf");
			assertDoesNotThrow(store::close);
			verify(client).close();
		}

		@Test
		@DisplayName("deletes nothing after a failed upload (only successful uploads are tracked)")
		void noDeleteAfterFailedUpload()
		{
			when(files.create(any(FileCreateParams.class))).thenThrow(new RuntimeException("boom"));

			assertNull(store.upload(BYTES, "application/pdf", "doc.pdf"));
			store.close();

			verify(files, never()).delete(anyString());
		}
	}
}
