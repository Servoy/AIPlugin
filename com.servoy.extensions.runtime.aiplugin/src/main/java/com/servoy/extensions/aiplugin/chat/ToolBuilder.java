package com.servoy.extensions.aiplugin.chat;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecification.Builder;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * ToolBuilder is a builder class for constructing AI tool specifications with
 * parameter definitions.
 * <p>
 * This class allows dynamic creation of tool specifications for AI agents,
 * supporting string, number, and boolean parameters. It is designed to be used
 * in scripting environments and integrates with Servoy's documentation system.
 * <p>
 * Usage example:
 * 
 * <pre>
 * ToolBuilder builder = new ToolBuilder(chatBuilder, toolFunction, "toolName", "Tool description");
 * builder.addStringParameter("param1", "Description", true).addNumberParameter("param2", "Description", false).build();
 * </pre>
 *
 * @param <T> the type of BaseChatBuilder
 * @author jcompagner
 * @since 2025.12
 */
@ServoyDocumented
public class ToolBuilder<T extends BaseChatBuilder<T>> implements IJavaScriptType {

	/**
	 * The chat builder instance to which this tool builder is attached.
	 */
	private final T chatBuilder;

	/**
	 * The internal builder for ToolSpecification.
	 */
	private final Builder builder;

	/**
	 * The function representing the tool's logic.
	 */
	private final Function toolFunction;

	/**
	 * Builder for defining tool parameters as a JSON object schema.
	 */
	private JsonObjectSchema.Builder parameterBuilder;

	/**
	 * List of all parameter names.
	 */
	private List<String> parameters;

	/**
	 * List of required parameter names.
	 */
	private List<String> requiredParameters;

	/**
	 * Constructs a ToolBuilder for a given chat builder and tool function.
	 *
	 * @param chatBuilder  the chat builder instance
	 * @param toolFunction the function representing the tool's logic
	 * @param name         the name of the tool
	 * @param description  the description of the tool
	 */
	public ToolBuilder(T chatBuilder, Function toolFunction, String name, String description) {
		this.chatBuilder = chatBuilder;
		this.toolFunction = toolFunction;
		builder = ToolSpecification.builder().description(description).name(name);
	}

	/**
	 * Adds a string parameter to the tool specification.
	 *
	 * @param name        the parameter name
	 * @param description the parameter description
	 * @param required    whether the parameter is required
	 * @return this ToolBuilder instance for chaining
	 */
	@JSFunction
	public ToolBuilder<T> addStringParameter(String name, String description, boolean required) {
		createParameterBuilder(name, required);
		parameterBuilder.addStringProperty(name, description);
		return this;
	}

	/**
	 * Adds a number parameter to the tool specification.
	 *
	 * @param name        the parameter name
	 * @param description the parameter description
	 * @param required    whether the parameter is required
	 * @return this ToolBuilder instance for chaining
	 */
	@JSFunction
	public ToolBuilder<T> addNumberParameter(String name, String description, boolean required) {
		createParameterBuilder(name, required);
		parameterBuilder.addNumberProperty(name, description);
		return this;
	}

	/**
	 * Adds a boolean parameter to the tool specification.
	 *
	 * @param name        the parameter name
	 * @param description the parameter description
	 * @param required    whether the parameter is required
	 * @return this ToolBuilder instance for chaining
	 */
	@JSFunction
	public ToolBuilder<T> addBooleanParameter(String name, String description, boolean required) {
		createParameterBuilder(name, required);
		parameterBuilder.addBooleanProperty(name, description);
		return this;
	}

	/**
	 * Finalizes the tool specification and returns the chat builder.
	 *
	 * @return the chat builder instance
	 */
	@JSFunction
	public T build() {
		if (parameterBuilder != null) {
			parameterBuilder.required(requiredParameters);
			builder.parameters(parameterBuilder.build());
		}
		ToolSpecification toolSpecification = builder.build();
		chatBuilder.addTool(toolSpecification, toolFunction, parameters);
		return chatBuilder;
	}

	/**
	 * Initializes the parameter builder and required parameters list if not already
	 * done. Adds the parameter name to the required list if needed.
	 *
	 * @param name     the parameter name
	 * @param required whether the parameter is required
	 */
	private void createParameterBuilder(String name, boolean required) {
		if (parameterBuilder == null) {
			parameterBuilder = JsonObjectSchema.builder();
			requiredParameters = new ArrayList<>();
			parameters = new ArrayList<>();
		}
		parameters.add(name);
		if (required) {
			requiredParameters.add(name);
		}
	}

}