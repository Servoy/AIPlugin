package com.servoy.extensions.aiplugin.chat;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import com.servoy.j2db.util.Debug;
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
 * This is the ChatClient class that wraps around a LLM to chat with it. It has
 * support for adding files (images, videos, audio, pdf, text) to the chat.
 */
@ServoyDocumented
public class ChatClient implements IScriptable, IJavaScriptType
{

	private final Assistant assistant;
	private final IClientPluginAccess access;
	private final List<Pair<Object, String>> files = new ArrayList<>();
	private final List< ? extends AutoCloseable> closeables;

	public ChatClient(Assistant assistant, IClientPluginAccess access, List< ? extends AutoCloseable> closeables)
	{
		this.assistant = assistant;
		this.access = access;
		this.closeables = closeables;
	}

	/**
	 * Add a file (String or JSFile) to the chat message. The file must be a
	 * image/*, video/*, audio/*, application/pdf or text/ content type This will be
	 * guessed based on the file extension or content.
	 *
	 * @param file A JSFile or String (path to a file on the server)
	 * @return The this
	 */
	@JSFunction
	public ChatClient addFile(Object file)
	{
		files.add(Pair.create(file, null));
		return this;
	}

	/**
	 * Add a file (String or JSFile) to the chat message. The file must be a
	 * image/*, video/*, audio/*, application/pdf or text/ content type
	 *
	 * @param file        A JSFile or String (path to a file on the server)
	 * @param contentType the content the file, must be a image/*, video/*, audio/*,
	 *                    application/pdf or text/ content type
	 * @return The this
	 */
	@JSFunction
	public ChatClient addFile(Object file, String contentType)
	{
		files.add(Pair.create(file, contentType));
		return this;
	}

	/**
	 * Adds a file by bytes to the chat message. The bytes must be a image/*,
	 * video/*, audio/*, application/pdf or text/ content type
	 *
	 * @param bytes The bytes of that must be send. The content type will be
	 *              detected automatically.
	 * @return The this
	 */
	@JSFunction
	public ChatClient addBytes(byte[] bytes)
	{
		files.add(Pair.create(bytes, null));
		return this;
	}

	/**
	 * Adds a file by bytes to the chat message. The bytes must be a image/*,
	 * video/*, audio/*, application/pdf or text/ content type
	 *
	 * @param bytes       The bytes of that must be send. The content type will be
	 *                    detected automatically.
	 * @param contentType the content the file, must be a image/*, video/*, audio/*,
	 *                    application/pdf or text/ content type
	 * @return The this
	 */
	@JSFunction
	public ChatClient addBytes(byte[] bytes, String contentType)
	{
		files.add(Pair.create(bytes, contentType));
		return this;
	}

	/**
	 * Send a userMessage to the ai. This will return a promise that will be
	 * resolved with the response. This response is a Promise that will get a
	 * ChatResponse object in the then or the promise will be rejected with an
	 * error.
	 *
	 * @param userMessage The user message
	 * @return {Promise<plugins.ai.ChatResponse>} A promise that will be resolved
	 *         with the assistant response.
	 */
	@JSFunction
	public NativePromise chat(String userMessage)
	{
		Deferred deferred = new Deferred(access);
		StringBuilder response = new StringBuilder();
		UserMessage msg = getUserMessage(userMessage);
		assistant.chat(msg).onPartialResponse(partialResponse -> response.append(partialResponse))
			.onCompleteResponse(completeResponse -> deferred
				.resolve(new ChatResponse(userMessage, completeResponse, response.toString())))
			.onError(error -> deferred.reject(error)).start();
		return deferred.getPromise();
	}

