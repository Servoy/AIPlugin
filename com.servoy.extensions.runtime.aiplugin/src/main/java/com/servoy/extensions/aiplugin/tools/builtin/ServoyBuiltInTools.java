package com.servoy.extensions.aiplugin.tools.builtin;

import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.agent.tool.Tool;

public class ServoyBuiltInTools {
	
	private final IClientPluginAccess access;

	public ServoyBuiltInTools(IClientPluginAccess access) {
		this.access = access;
	}

	@Tool("Get the current Servoy user name")
	public String getUserName() {
		return this.access.getDatabaseManager().getApplication().getUserName();
	}
}
