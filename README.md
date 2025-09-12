# Servoy AI Plugin

A powerful AI plugin for the Servoy platform that integrates [LangChain4j](https://github.com/langchain4j/langchain4j) to provide advanced AI capabilities including chat completion, text embeddings, and support for multiple AI providers.

## Features

- **Multi-Provider Support**: OpenAI and Azure OpenAI integration
- **Chat Completion**: Send messages and receive AI-generated responses
- **Text Embeddings**: Generate vector embeddings for text content
- **Servoy Integration**: Seamlessly integrates with Servoy's JavaScript environment
- **Type Safety**: Robust error handling and validation
- **Comprehensive Testing**: Full test coverage for reliability

## Dependencies

This plugin is built using:
- **LangChain4j**: Core AI library for Java
- **OpenAI/Azure OpenAI**: Chat and embedding models
- **Jackson**: JSON processing
- **SLF4J**: Logging framework
- **JUnit 5**: Testing framework

## Installation

1. Download the plugin JAR file from the releases
2. Copy the JAR and its dependencies to your Servoy plugins directory
3. Restart Servoy to load the plugin

## Configuration

### OpenAI Configuration

```javascript
// Configure OpenAI (default provider)
var success = plugins.aiplugin.configureOpenAI("your-api-key", "gpt-3.5-turbo");
if (success) {
    application.output("OpenAI configured successfully");
}
```

### Azure OpenAI Configuration

```javascript
// Configure Azure OpenAI
var success = plugins.aiplugin.configureAzureOpenAI(
    "https://your-resource.openai.azure.com/",
    "your-api-key",
    "your-deployment-name"
);
if (success) {
    application.output("Azure OpenAI configured successfully");
}
```

## Usage Examples

### Basic Chat

```javascript
// Send a simple chat message
var response = plugins.aiplugin.chat("Hello, how are you?");
application.output("AI Response: " + response);
```

### Chat with Specific Provider

```javascript
// Use a specific AI provider
var response = plugins.aiplugin.chat("Explain quantum computing", "azure");
application.output("AI Response: " + response);
```

### Generate Embeddings

```javascript
// Generate embeddings for text
var embeddings = plugins.aiplugin.embed("This is a sample text for embedding");
if (embeddings) {
    application.output("Generated " + embeddings.length + " dimensional embedding");
    // Use embeddings for similarity search, clustering, etc.
}
```

### Check Provider Availability

```javascript
// Check if providers are configured
if (plugins.aiplugin.isChatAvailable("openai")) {
    application.output("OpenAI chat is available");
}

if (plugins.aiplugin.isEmbeddingAvailable("openai")) {
    application.output("OpenAI embeddings are available");
}

// Get list of available providers
var chatProviders = plugins.aiplugin.getAvailableChatProviders();
var embeddingProviders = plugins.aiplugin.getAvailableEmbeddingProviders();
```

## API Reference

### Configuration Methods

- `configureOpenAI(apiKey, modelName)` - Configure OpenAI provider
- `configureAzureOpenAI(endpoint, apiKey, deploymentName)` - Configure Azure OpenAI provider

### Chat Methods

- `chat(message)` - Send message using default provider (OpenAI)
- `chat(message, provider)` - Send message using specific provider

### Embedding Methods

- `embed(text)` - Generate embeddings using default provider (OpenAI)
- `embed(text, provider)` - Generate embeddings using specific provider

### Utility Methods

- `isChatAvailable(provider)` - Check if chat is available for provider
- `isEmbeddingAvailable(provider)` - Check if embeddings are available for provider
- `getAvailableChatProviders()` - Get list of configured chat providers
- `getAvailableEmbeddingProviders()` - Get list of configured embedding providers
- `getInfo()` - Get plugin information

## Error Handling

The plugin includes comprehensive error handling:

```javascript
var response = plugins.aiplugin.chat("Hello");
if (response.startsWith("Error:")) {
    application.output("Chat failed: " + response);
} else {
    application.output("Success: " + response);
}
```

## Building from Source

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Servoy/AIPlugin.git
cd AIPlugin

# Compile and test
mvn clean compile test

# Package the plugin
mvn package

# The plugin JAR will be available in target/aiplugin-1.0-SNAPSHOT.jar
# Dependencies will be copied to target/lib/
```

### Running Tests

```bash
mvn test
```

## Development

### Project Structure

```
src/
├── main/java/com/servoy/plugins/aiplugin/
│   ├── AIPlugin.java              # Main plugin class
│   ├── services/
│   │   └── AIService.java         # Core AI service implementation
│   ├── scripting/
│   │   └── AIProvider.java        # JavaScript API provider
│   └── servoy/
│       ├── IServerPlugin.java     # Servoy plugin interface
│       └── IScriptObject.java     # Servoy script object interface
└── test/java/com/servoy/plugins/aiplugin/
    └── AIPluginTest.java          # Unit tests
```

### Adding New AI Providers

To add support for new AI providers:

1. Add the provider dependency to `pom.xml`
2. Extend `AIService.java` with new configuration methods
3. Update `AIProvider.java` to expose the functionality to JavaScript
4. Add appropriate tests

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Support

For issues and questions:
- Create an issue on GitHub
- Check the [Servoy documentation](https://www.servoy.com/documentation)
- Review the [LangChain4j documentation](https://docs.langchain4j.dev/)

## Changelog

### v1.0-SNAPSHOT
- Initial release
- OpenAI and Azure OpenAI support
- Chat completion functionality
- Text embedding generation
- Comprehensive test coverage
- Full Servoy integration