	/**
	 * Send a userMessage to the ai, this call is a synchronous call and will return a ChatResponse object directly.
	 * This can throw an exception of something goes wrong.
	 *
	 * Its recommended to use the Async (Promise) version. Only use this if you directly need a respond that you need to return from a function.
	 *
	 * @param userMessage The user message
	 * @return {plugins.ai.ChatResponse} The assistant response.
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@JSFunction
	public ChatResponse chatSync(String userMessage) throws RuntimeException
	{
		StringBuilder response = new StringBuilder();
		UserMessage msg = getUserMessage(userMessage);
		CompletableFuture<ChatResponse> future = new CompletableFuture<>();
		assistant.chat(msg).onPartialResponse(partialResponse -> response.append(partialResponse))
			.onCompleteResponse(completeResponse -> future.complete(new ChatResponse(userMessage, completeResponse, response.toString())))
			.onError(error -> {
				future.completeExceptionally(error);
			}).start();
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new RuntimeException("Error while getting chat response", e);
		}
	}

	/**
	 * Send a userMessage to the ai. This will call the provided functions on
	 * partial response, complete response and error. So this can be used for
	 * streaming responses.
	 *
	 * @param userMessage     The user message send to the ai.
	 * @param partialResponse {(partialResponse:String)=>void} A function that will
	 *                        be called with each partial string response from the
	 *                        ai.
	 * @param onComplete      {(response:plugins.ai.ChatResponse)=>void} A function
	 *                        that will be called when the response is complete, it
	 *                        will be called with the ChatResponse object.
	 * @param onError         {(error:Exception)=>void} A function that will be
	 *                        called when an error occurs.
	 */
	@JSFunction
	public void chat(String userMessage, Function partialResponse, Function onComplete, Function onError)
	{
		UserMessage msg = getUserMessage(userMessage);

		StringBuilder response = new StringBuilder();

		FunctionDefinition fdPartialResponse = partialResponse != null ? new FunctionDefinition(partialResponse) : null;
		FunctionDefinition fdOnComplete = onComplete != null ? new FunctionDefinition(onComplete) : null;
		FunctionDefinition fdOnError = onError != null ? new FunctionDefinition(onError) : null;
		assistant.chat(msg).onPartialResponse(pResonse -> {
			if (fdPartialResponse != null)
			{
				fdPartialResponse.executeAsync(access, new Object[] { pResonse });
			}
			response.append(pResonse);
		}).onCompleteResponse(completeResponse -> {
			if (fdOnComplete != null)
			{
				fdOnComplete.executeAsync(access,
					new Object[] { new ChatResponse(userMessage, completeResponse, response.toString()) });
			}
		}).onError(error -> {
			if (fdOnError != null)
			{
				fdOnError.executeAsync(access, new Object[] { error });
			}
		}).start();
	}

	public UserMessage getUserMessage(String userMessage)
	{
		UserMessage msg;
		if (!files.isEmpty())
		{
			List<Content> list = files.stream().map(pair -> createContent(pair.getLeft(), pair.getRight()))
				.collect(Collectors.toCollection(ArrayList::new));
			list.add(TextContent.from(userMessage));
			msg = new UserMessage(list);
		}
		else
		{
			msg = new UserMessage(userMessage);
		}
		return msg;
	}

	private Content createContent(Object fileOrBytes, String contenttType)
	{
		String ct = getContentType(fileOrBytes, contenttType);
		if (ct == null || ct.startsWith("text/"))
		{
			try
			{
				if (fileOrBytes instanceof byte[] bytes)
					return TextContent.from(new String(bytes, UTF_8));
				return TextContent.from(Utils.getTXTFileContent(((IFile)fileOrBytes).getInputStream(), UTF_8));
			}
			catch (IOException e)
			{
				throw new RuntimeException("Could not read file " + fileOrBytes, e);
			}
		}
		if (ct.startsWith("image/"))
		{
			return ImageContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}
		if (ct.startsWith("video/"))
		{
			return VideoContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}
		if (ct.startsWith("audio/"))
		{
			return AudioContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}
		if (ct.startsWith("application/pdf"))
		{
			return PdfFileContent.from(Base64.getEncoder().encodeToString(getBytes(fileOrBytes)), ct);
		}

		throw new IllegalArgumentException("File content type not supported: " + ct + " of the file: " + fileOrBytes);

	}

	private byte[] getBytes(Object fileOrBytes)
	{
		if (fileOrBytes instanceof byte[] bytes)
			return bytes;
		if (fileOrBytes instanceof IFile file)
			try
		{
			return Utils.getBytesFromInputStream(file.getInputStream());
		}
			catch (IOException e)
		{
			throw new RuntimeException("Could not read file " + fileOrBytes, e);
		}
		if (fileOrBytes instanceof String fileName)
		{
			return Utils.readFile(new File(fileName), -1);
		}
		return null;
	}

	private String getContentType(Object fileOrBytes, String contenttType)
	{
		if (contenttType != null)
			return contenttType;
		if (fileOrBytes instanceof String fileName)
		{
			return MimeTypes.getContentType(Utils.readFile(new File(fileName), 32), fileName);
		}
		if (fileOrBytes instanceof IFile file)
		{
			return file.getContentType();
		}
		if (fileOrBytes instanceof byte[] bytes)
		{
			return MimeTypes.getContentType(bytes);
		}
		return null;
	}

	/**
	 * Releases resources held by this chat client.<br/>
	 * Always remember to call this when you will no longer use a chat client.<br/><br/>
	 *
	 * For example a client that is configured to use remote MCP Servers needs to close the
	 * connections/streams or child processes (in case of STDIO MCP transport) of the MCP Clients.<br/><br/>
	 *
	 * It is a good idea to have .close called inside the finally of a try-catch block, as operations after the ChatClient was
	 * built might throw exceptions - and we want resources to be released properly.
	 */
	@JSFunction
	public void close()
	{
		RuntimeException[] exception = new RuntimeException[1];
		if (closeables != null) closeables.forEach((clsbl) -> {
			try
			{
				clsbl.close();
			}
			catch (Exception e)
			{
				Debug.log(e); // log all, and throw only once at the end; in this way, all closable .close() get a chance to execute
				exception[0] = new RuntimeException("Could not release resources of of one or more auto-closeables: ", e);
			}
		});
		if (exception[0] != null) throw exception[0];
	}

}