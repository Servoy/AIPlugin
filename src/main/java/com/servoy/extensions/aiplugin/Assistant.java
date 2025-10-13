package com.servoy.extensions.aiplugin;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.TokenStream;

interface Assistant {
	TokenStream chat(UserMessage userMessage);
}