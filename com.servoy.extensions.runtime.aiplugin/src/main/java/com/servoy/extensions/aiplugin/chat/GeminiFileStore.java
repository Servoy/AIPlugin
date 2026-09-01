package com.servoy.extensions.aiplugin.chat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.util.Debug;

import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.model.googleai.GeminiFiles;
import dev.langchain4j.model.googleai.GeminiFiles.GeminiFile;

/**
 * A {@link FileStore} backed by the Google Gemini Files API.
 * <p>
 * Files are uploaded once via {@link GeminiFiles#uploadFile(byte[], String, String)}; the returned
 * {@code https} file URI is referenced in the chat request as a Gemini {@code file_data} part rather
 * than being inlined as base64. Uploaded files are retained by Gemini for ~48h and are best-effort
 * deleted when the {@link ChatClient} is closed (see {@link #close()}).
 * </p>
 * <p>
 * Audio, video and PDF references flow through LangChain4j's Gemini mapper
 * ({@code PartsAndContentsMapper}) as {@code file_data} parts when built from the {@code https}
 * file URI. Image content built from an {@code https} URI is instead downloaded and inlined by that
 * mapper (and the raw file URI requires an API key to download), so images fall back to inline
 * base64 by returning {@code null} from {@link #upload(byte[], String, String)}.
 * </p>
 */
class GeminiFileStore implements FileStore, AutoCloseable
{
	private static final long POLL_INTERVAL_MILLIS = 500;
	private static final long POLL_TIMEOUT_MILLIS = 30_000;

	private final GeminiFiles geminiFiles;
	private final List<String> uploadedFileNames = new ArrayList<>();

	GeminiFileStore(String apiKey, String baseUrl)
	{
		GeminiFiles.Builder builder = GeminiFiles.builder().apiKey(apiKey);
		if (baseUrl != null) builder.baseUrl(baseUrl);
		this.geminiFiles = builder.build();
	}

	@Override
	public boolean supports(String contentType)
	{
		if (contentType == null) return false;
		// image/* is intentionally excluded: the Gemini mapper downloads+inlines image content built
		// from an http(s) URI (and the raw file URI needs an API key to download), so it cannot be
		// referenced as a file_data part. Images therefore stay on the inline-base64 path.
		return contentType.startsWith("audio/") || contentType.startsWith("video/") || contentType.startsWith("application/pdf");
	}

	@Override
	public Content upload(byte[] bytes, String contentType, String fileName)
	{
		if (!supports(contentType)) return null;
		try
		{
			GeminiFile file = geminiFiles.uploadFile(bytes, contentType, fileName != null ? fileName : "file");
			file = awaitActive(file);
			if (file == null || !file.isActive()) return null;
			synchronized (uploadedFileNames)
			{
				uploadedFileNames.add(file.name());
			}
			URI uri = URI.create(file.uri());
			if (contentType.startsWith("audio/")) return AudioContent.from(uri);
			if (contentType.startsWith("video/")) return VideoContent.from(uri);
			if (contentType.startsWith("application/pdf")) return PdfFileContent.from(uri);
			return null;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			Debug.error("Interrupted while uploading file '" + fileName + "' to the Gemini Files API, falling back to inline base64.", e);
			return null;
		}
		catch (Exception e)
		{
			Debug.error("Could not upload file '" + fileName + "' to the Gemini Files API, falling back to inline base64.", e);
			return null;
		}
	}

	private GeminiFile awaitActive(GeminiFile file) throws InterruptedException
	{
		GeminiFile current = file;
		long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
		while (current != null && current.isProcessing() && System.currentTimeMillis() < deadline)
		{
			Thread.sleep(POLL_INTERVAL_MILLIS);
			try
			{
				current = geminiFiles.getMetadata(current.name());
			}
			catch (Exception e)
			{
				Debug.log("Could not poll Gemini file metadata for '" + current.name() + "'.", e);
				return current;
			}
		}
		return current;
	}

	@Override
	public void close()
	{
		List<String> names;
		synchronized (uploadedFileNames)
		{
			names = new ArrayList<>(uploadedFileNames);
			uploadedFileNames.clear();
		}
		for (String name : names)
		{
			try
			{
				geminiFiles.deleteFile(name);
			}
			catch (Exception e)
			{
				Debug.log("Could not delete uploaded Gemini file '" + name + "'; it will expire per Gemini's retention.", e);
			}
		}
	}
}
