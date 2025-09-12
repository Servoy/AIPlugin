/**
 * Servoy AI Plugin Usage Examples
 * 
 * This file demonstrates how to use the AI Plugin in Servoy JavaScript.
 * Copy these examples to your Servoy solution methods.
 */

/**
 * Example 1: Basic OpenAI Configuration and Chat
 */
function example_basicOpenAIChat() {
    // Configure OpenAI
    var apiKey = "your-openai-api-key-here";
    var success = plugins.aiplugin.configureOpenAI(apiKey, "gpt-3.5-turbo");
    
    if (!success) {
        application.output("Failed to configure OpenAI");
        return;
    }
    
    // Send a chat message
    var response = plugins.aiplugin.chat("What is artificial intelligence?");
    
    if (response.indexOf("Error:") === 0) {
        application.output("Chat failed: " + response);
    } else {
        application.output("AI Response: " + response);
    }
}

/**
 * Example 2: Azure OpenAI Configuration and Chat
 */
function example_azureOpenAIChat() {
    // Configure Azure OpenAI
    var endpoint = "https://your-resource.openai.azure.com/";
    var apiKey = "your-azure-openai-key";
    var deploymentName = "gpt-35-turbo";
    
    var success = plugins.aiplugin.configureAzureOpenAI(endpoint, apiKey, deploymentName);
    
    if (!success) {
        application.output("Failed to configure Azure OpenAI");
        return;
    }
    
    // Send a chat message using Azure
    var response = plugins.aiplugin.chat("Explain machine learning in simple terms", "azure");
    application.output("Azure AI Response: " + response);
}

/**
 * Example 3: Text Embeddings
 */
function example_textEmbeddings() {
    // Ensure OpenAI is configured
    if (!plugins.aiplugin.isEmbeddingAvailable("openai")) {
        application.output("OpenAI embeddings not available. Configure first.");
        return;
    }
    
    // Generate embeddings for text
    var text = "Servoy is a powerful business application development platform";
    var embeddings = plugins.aiplugin.embed(text);
    
    if (embeddings) {
        application.output("Generated embedding with " + embeddings.length + " dimensions");
        
        // Example: Store embeddings in database
        // var record = foundset.getRecord(foundset.newRecord());
        // record.text_content = text;
        // record.embedding_vector = JSON.stringify(embeddings);
        // databaseManager.saveData(record);
    } else {
        application.output("Failed to generate embeddings");
    }
}

/**
 * Example 4: Chat with Context and History
 */
function example_contextualChat() {
    if (!plugins.aiplugin.isChatAvailable("openai")) {
        application.output("Chat not available. Configure OpenAI first.");
        return;
    }
    
    // Build context for the conversation
    var context = "You are a helpful assistant for a business application. " +
                  "You help users understand their data and create reports.";
    
    var userQuestion = "How can I create a sales report showing monthly trends?";
    var fullPrompt = context + "\n\nUser question: " + userQuestion;
    
    var response = plugins.aiplugin.chat(fullPrompt);
    application.output("Contextual AI Response: " + response);
}

/**
 * Example 5: Provider Management
 */
function example_providerManagement() {
    // Check what providers are available
    var chatProviders = plugins.aiplugin.getAvailableChatProviders();
    var embeddingProviders = plugins.aiplugin.getAvailableEmbeddingProviders();
    
    application.output("Available chat providers: " + chatProviders.join(", "));
    application.output("Available embedding providers: " + embeddingProviders.join(", "));
    
    // Check specific provider availability
    if (plugins.aiplugin.isChatAvailable("openai")) {
        application.output("OpenAI chat is ready");
    }
    
    if (plugins.aiplugin.isChatAvailable("azure")) {
        application.output("Azure OpenAI chat is ready");
    }
    
    // Get plugin information
    var info = plugins.aiplugin.getInfo();
    application.output("Plugin info: " + info);
}

/**
 * Example 6: Batch Processing
 */
function example_batchProcessing() {
    if (!plugins.aiplugin.isChatAvailable("openai")) {
        application.output("Chat not available. Configure OpenAI first.");
        return;
    }
    
    // Example: Process multiple customer feedback entries
    var feedbackTexts = [
        "Great product, very satisfied!",
        "Poor customer service, disappointed",
        "Good value for money, would recommend",
        "Product quality could be better"
    ];
    
    var sentiment_prompt = "Analyze the sentiment of this customer feedback and respond with: POSITIVE, NEGATIVE, or NEUTRAL. Feedback: ";
    
    for (var i = 0; i < feedbackTexts.length; i++) {
        var response = plugins.aiplugin.chat(sentiment_prompt + feedbackTexts[i]);
        application.output("Feedback " + (i + 1) + " sentiment: " + response);
    }
}

/**
 * Example 7: Error Handling
 */
function example_errorHandling() {
    try {
        // Attempt to use chat without configuration
        var response = plugins.aiplugin.chat("Test message");
        
        if (response.indexOf("Error:") === 0) {
            application.output("Expected error: " + response);
            
            // Handle different types of errors
            if (response.indexOf("not configured") > -1) {
                application.output("Need to configure AI provider first");
            } else if (response.indexOf("API key") > -1) {
                application.output("API key issue detected");
            }
        }
        
        // Test with empty message
        response = plugins.aiplugin.chat("");
        application.output("Empty message response: " + response);
        
    } catch (ex) {
        application.output("Exception occurred: " + ex.getMessage());
    }
}

/**
 * Example 8: Configuration Validation
 */
function example_configurationValidation() {
    // Test invalid configurations
    var success;
    
    // Test with empty API key
    success = plugins.aiplugin.configureOpenAI("", "gpt-3.5-turbo");
    application.output("Empty API key result: " + success); // Should be false
    
    // Test with null values
    success = plugins.aiplugin.configureOpenAI(null, "gpt-3.5-turbo");
    application.output("Null API key result: " + success); // Should be false
    
    // Test Azure with missing parameters
    success = plugins.aiplugin.configureAzureOpenAI("", "key", "deployment");
    application.output("Empty endpoint result: " + success); // Should be false
}