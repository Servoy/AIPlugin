package com.servoy.plugins.aiplugin.services;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core AI service that provides chat, embedding, and other AI capabilities
 * using langchain4j libraries.
 */
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    private final Map<String, ChatLanguageModel> chatModels = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModels = new ConcurrentHashMap<>();
    
    private String defaultApiKey;
    private String defaultModel = "gpt-3.5-turbo";
    private String defaultEmbeddingModel = "text-embedding-ada-002";
    
    /**
     * Initialize the AI service with configuration.
     */
    public void initialize(String apiKey) {
        this.defaultApiKey = apiKey;
        logger.info("AI Service initialized");
    }
    
    /**
     * Configure OpenAI chat model.
     */
    public void configureOpenAiChat(String modelName, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        try {
            ChatLanguageModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName != null ? modelName : defaultModel)
                    .temperature(0.7)
                    .maxTokens(1000)
                    .build();
            
            chatModels.put("openai", model);
            logger.info("OpenAI chat model configured: {}", modelName);
        } catch (Exception e) {
            logger.error("Failed to configure OpenAI chat model", e);
            throw new RuntimeException("Failed to configure OpenAI chat model: " + e.getMessage());
        }
    }
    
    /**
     * Configure Azure OpenAI chat model.
     */
    public void configureAzureOpenAiChat(String endpoint, String apiKey, String deploymentName) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        
        try {
            ChatLanguageModel model = AzureOpenAiChatModel.builder()
                    .endpoint(endpoint)
                    .apiKey(apiKey)
                    .deploymentName(deploymentName)
                    .temperature(0.7)
                    .maxTokens(1000)
                    .build();
            
            chatModels.put("azure", model);
            logger.info("Azure OpenAI chat model configured: {}", deploymentName);
        } catch (Exception e) {
            logger.error("Failed to configure Azure OpenAI chat model", e);
            throw new RuntimeException("Failed to configure Azure OpenAI chat model: " + e.getMessage());
        }
    }
    
    /**
     * Configure OpenAI embedding model.
     */
    public void configureOpenAiEmbedding(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        try {
            EmbeddingModel model = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .modelName(defaultEmbeddingModel)
                    .build();
            
            embeddingModels.put("openai", model);
            logger.info("OpenAI embedding model configured");
        } catch (Exception e) {
            logger.error("Failed to configure OpenAI embedding model", e);
            throw new RuntimeException("Failed to configure OpenAI embedding model: " + e.getMessage());
        }
    }
    
    /**
     * Send a chat message and get AI response.
     */
    public String chat(String message, String provider) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        ChatLanguageModel model = chatModels.get(provider != null ? provider : "openai");
        if (model == null) {
            throw new IllegalStateException("Chat model not configured for provider: " + provider);
        }
        
        try {
            UserMessage userMessage = UserMessage.from(message);
            AiMessage response = model.generate(userMessage).content();
            
            String result = response.text();
            logger.debug("Chat response generated for provider {}: {} chars", provider, result.length());
            return result;
        } catch (Exception e) {
            logger.error("Failed to generate chat response", e);
            throw new RuntimeException("Failed to generate chat response: " + e.getMessage());
        }
    }
    
    /**
     * Generate embeddings for text.
     */
    public float[] generateEmbedding(String text, String provider) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        EmbeddingModel model = embeddingModels.get(provider != null ? provider : "openai");
        if (model == null) {
            throw new IllegalStateException("Embedding model not configured for provider: " + provider);
        }
        
        try {
            var embedding = model.embed(text).content();
            float[] result = new float[embedding.vector().length];
            for (int i = 0; i < embedding.vector().length; i++) {
                result[i] = embedding.vector()[i];
            }
            
            logger.debug("Embedding generated for provider {}: {} dimensions", provider, result.length);
            return result;
        } catch (Exception e) {
            logger.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage());
        }
    }
    
    /**
     * Check if a chat model is configured for the given provider.
     */
    public boolean isChatModelConfigured(String provider) {
        return chatModels.containsKey(provider != null ? provider : "openai");
    }
    
    /**
     * Check if an embedding model is configured for the given provider.
     */
    public boolean isEmbeddingModelConfigured(String provider) {
        return embeddingModels.containsKey(provider != null ? provider : "openai");
    }
    
    /**
     * Get available chat model providers.
     */
    public String[] getAvailableChatProviders() {
        return chatModels.keySet().toArray(new String[0]);
    }
    
    /**
     * Get available embedding model providers.
     */
    public String[] getAvailableEmbeddingProviders() {
        return embeddingModels.keySet().toArray(new String[0]);
    }
    
    /**
     * Clean up resources.
     */
    public void shutdown() {
        chatModels.clear();
        embeddingModels.clear();
        logger.info("AI Service shut down");
    }
}