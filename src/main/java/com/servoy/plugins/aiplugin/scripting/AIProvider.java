package com.servoy.plugins.aiplugin.scripting;

import com.servoy.plugins.aiplugin.services.AIService;
import com.servoy.plugins.aiplugin.servoy.IScriptObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaScript API provider for AI functionality in Servoy.
 * This class exposes AI capabilities to Servoy's JavaScript environment.
 */
public class AIProvider implements IScriptObject {
    private static final Logger logger = LoggerFactory.getLogger(AIProvider.class);
    
    private final AIService aiService;
    
    public AIProvider(AIService aiService) {
        this.aiService = aiService;
    }
    
    /**
     * Configure OpenAI for chat functionality.
     * 
     * @param apiKey The OpenAI API key
     * @param modelName Optional model name (defaults to gpt-3.5-turbo)
     * @return true if configuration was successful
     */
    public boolean configureOpenAI(String apiKey, String modelName) {
        try {
            aiService.configureOpenAiChat(modelName, apiKey);
            aiService.configureOpenAiEmbedding(apiKey);
            logger.info("OpenAI configured successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to configure OpenAI", e);
            return false;
        }
    }
    
    /**
     * Configure Azure OpenAI for chat functionality.
     * 
     * @param endpoint The Azure OpenAI endpoint
     * @param apiKey The Azure OpenAI API key
     * @param deploymentName The deployment name
     * @return true if configuration was successful
     */
    public boolean configureAzureOpenAI(String endpoint, String apiKey, String deploymentName) {
        try {
            aiService.configureAzureOpenAiChat(endpoint, apiKey, deploymentName);
            logger.info("Azure OpenAI configured successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to configure Azure OpenAI", e);
            return false;
        }
    }
    
    /**
     * Send a chat message and get AI response.
     * 
     * @param message The message to send
     * @param provider Optional provider name ("openai" or "azure", defaults to "openai")
     * @return The AI response
     */
    public String chat(String message, String provider) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return "Error: Message cannot be empty";
            }
            
            String providerName = provider != null ? provider : "openai";
            if (!aiService.isChatModelConfigured(providerName)) {
                return "Error: Chat model not configured for provider: " + providerName;
            }
            
            return aiService.chat(message, providerName);
        } catch (Exception e) {
            logger.error("Chat request failed", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Send a chat message using the default OpenAI provider.
     * 
     * @param message The message to send
     * @return The AI response
     */
    public String chat(String message) {
        return chat(message, "openai");
    }
    
    /**
     * Generate embeddings for text.
     * 
     * @param text The text to embed
     * @param provider Optional provider name (defaults to "openai")
     * @return Array of embedding values, or null if failed
     */
    public float[] embed(String text, String provider) {
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Embed request with empty text");
                return null;
            }
            
            String providerName = provider != null ? provider : "openai";
            if (!aiService.isEmbeddingModelConfigured(providerName)) {
                logger.warn("Embedding model not configured for provider: {}", providerName);
                return null;
            }
            
            return aiService.generateEmbedding(text, providerName);
        } catch (Exception e) {
            logger.error("Embedding request failed", e);
            return null;
        }
    }
    
    /**
     * Generate embeddings for text using the default OpenAI provider.
     * 
     * @param text The text to embed
     * @return Array of embedding values, or null if failed
     */
    public float[] embed(String text) {
        return embed(text, "openai");
    }
    
    /**
     * Check if a chat model is available for the given provider.
     * 
     * @param provider The provider name
     * @return true if chat model is configured
     */
    public boolean isChatAvailable(String provider) {
        String providerName = provider != null ? provider : "openai";
        return aiService.isChatModelConfigured(providerName);
    }
    
    /**
     * Check if an embedding model is available for the given provider.
     * 
     * @param provider The provider name
     * @return true if embedding model is configured
     */
    public boolean isEmbeddingAvailable(String provider) {
        String providerName = provider != null ? provider : "openai";
        return aiService.isEmbeddingModelConfigured(providerName);
    }
    
    /**
     * Get list of available chat providers.
     * 
     * @return Array of provider names
     */
    public String[] getAvailableChatProviders() {
        return aiService.getAvailableChatProviders();
    }
    
    /**
     * Get list of available embedding providers.
     * 
     * @return Array of provider names
     */
    public String[] getAvailableEmbeddingProviders() {
        return aiService.getAvailableEmbeddingProviders();
    }
    
    /**
     * Get plugin information.
     * 
     * @return Plugin version and information
     */
    public String getInfo() {
        return "Servoy AI Plugin v1.0 - Powered by LangChain4j";
    }
}