package com.servoy.plugins.aiplugin;

import com.servoy.plugins.aiplugin.services.AIService;
import com.servoy.plugins.aiplugin.scripting.AIProvider;
import com.servoy.plugins.aiplugin.servoy.IServerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Servoy AI Plugin class that integrates langchain4j capabilities
 * into the Servoy platform.
 */
public class AIPlugin implements IServerPlugin {
    private static final Logger logger = LoggerFactory.getLogger(AIPlugin.class);
    
    public static final String PLUGIN_NAME = "AI Plugin";
    public static final String PLUGIN_VERSION = "1.0-SNAPSHOT";
    
    private AIService aiService;
    private AIProvider aiProvider;
    private boolean initialized = false;
    
    @Override
    public void initialize() throws Exception {
        try {
            aiService = new AIService();
            aiProvider = new AIProvider(aiService);
            
            initialized = true;
            logger.info("{} v{} initialized successfully", PLUGIN_NAME, PLUGIN_VERSION);
        } catch (Exception e) {
            logger.error("Failed to initialize AI Plugin", e);
            throw new Exception("Failed to initialize AI Plugin: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void load() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Plugin not initialized");
        }
        
        try {
            // Plugin is ready to use
            logger.info("{} loaded and ready to use", PLUGIN_NAME);
        } catch (Exception e) {
            logger.error("Failed to load AI Plugin", e);
            throw new Exception("Failed to load AI Plugin: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void unload() throws Exception {
        try {
            if (aiService != null) {
                aiService.shutdown();
            }
            
            aiProvider = null;
            aiService = null;
            initialized = false;
            
            logger.info("{} unloaded successfully", PLUGIN_NAME);
        } catch (Exception e) {
            logger.error("Error during AI Plugin unload", e);
            throw new Exception("Error during AI Plugin unload: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }
    
    /**
     * Get the AI provider for JavaScript access.
     * This method would typically be called by Servoy's plugin framework
     * to expose the JavaScript API.
     * 
     * @return The AI provider instance
     */
    public AIProvider getAIProvider() {
        if (!initialized || aiProvider == null) {
            throw new IllegalStateException("Plugin not properly initialized");
        }
        return aiProvider;
    }
    
    /**
     * Get the AI service for internal use.
     * 
     * @return The AI service instance
     */
    public AIService getAIService() {
        if (!initialized || aiService == null) {
            throw new IllegalStateException("Plugin not properly initialized");
        }
        return aiService;
    }
    
    /**
     * Check if the plugin is properly initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get plugin version.
     * 
     * @return The plugin version
     */
    public String getVersion() {
        return PLUGIN_VERSION;
    }
}