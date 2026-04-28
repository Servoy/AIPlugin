package com.servoy.extensions.aiplugin.chat;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.DefaultMcpClient.Builder;
import dev.langchain4j.mcp.client.McpRoot;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

/**
 * <p>
 * This uses the builder pattern to construct & configure connections to MCP servers for the AI Agent.
 * </p>
 * <p>
 * You are required to configure at least one type of connection via a connectVia...() call in order to be able to call .build().
 * All other configurations are optional and help configure the interaction with the MCP server, if you want to override the defaults.
 * </p>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * let chatClientBuilder = plugins.ai.createGeminiChatBuilder();
 * let mcpClient = chatClientBuilder.createMCPClient()
 *         .connectViaStreamableHTTP("https://myMCPUrl")
 *         .toolExecutionTimeout(120 * 1000)
 *         .build();
 *  let chatClient = chatClientBuilder.build();
 * </pre>
 *
 * @param <T> the type of BaseChatBuilder
 * @author acostescu
 * @since 2026.6
 */
@ServoyDocumented
public class MCPClientBuilder<T extends BaseChatBuilder<T>>
{

	private final T chatBuilder;
	private final Builder mcpClientBuilder;
	private List<String> toolNameFilters = null;
	private boolean transportWasConfigured = false;

	public MCPClientBuilder(T chatBuilder)
	{
		this.chatBuilder = chatBuilder;
		mcpClientBuilder = new DefaultMcpClient.Builder();
	}

	// should we have a transport builder with the optional args exposed to Servoy scripting as well instead of
	// these connectVia... methods? it would be less code completion options on this class but more nested builder calls

