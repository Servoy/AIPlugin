package com.servoy.extensions.aiplugin.chat;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.TokenStream;

public interface Assistant {
	TokenStream chat(UserMessage userMessage);
}