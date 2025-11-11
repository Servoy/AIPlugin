package com.servoy.extensions.aiplugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IFile;
import com.servoy.j2db.scripting.Deferred;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;


/**
 * This is the ChatClient class that wraps around a LLM to chat with it.
 * It has support for adding files (images, videos, audio, pdf, text) to the chat.
 */
@ServoyDocumented(scriptingName = "ChatClient")
public class ChatClient implements IScriptable, IJavaScriptType {

	private final Assistant assistant;
	private final IClientPluginAccess access;
	private final List<Pair<Object, String>> files = new ArrayList<>();

	public ChatClient(Assistant assistant, IClientPluginAccess access) {
		this.assistant = assistant;
		this.access = access;
	}

	/**
	 * Add a file (String or JSFile) to the chat message.
	 * The file must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 * This will be guessed based on the file extension or content.
	 * 
	 * @param file A JSFile or String (path to a file on the server)
	 * @return The this
	 */
	@JSFunction
	public ChatClient addFile(Object file) {
		files.add(Pair.create(file, null));
		return this;
	}

	/**
	 * Add a file (String or JSFile) to the chat message.
	 * The file must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 * 
	 * @param file A JSFile or String (path to a file on the server)
	 * @param contentType the content the file, must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 * @return The this
	 */
	@JSFunction
	public ChatClient addFile(Object file, String contentType) {
		files.add(Pair.create(file, contentType));
		return this;
	}

	/**
	 * Adds a file by bytes to the chat message.
	 * The bytes must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 * 
	 * @param bytes The bytes of that must be send. The content type will be detected automatically.
	 * @return The this
	 */
	@JSFunction
	public ChatClient addBytes(byte[] bytes) {
		files.add(Pair.create(bytes, null));
		return this;
	}

	/**
	 * Adds a file by bytes to the chat message.
	 * The bytes must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 *
	 * @param bytes The bytes of that must be send. The content type will be detected automatically.
	 * @param contentType the content the file, must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 * @return The this
	 */
	@JSFunction
	public ChatClient addBytes(byte[] bytes, String contentType) {
		files.add(Pair.create(bytes, contentType));
		return this;
	}

	/**
	 * Send a userMessage to the ai. This will return a promise that will be resolved with the response.
	 * This respose is the ChatResponse object or it will be rejected with an error.
	 * 
	 * @param userMessage The user message
	 * @return A promise that will be resolved with the assistant response.
	 */
	@JSFunction
	public NativePromise chat(String userMessage) {
		Deferred deferred = new Deferred(access);
		StringBuilder response = new StringBuilder();
		UserMessage msg = getUserMessage(userMessage);
		assistant.chat(msg)
			.onPartialResponse(partialResponse -> response.append(partialResponse))
			.onCompleteResponse(completeResponse ->  deferred.resolve(new ChatResponse(userMessage,completeResponse, response.toString()) ))
			.onError(error -> deferred.reject(error)).start(); 
		return deferred.getPromise();
	}
	
	/**
	 * Send a userMessage to the ai. This will call the provided functions on partial response, complete response and error.
	 * So this can be used for streaming responses.
	 * 
	 * @param userMessage The user message send to the ai.
	 * @param partialResponse A function that will be called with each partial response from the ai.
	 * @param onComplete A function that will be called when the response is complete, it will be called with the ChatResponse object.
	 * @param onError A function that will be called when an error occurs.
	 */
	@JSFunction
	public void chat(String userMessage, Function partialResponse, Function onComplete, Function onError) {
		UserMessage msg = getUserMessage(userMessage);
		
		StringBuilder response = new StringBuilder();
		
		FunctionDefinition fdPartialResponse = partialResponse != null ? new FunctionDefinition(partialResponse) : null;
		FunctionDefinition fdOnComplete = onComplete != null ? new FunctionDefinition(onComplete) : null;
		FunctionDefinition fdOnError = onError != null ? new FunctionDefinition(onError) : null;
		assistant.chat(msg)
			.onPartialResponse(pResonse -> {
				if (fdPartialResponse != null) {
					fdPartialResponse.executeAsync(access, new Object[] { pResonse });
				}
				response.append(pResonse);
			})
			.onCompleteResponse(completeResponse -> {
				if (fdOnComplete != null) {
					fdOnComplete.executeAsync(access, new Object[] { new ChatResponse(userMessage, completeResponse, response.toString()) } );
				}
			})
			.onError(error -> {
				if (fdOnError != null) {
					fdOnError.executeAsync(access, new Object[] { error });
				}
			}).start(); 
	}

	/**
	 * @param userMessage
	 * @return
	 */
	public UserMessage getUserMessage(String userMessage) {
		UserMessage msg;
		if (files.size() > 0) {
			List<Content> list = files.stream().map(pair -> createContent(pair.getLeft(), pair.getRight()))
					.collect(Collectors.toCollection(ArrayList::new));
			list.add(TextContent.from(userMessage));
			msg = new UserMessage(list);
		} else {
			msg = new UserMessage(userMessage);
		}
		return msg;
	}

	private Content createContent(Object fileOrBytes, String contenttType) {
		String ct = getContentType(fileOrBytes, contenttType);
		if (ct == null || ct.startsWith("text/")) {
			try {
				if (fileOrBytes instanceof byte[] bytes)
					return TextContent.from(new String(bytes, Charset.forName("UTF-8")));
				return TextContent.from(Utils.getTXTFileContent(((IFile)fileOrBytes).getInputStream(), Charset.forName("UTF-8")));
			} catch (IOException e) {
				throw new RuntimeException("Could not read file " + fileOrBytes, e);
			}
		}
		if (ct.startsWith("image/")) {
			return ImageContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}
		if (ct.startsWith("video/")) {
			return VideoContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}
		if (ct.startsWith("audio/")) {
			return AudioContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}
		if (ct.startsWith("application/pdf")) {
			return PdfFileContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}

		throw new IllegalArgumentException("File content type not supported: " + ct + " of the file: " + fileOrBytes);

	}

	private byte[] getBytes(Object fileOrBytes) {
		if (fileOrBytes instanceof byte[] bytes)
			return bytes;
		if (fileOrBytes instanceof IFile file)
			try {
				return Utils.getBytesFromInputStream(file.getInputStream());
			} catch (IOException e) {
				throw new RuntimeException("Could not read file " + fileOrBytes, e);
			}
		if (fileOrBytes instanceof String fileName) {
			return Utils.readFile(new File(fileName), -1);
		}
		return null;
	}

	private String getContentType(Object fileOrBytes, String contenttType) {
		if (contenttType != null)
			return contenttType;
		if (fileOrBytes instanceof String fileName) {
			return MimeTypes.getContentType(Utils.readFile(new File(fileName), 32), fileName);
		}
		if (fileOrBytes instanceof IFile file) {
			return file.getContentType();
		}
		if (fileOrBytes instanceof byte[] bytes) {
			return MimeTypes.getContentType(bytes);
		}
		return null;
	}

}
