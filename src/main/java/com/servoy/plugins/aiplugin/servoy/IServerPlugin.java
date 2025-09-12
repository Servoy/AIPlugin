package com.servoy.plugins.aiplugin.servoy;

/**
 * Stub interface for Servoy's IServerPlugin.
 * This is a simplified version for development without actual Servoy dependencies.
 */
public interface IServerPlugin {
    void initialize() throws Exception;
    void load() throws Exception;
    void unload() throws Exception;
    String getName();
}