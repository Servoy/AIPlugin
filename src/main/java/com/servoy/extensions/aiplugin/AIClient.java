package com.servoy.extensions.aiplugin;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IScriptable;

import dev.langchain4j.model.chat.ChatModel;

public class AIClient implements IScriptable, IJavaScriptType {

	private ChatModel model;

	public AIClient(ChatModel model) {
		this.model = model;
	}
	
	@JSFunction
	public String chat(String message) {
		return model.chat(message);
	}

}