	/**
	 * Connect to an MCP Server via the STDIO (communication over standard in and standard out).
	 * The client will launch an MCP Server as a sub-process and then communicate with it via the in/out streams.<br/><br/>
	 *
	 * It will not set specific environment variables for the spawned MCP Server sub-process.
	 *
	 * @param command {Array<String>} the command to be used to start the MCP Server
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaSTDIO(String[] command)
	{
		return connectViaSTDIO(command, null);
	}

	/**
	 * Connect to an MCP Server via the STDIO (communication over standard in and standard out).
	 * The client will launch an MCP Server as a sub-process and then communicate with it via the in/out streams.
	 *
	 * @param command {Array<String>} the command to be used to start the MCP Server
	 * @param environment {Object<String>} any environment variables that are needed; the key is the variable
	 *     name and the value is the value of that environment variable
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaSTDIO(String[] command, Map<String, String> environment)
	{
		if (command == null || command.length == 0) throw new RuntimeException("An actual 'command' is required for connecting to MCP Servers via STDIO");

		StdioMcpTransport.Builder transportBuilder = StdioMcpTransport.builder()
			.command(Arrays.asList(command));
		if (environment != null)
			transportBuilder.environment(environment);

		mcpClientBuilder.transport(transportBuilder.build());
		transportWasConfigured = true;

		return this;
	}

	/**
	 * Connect to an MCP Server via the StreamableHTTP.<br/><br/>
	 *
	 * the server operates as an independent process (even remotely) that can handle multiple client connections.
	 * This transport uses HTTP POST and GET requests. Server can optionally make use of Server-Sent Events (SSE) to
	 * stream multiple server messages. This permits basic MCP servers, as well as more feature-rich servers supporting
	 * streaming and server-to-client notifications and requests.<br/><br/>
	 *
	 * NOTE: the authentication part (what is required depends on the MCP server) should be prepared beforehand. Use the
	 * "headers" param. in order to give the needed access rights to the MCP server (it may be some API key or an OAuth token
	 * resulted from using the oauth plugin for example).
	 *
	 * It does not follow redirects. Subsidiary SSE channel is disabled.
	 *
	 * @param url {String} the URL of the MCP Server to connect to.
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaStreamableHTTP(String url)
	{
		return connectViaStreamableHTTP(url, null, -1, false, false, false);
	}

	/**
	 * Connect to an MCP Server via the StreamableHTTP.<br/><br/>
	 *
	 * the server operates as an independent process (even remotely) that can handle multiple client connections.
	 * This transport uses HTTP POST and GET requests. Server can optionally make use of Server-Sent Events (SSE) to
	 * stream multiple server messages. This permits basic MCP servers, as well as more feature-rich servers supporting
	 * streaming and server-to-client notifications and requests.<br/><br/>
	 *
	 * NOTE: the authentication part (what is required depends on the MCP server) should be prepared beforehand. Use the
	 * "headers" param. in order to give the needed access rights to the MCP server (it may be some API key or an OAuth token
	 * resulted from using the oauth plugin for example).
	 *
	 * It does not follow redirects. Subsidiary SSE channel is disabled.
	 *
	 * @param url {String} the URL of the MCP Server to connect to.
	 * @param headers {Object<String>} any request headers that need to be given to the MCP Server. The object contains String keys that are header names -> the values of those headers.
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaStreamableHTTP(String url, Map<String, String> headers)
	{
		return connectViaStreamableHTTP(url, headers, -1, false, false, false);
	}

	/**
	 * Connect to an MCP Server via the StreamableHTTP.<br/><br/>
	 *
	 * the server operates as an independent process (even remotely) that can handle multiple client connections.
	 * This transport uses HTTP POST and GET requests. Server can optionally make use of Server-Sent Events (SSE) to
	 * stream multiple server messages. This permits basic MCP servers, as well as more feature-rich servers supporting
	 * streaming and server-to-client notifications and requests.<br/><br/>
	 *
	 * NOTE: the authentication part (what is required depends on the MCP server) should be prepared beforehand. Use the
	 * "headers" param. in order to give the needed access rights to the MCP server (it may be some API key or an OAuth token
	 * resulted from using the oauth plugin for example).
	 *
	 * It does not follow redirects. Subsidiary SSE channel is disabled.
	 *
	 * @param url {String} the URL of the MCP Server to connect to.
	 * @param headers {Object<String>} any request headers that need to be given to the MCP Server. The object contains String keys that are header names -> the values of those headers.
	 * @param timeoutMillis {Number} the connection timeout in milliseconds (applied at the http client level). Application-level timeouts are handled by other methods of the MCPClientBuilder itself.
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaStreamableHTTP(String url, Map<String, String> headers, long timeoutMillis)
	{
		return connectViaStreamableHTTP(url, headers, timeoutMillis, false, false, false);
	}

	/**
	 * Connect to an MCP Server via the StreamableHTTP.<br/><br/>
	 *
	 * the server operates as an independent process (even remotely) that can handle multiple client connections.
	 * This transport uses HTTP POST and GET requests. Server can optionally make use of Server-Sent Events (SSE) to
	 * stream multiple server messages. This permits basic MCP servers, as well as more feature-rich servers supporting
	 * streaming and server-to-client notifications and requests.<br/><br/>
	 *
	 * NOTE: the authentication part (what is required depends on the MCP server) should be prepared beforehand. Use the
	 * "headers" param. in order to give the needed access rights to the MCP server (it may be some API key or an OAuth token
	 * resulted from using the oauth plugin for example).
	 *
	 * It does not follow redirects.
	 *
	 * @param url {String} the URL of the MCP Server to connect to.
	 * @param headers {Object<String>} any request headers that need to be given to the MCP Server. The object contains String keys that are header names -> the values of those headers.
	 * @param timeoutMillis {Number} the connection timeout in milliseconds (applied at the http client level). Application-level timeouts are handled by other methods of the MCPClientBuilder itself.
	 * @param subsidiaryChannel {Boolean} enables or disables the subsidiary SSE channel. When enabled, the transport will open an HTTP GET-based SSE stream after initialization, allowing the server to send notifications and requests to the client without the client first sending data via HTTP POST. If the server does not support the subsidiary channel (returns 405), the transport will log a warning and continue without it. If the stream breaks after being successfully established, the transport will automatically attempt to reconnect. Defaults to false.
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaStreamableHTTP(String url, Map<String, String> headers, long timeoutMillis, boolean subsidiaryChannel)
	{
		return connectViaStreamableHTTP(url, headers, timeoutMillis, subsidiaryChannel, false, false);
	}

	/**
	 * Connect to an MCP Server via the StreamableHTTP.<br/><br/>
	 *
	 * the server operates as an independent process (even remotely) that can handle multiple client connections.
	 * This transport uses HTTP POST and GET requests. Server can optionally make use of Server-Sent Events (SSE) to
	 * stream multiple server messages. This permits basic MCP servers, as well as more feature-rich servers supporting
	 * streaming and server-to-client notifications and requests.<br/><br/>
	 *
	 * NOTE: the authentication part (what is required depends on the MCP server) should be prepared beforehand. Use the
	 * "headers" param. in order to give the needed access rights to the MCP server (it may be some API key or an OAuth token
	 * resulted from using the oauth plugin for example).
	 *
	 * @param url {String} the URL of the MCP Server to connect to.
	 * @param headers {Object<String>} any request headers that need to be given to the MCP Server. The object contains String keys that are header names -> the values of those headers.
	 * @param timeoutMillis {Number} the connection timeout in milliseconds (applied at the http client level). Application-level timeouts are handled by other methods of the MCPClientBuilder itself.
	 * @param subsidiaryChannel {Boolean} enables or disables the subsidiary SSE channel. When enabled, the transport will open an HTTP GET-based SSE stream after initialization, allowing the server to send notifications and requests to the client without the client first sending data via HTTP POST. If the server does not support the subsidiary channel (returns 405), the transport will log a warning and continue without it. If the stream breaks after being successfully established, the transport will automatically attempt to reconnect. Defaults to false.
	 * @param followRedirects {Boolean} enables or disables following HTTP redirects (3xx status codes). When enabled, the transport will automatically follow redirects using HttpClient.Redirect.NORMAL policy (always redirect, except from HTTPS to HTTP). Defaults to false.
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaStreamableHTTP(String url, Map<String, String> headers, long timeoutMillis, boolean subsidiaryChannel,
		boolean followRedirects)
	{
		return connectViaStreamableHTTP(url, headers, timeoutMillis, subsidiaryChannel, followRedirects, false);
	}

	/**
	 * Connect to an MCP Server via the StreamableHTTP.<br/><br/>
	 *
	 * the server operates as an independent process (even remotely) that can handle multiple client connections.
	 * This transport uses HTTP POST and GET requests. Server can optionally make use of Server-Sent Events (SSE) to
	 * stream multiple server messages. This permits basic MCP servers, as well as more feature-rich servers supporting
	 * streaming and server-to-client notifications and requests.<br/><br/>
	 *
	 * NOTE: the authentication part (what is required depends on the MCP server) should be prepared beforehand. Use the
	 * "headers" param. in order to give the needed access rights to the MCP server (it may be some API key or an OAuth token
	 * resulted from using the oauth plugin for example).
	 *
	 * @param url {String} the URL of the MCP Server to connect to.
	 * @param headers {Object<String>} any request headers that need to be given to the MCP Server. The object contains String keys that are header names -> the values of those headers.
	 * @param timeoutMillis {Number} the connection timeout in milliseconds (applied at the http client level). Application-level timeouts are handled by other methods of the MCPClientBuilder itself.
	 * @param subsidiaryChannel {Boolean} enables or disables the subsidiary SSE channel. When enabled, the transport will open an HTTP GET-based SSE stream after initialization, allowing the server to send notifications and requests to the client without the client first sending data via HTTP POST. If the server does not support the subsidiary channel (returns 405), the transport will log a warning and continue without it. If the stream breaks after being successfully established, the transport will automatically attempt to reconnect. Defaults to false.
	 * @param followRedirects {Boolean} enables or disables following HTTP redirects (3xx status codes). When enabled, the transport will automatically follow redirects using HttpClient.Redirect.NORMAL policy (always redirect, except from HTTPS to HTTP). Defaults to false.
	 * @param forceHTTPVersion1_1 {Boolean} forces the transport to use HTTP/1.1 instead of the default HTTP/2.
	 */
	@JSFunction
	public MCPClientBuilder<T> connectViaStreamableHTTP(String url, Map<String, String> headers, long timeoutMillis, boolean subsidiaryChannel,
		boolean followRedirects,
		boolean forceHTTPVersion1_1)
	{
		if (url == null || url.length() == 0) throw new RuntimeException("An actual 'url' is required for connecting to MCP Servers via StreamableHTTP");

		StreamableHttpMcpTransport.Builder transportBuilder = StreamableHttpMcpTransport.builder()
			.url(url).subsidiaryChannel(subsidiaryChannel).followRedirects(followRedirects);
		if (headers != null)
			transportBuilder.customHeaders(headers);
		if (timeoutMillis >= 0)
			transportBuilder.timeout(Duration.ofMillis(timeoutMillis));
		if (forceHTTPVersion1_1)
			transportBuilder.setHttpVersion1_1();

		mcpClientBuilder.transport(transportBuilder.build());
		transportWasConfigured = true;
		return this;
	}

