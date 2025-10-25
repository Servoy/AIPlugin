package com.servoy.extensions.aiplugin.server;

import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.plugins.IServerPlugin;
import com.servoy.j2db.plugins.PluginException;

import java.util.Map;
import java.util.Properties;

import static com.servoy.extensions.aiplugin.AiPluginService.AIPLUGIN_SERVICE;

public class AiServerPlugin implements IServerPlugin {

    @Override
    public void initialize(IServerAccess serverAccess) throws PluginException {
        try {
            serverAccess.registerRMIService(AIPLUGIN_SERVICE, new AiPluginServiceImpl(serverAccess));
        } catch (Exception e) {
            throw new PluginException(e);
        }
    }

    @Override
    public Properties getProperties() {
        Properties props = new Properties();
        props.put(DISPLAY_NAME, "AI Server Plugin");
        return props;
    }

    @Override
    public Map<String, String> getRequiredPropertyNames() {
        return Map.of();
    }

    @Override
    public void load() throws PluginException {

    }

    @Override
    public void unload() throws PluginException {

    }
}
