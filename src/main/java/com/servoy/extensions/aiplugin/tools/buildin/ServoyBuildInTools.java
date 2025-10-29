package com.servoy.extensions.aiplugin.tools.buildin;

import com.servoy.j2db.plugins.IClientPluginAccess;

import dev.langchain4j.agent.tool.Tool;

public class ServoyBuildInTools {
	
	private IClientPluginAccess access;

	public ServoyBuildInTools(IClientPluginAccess access) {
		this.access = access;
	}

	@Tool("Get the current Servoy user name")
	public String getUserName() {
		return this.access.getDatabaseManager().getApplication().getUserName();
	}
}
