package com.servoy.extensions.aiplugin.chat;

import dev.langchain4j.data.message.Content;

/**
 * Represents a provider capability to persist a file once and reference it in a
 * chat request, instead of inlining the file bytes as base64 on every request.
 * <p>
 * A provider delegate that has such a capability supplies a {@code FileStore} to
 * the {@link ChatClient}; providers without one supply {@code null} and keep the
 * inline-base64 behaviour.
 * </p>
 */
interface FileStore
{
	/**
	 * Uploads the given bytes to the provider file store and returns a LangChain4j
	 * {@link Content} that references the uploaded file (so the chat request carries
	 * a file reference rather than inline bytes).
	 *
	 * @param bytes       the file bytes to upload
	 * @param contentType the content type of the file
	 * @param fileName    a display name for the file (may be used by the provider)
	 * @return a {@link Content} referencing the uploaded file, or {@code null} if the
	 *         file could not be referenced through the provider file store and the
	 *         caller should fall back to inline base64
	 */
	Content upload(byte[] bytes, String contentType, String fileName);

	/**
	 * Indicates whether files of the given content type should be uploaded to the
	 * provider file store. Content types that return {@code false} fall back to
	 * inline base64.
	 *
	 * @param contentType the content type to check
	 * @return {@code true} if the content type should be uploaded, {@code false} otherwise
	 */
	boolean supports(String contentType);
}