	/**
	 * Enables or disables the automatic health check feature.
	 *
	 * When enabled, the client will periodically send ping messages to the server to ensure the connection is alive,
	 * and will attempt to reconnect if it's not.<br/><br/>
	 *
	 * The default is enabled
	 *
	 * @param autoHealthCheck {Boolean}
	 */
	@JSFunction
	public MCPClientBuilder<T> autoHealthCheck(boolean autoHealthCheck)
	{
		mcpClientBuilder.autoHealthCheck(autoHealthCheck);
		return this;
	}

	/**
	 * Sets the interval for the automatic health checks.<br/>
	 * This is only used when the auto health check feature is enabled.<br/><br/>
	 *
	 * The default is 30 seconds.
	 *
	 * @param autoHealthCheckInterval {Number} the auto health check interval in milliseconds.
	 */
	@JSFunction
	public MCPClientBuilder<T> autoHealthCheckInterval(long autoHealthCheckInterval)
	{
		mcpClientBuilder.autoHealthCheckInterval(Duration.ofMillis(autoHealthCheckInterval));
		return this;
	}

	// TODO if we want to support MCP Prompts, we need to expose to scripting an MCPClient that then can call stuff to get back the template prompts.
	// Currently this builder only adds the MCP Client to the AI Agent, so no use exposing things related to template prompts
//	/**
//	 * If set to true, the client will cache the prompt list obtained from the server until it's notified by
//	 * the server that the prompts have changed.<br/>
//	 * If set to false, there is no caching and the client will always fetch the prompt list from the server.<br/><br/>
//	 *
//	 * The default is true.
//	 */
//	@JSFunction
//	public MCPClientBuilder<T> cachePromptList(boolean cachePromptList)
//	{
//		mcpClientBuilder.cachePromptList(cachePromptList);
//		return this;
//	}
//	@JSFunction
//	public MCPClientBuilder<T> promptsTimeout(long pingTimeout)
//	{
//		mcpClientBuilder.promptsTimeout(...);
//		return this;
//	}

