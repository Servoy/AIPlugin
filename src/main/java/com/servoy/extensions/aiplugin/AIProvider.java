package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;

import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

@ServoyDocumented(publicName = "ai", scriptingName = "plugins.ai")
public class AIProvider implements IReturnedTypesProvider, IScriptable {

	private IClientPluginAccess access;

	public AIProvider(IClientPluginAccess access) {
		this.access = access;
	}

	@Override
	public Class<?>[] getAllReturnedTypes() {
		return new Class[] { AIClient.class,GeminiBuilder.class,OpenAiBuilder.class };
	}
	
	

	@JSFunction
	public GeminiBuilder createGeminiBuilder() {
		return new GeminiBuilder(access);
	}

	@JSFunction
	public OpenAiBuilder createOpenAiBuilder() {
		return new OpenAiBuilder(access);
	}
	@JSFunction
	public AIClient createGeminiClient(String apiKey, String modelName) {
		GoogleAiGeminiStreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder().temperature(null).apiKey(apiKey)
				.modelName(modelName).build();
		return new AIClient(model, access);
	}

	@JSFunction
	public AIClient createOpenAIClient(String apiKey, String modelName) {
		OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder().apiKey(apiKey).modelName(modelName).build();
		return new AIClient(model, access);
	}

	public static void main(String[] args) {
		try (Context enter = Context.enter()) {
			enter.initStandardObjects();
			AIProvider p = new AIProvider(new ClientPluginAccessProvider(new TestApplication()));
			AIClient geminiClient = p.createGeminiClient(args[0], "gemini-2.5-flash");
			NativePromise chat = geminiClient.chat("Wat is het verschil tussen een man en een vrouw?");
			chat.put("then", chat, new BaseFunction() {
				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
					System.err.println(args);
					return super.call(cx, scope, thisObj, args);
				}
			});

			System.err.println("-------------------");
			AIClient openAIClient = p.createOpenAIClient(args[1], "gpt-5");
			chat = openAIClient.chat("Wat is het verschil tussen een man en een vrouw?");
			chat.put("then", chat, new BaseFunction() {
				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
					System.err.println(args);
					return super.call(cx, scope, thisObj, args);
				}
			});
			System.err.println(chat);
		}
	}

}
