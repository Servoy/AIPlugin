package com.servoy.plugins.aiplugin;

import com.servoy.plugins.aiplugin.services.AIService;
import com.servoy.plugins.aiplugin.scripting.AIProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AI Plugin functionality.
 */
public class AIPluginTest {
    
    private AIPlugin plugin;
    
    @BeforeEach
    void setUp() {
        plugin = new AIPlugin();
    }
    
    @Test
    @DisplayName("Plugin should initialize successfully")
    void testPluginInitialization() throws Exception {
        plugin.initialize();
        
        assertTrue(plugin.isInitialized());
        assertEquals("AI Plugin", plugin.getName());
        assertEquals("1.0-SNAPSHOT", plugin.getVersion());
        assertNotNull(plugin.getAIProvider());
        assertNotNull(plugin.getAIService());
    }
    
    @Test
    @DisplayName("Plugin should load after initialization")
    void testPluginLoad() throws Exception {
        plugin.initialize();
        assertDoesNotThrow(() -> plugin.load());
    }
    
    @Test
    @DisplayName("Plugin should unload cleanly")
    void testPluginUnload() throws Exception {
        plugin.initialize();
        plugin.load();
        assertDoesNotThrow(() -> plugin.unload());
        assertFalse(plugin.isInitialized());
    }
    
    @Test
    @DisplayName("Plugin should fail to load if not initialized")
    void testLoadWithoutInitialization() {
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            plugin.load();
        });
        assertTrue(exception.getMessage().contains("not initialized"));
    }
    
    @Test
    @DisplayName("AI Provider should handle empty messages")
    void testAIProviderEmptyMessage() throws Exception {
        plugin.initialize();
        AIProvider provider = plugin.getAIProvider();
        
        String response = provider.chat("");
        assertTrue(response.contains("Error"));
        assertTrue(response.contains("empty"));
    }
    
    @Test
    @DisplayName("AI Provider should handle null messages")
    void testAIProviderNullMessage() throws Exception {
        plugin.initialize();
        AIProvider provider = plugin.getAIProvider();
        
        String response = provider.chat(null);
        assertTrue(response.contains("Error"));
        assertTrue(response.contains("empty"));
    }
    
    @Test
    @DisplayName("AI Provider should report no models configured initially")
    void testAIProviderNoModelsConfigured() throws Exception {
        plugin.initialize();
        AIProvider provider = plugin.getAIProvider();
        
        assertFalse(provider.isChatAvailable("openai"));
        assertFalse(provider.isChatAvailable("azure"));
        assertFalse(provider.isEmbeddingAvailable("openai"));
        
        assertEquals(0, provider.getAvailableChatProviders().length);
        assertEquals(0, provider.getAvailableEmbeddingProviders().length);
    }
    
    @Test
    @DisplayName("AI Provider should return plugin info")
    void testAIProviderInfo() throws Exception {
        plugin.initialize();
        AIProvider provider = plugin.getAIProvider();
        
        String info = provider.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Servoy AI Plugin"));
        assertTrue(info.contains("LangChain4j"));
    }
    
    @Test
    @DisplayName("AI Service should handle invalid API keys")
    void testAIServiceInvalidAPIKey() throws Exception {
        plugin.initialize();
        AIService service = plugin.getAIService();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.configureOpenAiChat("gpt-3.5-turbo", null);
        });
        assertTrue(exception.getMessage().contains("API key"));
        
        exception = assertThrows(IllegalArgumentException.class, () -> {
            service.configureOpenAiChat("gpt-3.5-turbo", "");
        });
        assertTrue(exception.getMessage().contains("API key"));
    }
}
