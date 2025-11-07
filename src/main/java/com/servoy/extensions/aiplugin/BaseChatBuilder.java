package com.servoy.extensions.aiplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabsorb.serializer.MarshallException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.servoy.extensions.aiplugin.tools.buildin.ServoyBuildInTools;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.serialize.JSONSerializerWrapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;

/**
 * BaseChatBuilder is an abstract builder class for constructing AI chat agents in Servoy.
 * <p>
 * It manages tool specifications, tool executors, and integration with Servoy's scripting context.
 * Subclasses can extend this builder to add custom tools and behaviors for AI assistants.
 * <p>
 * Key features:
 * <ul>
 *   <li>Supports built-in Servoy tools via {@link #useBuildInTools(boolean)}</li>
 *   <li>Allows dynamic creation and registration of custom tools using {@link #createTool(Function, String, String)}</li>
 *   <li>Handles tool execution requests and argument mapping for AI agent workflows</li>
 * </ul>
 *
 * @param <T> the type of the builder subclass
 * @author jcompagner
 * @since 2025.12
 */
public class BaseChatBuilder<T extends BaseChatBuilder<T>> {

    /**
     * Indicates whether built-in Servoy tools should be injected into the AI agent.
     */
    protected boolean useBuiltInTools;

    /**
     * The client plugin access instance for Servoy scripting context.
     */
    protected final IClientPluginAccess access;

    /**
     * Map of tool specifications to their executors for the AI agent.
     */
    private Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

    /**
     * Constructs a new BaseChatBuilder with the given Servoy client plugin access.
     *
     * @param access the Servoy client plugin access instance
     */
    protected BaseChatBuilder(IClientPluginAccess access) {
        this.access = access;
    }

    /**
     * Creates and configures an AI assistant builder, injecting built-in and custom tools as needed.
     *
     * @return a configured AiServices builder for Assistant
     */
    protected AiServices<Assistant> createAssistentBuilder() {
        AiServices<Assistant> builder = AiServices.builder(Assistant.class);
        if (useBuiltInTools) {
            builder.tools(new ServoyBuildInTools(access));
        }
        if (tools.size() > 0) {
            builder.tools(tools);
        }
        return builder;
    }

    /**
     * Injects the built-in Servoy tools (such as user info) into the AI agent.
     *
     * @param useBuiltInTools Boolean to indicate whether to use built-in tools.
     * @return This builder instance for chaining.
     */
    @SuppressWarnings("unchecked")
    @JSFunction
    public T useBuiltInTools(boolean useBuiltInTools) {
        this.useBuiltInTools = useBuiltInTools;
        return (T) this;
    }

    /**
     * Creates a new ToolBuilder for defining a custom tool for the AI agent.
     *
     * @param toolFunction the function representing the tool's logic
     * @param name         the name of the tool
     * @param description  the description of the tool
     * @return a ToolBuilder instance for further configuration
     */
    @JSFunction
    public ToolBuilder<T> createTool(Function toolFunction, String name, String description) {
        return new ToolBuilder<T>((T) this, toolFunction, name, description);
    }

    /**
     * Registers a tool specification and its executor for the AI agent.
     * Handles argument mapping and execution for tool requests.
     *
     * @param toolSpecification the tool specification
     * @param toolFunction      the function representing the tool's logic
     * @param parameters        the list of parameter names for the tool
     */
    void addTool(ToolSpecification toolSpecification, Function toolFunction, List<String> parameters) {
        tools.put(toolSpecification, new ToolExecutor() {

            @Override
            public String execute(ToolExecutionRequest request, Object memoryId) {
                FunctionDefinition fd = new FunctionDefinition(toolFunction);

                List<Object> arguments = null;
                if (parameters.size() > 0 && request.arguments() != null && !request.arguments().isBlank()) {
                    arguments = new ArrayList<>();
                    JSONObject argsJson = new JSONObject(request.arguments());
                    for (String paramName : parameters) {
                        arguments.add(argsJson.opt(paramName));
                    }
                }
                Object retValue = fd.executeSync(access, arguments != null ? arguments.toArray() : null);
                if (retValue == null) {
                    return "Success"; // see DefaultToolExecutor
                }
                if (retValue instanceof String) {
                    return retValue.toString();
                }
                try {
                    return new JSONSerializerWrapper(false).toJSON(retValue).toString();
                } catch (MarshallException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return "Failure";
            }

        });
    }

}