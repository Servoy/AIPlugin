package com.servoy.extensions.aiplugin.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.model.googleai.GeminiFiles;
import dev.langchain4j.model.googleai.GeminiFiles.GeminiFile;

@DisplayName("GeminiFileStore")
class GeminiFileStoreTest
{
	private static final String API_KEY = "test-key";
	private static final byte[] BYTES = new byte[] { 1, 2, 3, 4 };

	private static GeminiFile file(String name, String uri, String state)
	{
		return new GeminiFile(name, "display", "application/pdf", 4L, "c", "u", "e", "sha", uri, state);
	}

	private static GeminiFile activeFile(String name, String uri)
	{
		return file(name, uri, "ACTIVE");
	}

	private static GeminiFile processingFile(String name, String uri)
	{
		return file(name, uri, "PROCESSING");
	}

	/**
	 * Runs the given action with a mocked GeminiFiles so no network call is made. The supplied
	 * GeminiFiles mock is returned by the (mocked) static builder chain used by the GeminiFileStore
	 * constructor.
	 */
	private static void withMockedGeminiFiles(GeminiFiles geminiFiles, ThrowingConsumer action) throws Exception
	{
		GeminiFiles.Builder builder = mock(GeminiFiles.Builder.class, RETURNS_SELF);
		when(builder.build()).thenReturn(geminiFiles);
		try (var mocked = mockStatic(GeminiFiles.class))
		{
			mocked.when(GeminiFiles::builder).thenReturn(builder);
			action.accept(new GeminiFileStore(API_KEY, null));
		}
	}

	@FunctionalInterface
	private interface ThrowingConsumer
	{
		void accept(GeminiFileStore store) throws Exception;
	}

	@Nested
	@DisplayName("supports()")
	class Supports
	{
		@ParameterizedTest
		@ValueSource(strings = { "audio/mpeg", "audio/wav", "video/mp4", "video/webm", "application/pdf" })
		@DisplayName("returns true for audio/*, video/* and application/pdf")
		void returnsTrueForUploadableTypes(String contentType) throws Exception
		{
			withMockedGeminiFiles(mock(GeminiFiles.class), store -> assertTrue(store.supports(contentType)));
		}

		@ParameterizedTest
		@ValueSource(strings = { "image/png", "image/jpeg", "text/plain", "text/csv", "application/octet-stream", "application/json" })
		@DisplayName("returns false for image/*, text/* and other unsupported types")
		void returnsFalseForUnsupportedTypes(String contentType) throws Exception
		{
			withMockedGeminiFiles(mock(GeminiFiles.class), store -> assertFalse(store.supports(contentType)));
		}

		@ParameterizedTest
		@NullSource
		@DisplayName("returns false for null content type")
		void returnsFalseForNull(String contentType) throws Exception
		{
			withMockedGeminiFiles(mock(GeminiFiles.class), store -> assertFalse(store.supports(contentType)));
		}
	}