	/**
	 * If set to true, the client will cache the resource and resource template lists obtained from the server
	 * until it's notified by the server that the resources have changed.<br/>
	 * If set to false, there is no caching and the client will always fetch the resource list from the server.<br/><br/>
	 *
	 * The default is true.
	 *
	 * @param cacheResourceList {Boolean}
	 */
	@JSFunction
	public MCPClientBuilder<T> cacheResourceList(boolean cacheResourceList)
	{
		mcpClientBuilder.cacheResourceList(cacheResourceList);
		return this;
	}

	/**
	 * If set to true, the client will cache the tool list obtained from the server until it's notified
	 * by the server that the tools have changed or until the cache is evicted.<br/>
	 * If set to false, there is no tool caching and the client will always fetch the tool list from the server.<br/><br/>
	 *
	 * The default is true.
	 *
	 * @param cacheToolList {Boolean}
	 */
	@JSFunction
	public MCPClientBuilder<T> cacheToolList(boolean cacheToolList)
	{
		mcpClientBuilder.cacheToolList(cacheToolList);
		return this;
	}

	/**
	 * Sets the name that the client will use to identify itself to the MCP server in the initialization message.<br/><br/>
	 *
	 * The default value is "langchain4j".
	 *
	 * @param clientName {String}
	 */
	@JSFunction
	public MCPClientBuilder<T> clientName(String clientName)
	{
		mcpClientBuilder.clientName(clientName);
		return this;
	}

	/**
	 * Sets the version string that the client will use to identify
	 * itself to the MCP server in the initialization message.<br/><br/>
	 *
	 * The default value is "1.0".
	 *
	 * @param clientVersion {String}
	 */
	@JSFunction
	public MCPClientBuilder<T> clientVersion(String clientVersion)
	{
		mcpClientBuilder.clientVersion(clientVersion);
		return this;
	}

	/**
	 * Sets the timeout for initializing the client.<br/><br/>
	 *
	 * The default value is 30 seconds.
	 *
	 * @param initializationTimeout {Number} the initialization timeout in milliseconds.
	 */
	@JSFunction
	public MCPClientBuilder<T> initializationTimeout(long initializationTimeout)
	{
		mcpClientBuilder.initializationTimeout(Duration.ofMillis(initializationTimeout));
		return this;
	}

	// Can be supported if needed in the future. But then more scripting types need to be added for various arguments.
//	@JSFunction
//	public MCPClientBuilder<T> listener(...)
//	{
//		mcpClientBuilder.listener(...);
//	}
//	@JSFunction
//	public MCPClientBuilder<T> metaSupplier(...)
//	{
//		mcpClientBuilder.metaSupplier(...);
//	}
//	@JSFunction
//	public MCPClientBuilder<T> progressHandler(...)
//	{
//		mcpClientBuilder.progressHandler(...);
//	}


	/**
	 * The timeout to apply when waiting for a ping response. Currently, this is only used in
	 * the health check - if the server does not send a pong within this timeframe, the health check will fail.<br/><br/>
	 *
	 * The default timeout is 10 seconds.
	 *
	 * @param pingTimeout {Number} the ping timeout in milliseconds.
	 */
	@JSFunction
	public MCPClientBuilder<T> pingTimeout(long pingTimeout)
	{
		mcpClientBuilder.pingTimeout(Duration.ofMillis(pingTimeout));
		return this;
	}

