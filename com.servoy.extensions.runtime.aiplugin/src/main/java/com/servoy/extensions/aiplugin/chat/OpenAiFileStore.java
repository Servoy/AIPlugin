package com.servoy.extensions.aiplugin.chat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.servoy.j2db.util.Debug;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.PdfFileContent;

/**
 * A {@link FileStore} backed by the OpenAI Files API.
 * <p>
 * Files are uploaded once via {@link OpenAIClient#files()} ({@code files().create(...)}) with
 * {@link FilePurpose#USER_DATA}; the returned {@code file_id} is referenced in the chat request
 * as an {@code input_file} part rather than being inlined as base64. The reference is carried on a
 * {@link PdfFileContent} built from an {@code openai-file://<id>} URI, which the vendored
 * {@code com.servoy.extensions.aiplugin.chat.openai.OpenAiFilesResponsesStreamingChatModel}
 * recognises and maps to {@code ResponseInputFile.fileId(id)}.
 * </p>
 * <p>
 * Only {@code application/pdf} is uploaded for now; other content types fall back to inline base64
 * by returning {@code false} from {@link #supports(String)}. Uploaded files persist until deleted
 * and are best-effort deleted when the {@link ChatClient} is closed (see {@link #close()}).
 * </p>
 * <p>
 * This store owns the shared {@link OpenAIClient} (also used by the chat model), so {@link #close()}
 * closes it to release the underlying OkHttp connection pool and dispatcher threads.
 * </p>
 */
public class OpenAiFileStore implements FileStore, AutoCloseable
{
	/**
	 * URI scheme used to carry an uploaded OpenAI {@code file_id} on a {@link PdfFileContent}. The
	 * vendored {@code OpenAiFilesResponsesStreamingChatModel} reads it back via {@link #extractFileId}
	 * and maps it to {@code ResponseInputFile.fileId(id)}. Kept here so the mint and parse sides share
	 * one definition and cannot drift.
	 */
	public static final String FILE_ID_SCHEME = "openai-file";

	private final OpenAIClient client;
	private final List<String> uploadedFileIds = new ArrayList<>();

	OpenAiFileStore(OpenAIClient client)
	{
		this.client = client;
	}

	/**
	 * Builds the marker {@link PdfFileContent} that references an uploaded OpenAI file by its id.
	 */
	static PdfFileContent toFileReference(String fileId)
	{
		return PdfFileContent.from(URI.create(FILE_ID_SCHEME + "://" + fileId));
	}

	/**
	 * Returns the OpenAI {@code file_id} carried on the given content, or {@code null} when the content
	 * is not an {@code openai-file://<id>} marker (in which case the original url/base64 handling applies).
	 */
	public static String extractFileId(PdfFileContent pdfFileContent)
	{
		if (pdfFileContent == null) return null;
		var pdfFile = pdfFileContent.pdfFile();
		if (pdfFile == null || pdfFile.url() == null) return null;
		URI uri = pdfFile.url();
		if (!FILE_ID_SCHEME.equals(uri.getScheme())) return null;
		String id = uri.getSchemeSpecificPart();
		if (id != null && id.startsWith("//")) id = id.substring(2);
		return id != null && !id.isEmpty() ? id : null;
	}

	@Override
	public boolean supports(String contentType)
	{
		if (contentType == null) return false;
		return contentType.startsWith("application/pdf");
	}

	@Override
	public Content upload(byte[] bytes, String contentType, String fileName)
	{
		if (!supports(contentType)) return null;
		try
		{
			FileObject file = client.files().create(FileCreateParams.builder()
				.file(bytes)
				.purpose(FilePurpose.USER_DATA)
				.build());
			String id = file.id();
			if (id == null || id.isEmpty()) return null;
			synchronized (uploadedFileIds)
			{
				uploadedFileIds.add(id);
			}
			return toFileReference(id);
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException) Thread.currentThread().interrupt();
			Debug.error("Could not upload file '" + fileName + "' to the OpenAI Files API, falling back to inline base64.", e);
			return null;
		}
	}

	@Override
	public void close()
	{
		List<String> ids;
		synchronized (uploadedFileIds)
		{
			ids = new ArrayList<>(uploadedFileIds);
			uploadedFileIds.clear();
		}
		for (String id : ids)
		{
			try
			{
				client.files().delete(id);
			}
			catch (Exception e)
			{
				Debug.log("Could not delete uploaded OpenAI file '" + id + "'.", e);
			}
		}
		try
		{
			client.close();
		}
		catch (Exception e)
		{
			Debug.log("Could not close the OpenAI client.", e);
		}
	}
}