	@Nested
	@DisplayName("upload() success")
	class UploadSuccess
	{
		@Test
		@DisplayName("returns PdfFileContent whose URI is the Gemini file URI (not base64) for application/pdf")
		void uploadsPdfAsFileReference() throws Exception
		{
			String fileUri = "https://generativelanguage.googleapis.com/v1beta/files/pdf123";
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), eq("application/pdf"), anyString()))
				.thenReturn(activeFile("files/pdf123", fileUri));

			withMockedGeminiFiles(geminiFiles, store -> {
				Content content = store.upload(BYTES, "application/pdf", "doc.pdf");
				PdfFileContent pdf = assertInstanceOf(PdfFileContent.class, content);
				assertEquals(URI.create(fileUri), pdf.pdfFile().url());
				assertNull(pdf.pdfFile().base64Data(), "should be a file reference, not inline base64");
			});
		}

		@Test
		@DisplayName("returns AudioContent whose URI is the Gemini file URI for audio/*")
		void uploadsAudioAsFileReference() throws Exception
		{
			String fileUri = "https://generativelanguage.googleapis.com/v1beta/files/aud123";
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), eq("audio/mpeg"), anyString()))
				.thenReturn(activeFile("files/aud123", fileUri));

			withMockedGeminiFiles(geminiFiles, store -> {
				Content content = store.upload(BYTES, "audio/mpeg", "clip.mp3");
				AudioContent audio = assertInstanceOf(AudioContent.class, content);
				assertEquals(URI.create(fileUri), audio.audio().url());
				assertNull(audio.audio().base64Data(), "should be a file reference, not inline base64");
			});
		}

		@Test
		@DisplayName("returns VideoContent whose URI is the Gemini file URI for video/*")
		void uploadsVideoAsFileReference() throws Exception
		{
			String fileUri = "https://generativelanguage.googleapis.com/v1beta/files/vid123";
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), eq("video/mp4"), anyString()))
				.thenReturn(activeFile("files/vid123", fileUri));

			withMockedGeminiFiles(geminiFiles, store -> {
				Content content = store.upload(BYTES, "video/mp4", "clip.mp4");
				VideoContent video = assertInstanceOf(VideoContent.class, content);
				assertEquals(URI.create(fileUri), video.video().url());
				assertNull(video.video().base64Data(), "should be a file reference, not inline base64");
			});
		}

		@Test
		@DisplayName("uses a default file name when fileName is null")
		void usesDefaultNameWhenNull() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), eq("application/pdf"), eq("file")))
				.thenReturn(activeFile("files/x", "gemini://files/x"));

			withMockedGeminiFiles(geminiFiles, store -> {
				assertNotNull(store.upload(BYTES, "application/pdf", null));
				verify(geminiFiles).uploadFile(any(), eq("application/pdf"), eq("file"));
			});
		}
	}

	@Nested
	@DisplayName("upload() fallback (returns null, no upload)")
	class UploadFallback
	{
		@Test
		@DisplayName("returns null and does not call uploadFile for image/*")
		void imageFallsBack() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			withMockedGeminiFiles(geminiFiles, store -> {
				assertNull(store.upload(BYTES, "image/png", "pic.png"));
				verify(geminiFiles, never()).uploadFile(any(), anyString(), anyString());
			});
		}

		@Test
		@DisplayName("returns null and does not call uploadFile for text/*")
		void textFallsBack() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			withMockedGeminiFiles(geminiFiles, store -> {
				assertNull(store.upload(BYTES, "text/plain", "note.txt"));
				verify(geminiFiles, never()).uploadFile(any(), anyString(), anyString());
			});
		}

		@Test
		@DisplayName("returns null and does not call uploadFile for null content type")
		void nullTypeFallsBack() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			withMockedGeminiFiles(geminiFiles, store -> {
				assertNull(store.upload(BYTES, null, "unknown"));
				verify(geminiFiles, never()).uploadFile(any(), anyString(), anyString());
			});
		}

		@Test
		@DisplayName("returns null when uploadFile throws")
		void uploadThrowsFallsBack() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), anyString(), anyString()))
				.thenThrow(new RuntimeException("boom"));

			withMockedGeminiFiles(geminiFiles, store -> assertNull(store.upload(BYTES, "application/pdf", "doc.pdf")));
		}

		@Test
		@DisplayName("returns null when the uploaded file never becomes ACTIVE")
		@Timeout(45)
		void neverActiveFallsBack() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			GeminiFile failed = file("files/f", "gemini://files/f", "FAILED");
			when(geminiFiles.uploadFile(any(), anyString(), anyString())).thenReturn(failed);

			withMockedGeminiFiles(geminiFiles, store -> assertNull(store.upload(BYTES, "application/pdf", "doc.pdf")));
		}
	}

	@Nested
	@DisplayName("upload() polling")
	class UploadPolling
	{
		@Test
		@DisplayName("polls getMetadata until ACTIVE and then succeeds")
		@Timeout(10)
		void pollsUntilActive() throws Exception
		{
			String fileUri = "https://generativelanguage.googleapis.com/v1beta/files/proc";
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), anyString(), anyString()))
				.thenReturn(processingFile("files/proc", fileUri));
			when(geminiFiles.getMetadata("files/proc"))
				.thenReturn(activeFile("files/proc", fileUri));

			withMockedGeminiFiles(geminiFiles, store -> {
				Content content = store.upload(BYTES, "application/pdf", "doc.pdf");
				assertInstanceOf(PdfFileContent.class, content);
				verify(geminiFiles, atLeastOnce()).getMetadata("files/proc");
			});
		}
	}

	@Nested
	@DisplayName("close()")
	class Close
	{
		@Test
		@DisplayName("deletes each uploaded file by name")
		void deletesUploadedFiles() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), eq("application/pdf"), anyString()))
				.thenReturn(activeFile("files/one", "gemini://files/one"));
			when(geminiFiles.uploadFile(any(), eq("audio/mpeg"), anyString()))
				.thenReturn(activeFile("files/two", "gemini://files/two"));

			withMockedGeminiFiles(geminiFiles, store -> {
				store.upload(BYTES, "application/pdf", "a.pdf");
				store.upload(BYTES, "audio/mpeg", "b.mp3");
				store.close();
				verify(geminiFiles).deleteFile("files/one");
				verify(geminiFiles).deleteFile("files/two");
			});
		}

		@Test
		@DisplayName("does not throw when deleteFile throws")
		void swallowsDeleteFailure() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			when(geminiFiles.uploadFile(any(), eq("application/pdf"), anyString()))
				.thenReturn(activeFile("files/one", "gemini://files/one"));
			doThrow(new RuntimeException("delete failed")).when(geminiFiles).deleteFile(anyString());

			withMockedGeminiFiles(geminiFiles, store -> {
				store.upload(BYTES, "application/pdf", "a.pdf");
				assertDoesNotThrow(store::close);
			});
		}

		@Test
		@DisplayName("does nothing when no files were uploaded")
		void noOpWhenEmpty() throws Exception
		{
			GeminiFiles geminiFiles = mock(GeminiFiles.class);
			withMockedGeminiFiles(geminiFiles, store -> {
				assertDoesNotThrow(store::close);
				verify(geminiFiles, never()).deleteFile(anyString());
			});
		}
	}
}