	/**
	 * Sets the protocol version that the client will advertise in the initialization message.<br/><br/>
	 *
	 * The default value will change over time in newer langchain4j versions. For example: "2024-11-05".
	 *
	 * @param protocolVersion {String}
	 */
	@JSFunction
	public MCPClientBuilder<T> protocolVersion(String protocolVersion)
	{
		mcpClientBuilder.protocolVersion(protocolVersion);
		return this;
	}

	/**
	 * The delay before attempting to reconnect after a failed connection. The default is 5 seconds.
	 *
	 * @param reconnectIntervalMillis {Number} the reconnect interval in milliseconds.
	 */
	@JSFunction
	public MCPClientBuilder<T> reconnectInterval(long reconnectIntervalMillis)
	{
		mcpClientBuilder.reconnectInterval(Duration.ofMillis(reconnectIntervalMillis));
		return this;
	}

	/**
	 * Sets the timeout for resource-related operations (listing resources as well as reading
	 * the contents of a resource).<br/><br/>
	 *
	 * The default value is 60 seconds. A value of zero means no timeout.
	 *
	 * @param resourcesTimeoutMillis {Number} the resources timeout in milliseconds.
	 */
	@JSFunction
	public MCPClientBuilder<T> resourcesTimeout(long resourcesTimeoutMillis)
	{
		mcpClientBuilder.resourcesTimeout(Duration.ofMillis(resourcesTimeoutMillis));
		return this;
	}

	/**
	 * Specify the initial set of roots that are available to the server upon its request.<br/>
	 * It is an array of 'root' objects that have the following keys: { name: String, uri: String }
	 *
	 * @param roots {Array<{name: String, uri: String}>} an array of 'root' objects that have the following keys: { name: String, uri: String }
	 */
	@JSFunction
	public MCPClientBuilder<T> roots(Map<String, String>[] roots)
	{
		List<McpRoot> translatedRoots = Arrays.asList(roots).stream().map((root) -> new McpRoot(root.get("name"), root.get("uri")))
			.collect(Collectors.toList());
		mcpClientBuilder.roots(translatedRoots);
		return this;
	}

	/**
	 * Sets the timeout for tool execution. This value applies to each tool execution individually.<br/><br/>
	 *
	 * The default value is 60 seconds. A value of zero means no timeout.
	 *
	 * @param toolExecutionTimeoutMillis {Number} the tool execution timeout in milliseconds.
	 */
	@JSFunction
	public MCPClientBuilder<T> toolExecutionTimeout(long toolExecutionTimeoutMillis)
	{
		mcpClientBuilder.toolExecutionTimeout(Duration.ofMillis(toolExecutionTimeoutMillis));
		return this;
	}

	/**
	 * Sets the error message to return when a tool execution times out. The default value is "There was a timeout executing the tool".
	 *
	 * @param toolExecutionTimeoutErrorMessage {String}
	 */
	@JSFunction
	public MCPClientBuilder<T> toolExecutionTimeoutErrorMessage(String toolExecutionTimeoutErrorMessage)
	{
		mcpClientBuilder.toolExecutionTimeoutErrorMessage(toolExecutionTimeoutErrorMessage);
		return this;
	}

	/**
	 * If you do not want all the tools from the MCP Server to be available to the AI Agent, you can
	 * provide a list of tool names to expose.
	 *
	 * @param toolNameFilters {Array<String>} only tools with the names present in this param will be made available to the AI Agent.
	 */
	@SuppressWarnings("hiding")
	@JSFunction
	public MCPClientBuilder<T> toolFilters(String[] toolNameFilters)
	{
		this.toolNameFilters = Arrays.asList(toolNameFilters);
		return this;
	}

	/**
	 * Create the actual MCP Client based on what was called already on this builder.<br/><br/>
	 *
	 * IMPORTANT: you have to call ChatClient.close(...) when you will no longer use the chat client - in
	 * order to clean up the client's resources (including MCP client connections or child processes (in case
	 * of STDIO transport/connection))
	 */
	@JSFunction
	public T build()
	{
		if (!transportWasConfigured) throw new RuntimeException(
			"Configuring a connection/transport in the MCPClientBuilder is mandatory! Call one of the connectVia...() methods in order to do that.");

		chatBuilder.addMCPClient(mcpClientBuilder.build(), toolNameFilters);
		return chatBuilder;
	}

}
