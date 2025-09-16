package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class AIProvider implements IReturnedTypesProvider, IScriptable{

	private IClientPluginAccess access;

	public AIProvider(IClientPluginAccess access) {
		this.access = access;
	}

	@Override
	public Class<?>[] getAllReturnedTypes() {
		return new Class[] { AIClient.class };
	}
	
	@JSFunction
	public AIClient createGeminiClient(String apiKey, String modelName) {
		ChatModel model = GoogleAiGeminiChatModel.builder().apiKey(apiKey).modelName(modelName).build();
		return new AIClient(model);
	}
	
	@JSFunction
	public AIClient createOpenAIClient(String apiKey, String modelName) {
		ChatModel model = OpenAiChatModel.builder().apiKey(apiKey).modelName(modelName).build();
		return new AIClient(model);
	}
	
	
	public static void main(String[] args) {
		AIProvider p = new AIProvider(null);
		AIClient geminiClient = p.createGeminiClient(args[0],"gemini-2.5-flash");
		String chat = geminiClient.chat("Wat is het verschil tussen een man en een vrouw?");
		System.err.println(chat);
		
		System.err.println("-------------------");
		AIClient openAIClient = p.createOpenAIClient(args[1], "gpt-5");
		chat = openAIClient.chat("Wat is het verschil tussen een man en een vrouw?");
		System.err.println(chat);
	}

}
