package com.servoy.extensions.aiplugin;

import java.beans.PropertyChangeEvent;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.PluginException;
import com.servoy.j2db.scripting.IScriptable;

public class AIPlugin implements IClientPlugin {

	private IClientPluginAccess access;
	private AIProvider impl;

	@Override
	public Properties getProperties() {
		Properties props = new Properties();
		props.put(DISPLAY_NAME, "AI Plugin"); //$NON-NLS-1$
		return props;	}

	@Override
	public void load() throws PluginException {
	}

	@Override
	public void unload() throws PluginException {
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}

	@Override
	public IScriptable getScriptObject() {
		if (impl == null)
		{
			impl = new AIProvider(access);
		}
		return null;
	}

	@Override
	public Icon getImage() {
		java.net.URL iconUrl = this.getClass().getResource("ai2x.png"); //$NON-NLS-1$
		if (iconUrl != null)
		{
			return new ImageIcon(iconUrl);
		}
		else
		{
			return null;
		}
	}

	@Override
	public String getName() {
		return "ai";
	}

	@Override
	public void initialize(IClientPluginAccess access) throws PluginException {
		this.access = access;
	}

}
