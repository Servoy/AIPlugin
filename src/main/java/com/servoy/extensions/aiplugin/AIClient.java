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
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;



@ServoyDocumented(scriptingName = "AIClient")
public class AIClient implements IScriptable, IJavaScriptType {

	private final StreamingChatModel model;
	private final IClientPluginAccess access;
	private final List<Pair<Object, String>> files = new ArrayList<>();

	public AIClient(StreamingChatModel model, IClientPluginAccess access) {
		this.model = model;
		this.access = access;
	}

	/**
	 * @param file A JSFile or String (path to a file on the server)
	 * @return
	 */
	@JSFunction
	public AIClient addFile(Object file) {
		files.add(Pair.create(file, null));
		return this;
	}

	/**
	 * @param file A JSFile or String (path to a file on the server)
	 * @param contentType the content the file, must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 * @return
	 */
	@JSFunction
	public AIClient addFile(Object file, String contentType) {
		files.add(Pair.create(file, contentType));
		return this;
	}

	/**
	 * @param bytes The bytes of that must be send. The content type will be detected automatically.
	 * @return
	 */
	@JSFunction
	public AIClient addBytes(byte[] bytes) {
		files.add(Pair.create(bytes, null));
		return this;
	}

	/**
	 * @param bytes The bytes of that must be send. The content type will be detected automatically.
	 * @param contentType the content the file, must be a image/*, video/*, audio/*, application/pdf or text/ content type
	 * @return
	 */
	@JSFunction
	public AIClient addBytes(byte[] bytes, String contentType) {
		files.add(Pair.create(bytes, contentType));
		return this;
	}

	@JSFunction
	public NativePromise chat(String userMessage) {
		Deferred deferred = new Deferred(access);
		StringBuilder repsonse = new StringBuilder();
		UserMessage msg = getUserMessage(userMessage);
		model.chat(ChatRequest.builder().messages(msg).build(), new StreamingChatResponseHandler() {

			@Override
			public void onPartialResponse(String partialResponse) {
				repsonse.append(partialResponse);
			}

			@Override
			public void onCompleteResponse(ChatResponse completeResponse) {
				deferred.resolve(repsonse.toString());
			}

			@Override
			public void onError(Throwable error) {
				deferred.reject(error);
			}

		});
		return deferred.getPromise();
	}
	
	@JSFunction
	public void chat(String userMessage, Function partialRespose, Function onComplete, Function onError) {
		UserMessage msg = getUserMessage(userMessage);
		
		StringBuilder repsonse = new StringBuilder();
		
		FunctionDefinition fdPartialRespose = partialRespose != null ? new FunctionDefinition(partialRespose) : null;
		FunctionDefinition fdOnComplete = onComplete != null ? new FunctionDefinition(onComplete) : null;
		FunctionDefinition fdOnError = onError != null ? new FunctionDefinition(onError) : null;
		model.chat(ChatRequest.builder().messages(msg).build(), new StreamingChatResponseHandler() {
			@Override
			public void onPartialResponse(String partialResponse) {
				if (fdPartialRespose != null) {
					fdPartialRespose.executeAsync(access, new Object[] { partialResponse });
				}
				repsonse.append(partialResponse);
			}

			@Override
			public void onCompleteResponse(ChatResponse completeResponse) {
				if (fdOnComplete != null) {
					fdOnComplete.executeAsync(access, new Object[] { userMessage, repsonse.toString() } );
				}
			}

			@Override
			public void onError(Throwable error) {
				if (fdOnError != null) {
					fdOnError.executeAsync(access, new Object[] { error });
				}
			}
		});
		
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
