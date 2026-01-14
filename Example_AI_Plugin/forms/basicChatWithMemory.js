/**
 * The name of the model to use for chat completions.
 * @type {String}
 * @properties={typeid:35,uuid:"DAFC8356-A604-4F93-AACF-153AACD318A9"}
 */
var modelName = 'gpt-3.5-turbo';

/**
 * The system message to set the behavior of the model
 * @type {String}
 * @properties={typeid:35,uuid:"EB3D0463-5BAB-4DB1-91C0-8985410AF7C6"}
 */
var systemMessage = 'You are a helpful assistant.';

/**
 * The user message or prompt to send to the model
 * @type {String}
 * @properties={typeid:35,uuid:"5C76BFE9-08E1-4290-B6A9-2F118EAB2343"}
 */
var userMessage = "Hi, My name is Servoy Hero";

/**
 * The response message from the model
 * @type {String}
 * @properties={typeid:35,uuid:"D758FECC-6DBC-45C8-A1AE-1F596BB933E1"}
 */
var responseMessage = '';

/**
 * The number of tokens used per session
 * @type {Number}
 * @properties={typeid:35,uuid:"A954174A-7939-4701-9DF9-6F21E49AD278",variableType:4}
 */
var tokenCount = 0;

/**
 * Cache the client for chat with memory
 * @type {plugins.ai.ChatClient}
 * @properties={typeid:35,uuid:"A6B312D6-ACB9-41F4-B714-EC40DBCF7EA3",variableType:-4}
 */
var client;

/**
 * The max tokens remembered in a chat session
 * @type {Number}
 * @properties={typeid:35,uuid:"363F421B-8F1A-49D1-906C-D674AABDCF65",variableType:4}
 */
var maxTokenMemory = 5000;

/**
 * Performs a basic chat completion using the specified model, system message, and user message.
 * @properties={typeid:24,uuid:"28BB09E1-D889-41CE-8219-8B488990483D"}
 */
function basicChat() {

	responseMessage += 'ME: '+userMessage+'\n\n';
	
	// send the user message and handle the response
	getChatClient().chat(userMessage).then(function(response) {
		tokenCount += response.getTokenUsage().totalTokenCount();
		responseMessage += 'BOT: ' + response.getResponse() + '\n\n';
		
	// catch any errors
	}).catch(function(e){
		responseMessage = 'Error: ' + e.message;
	});
}

/**
 * Gets the reusable chat client to preserve memory across calls.
 * Instantiates the client if it does not already exist (first call).
 * @private 
 * @return {plugins.ai.ChatClient}
 * @properties={typeid:24,uuid:"96AEA452-C2E7-41CB-9612-4A33FA489EB6"}
 */
function getChatClient(){
	
	// Reuse the chached client, so that memory is preserved across calls
	if(client){
		return client;
	}
	
	// Gemini models
	if(modelName.startsWith('gemini')){
		client = plugins.ai.createGeminiChatBuilder()
		    .apiKey(scopes.exampleAIPlugin.getGeminiApiKey())
			.modelName(modelName)
			.addSystemMessage(systemMessage)
			.maxMemoryTokens(maxTokenMemory) // set the max memory tokens here
			.build();
		return client;
	}
	
	// OpenAI models
	client = plugins.ai.createOpenAiChatBuilder()
	    .apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName(modelName)
		.addSystemMessage(systemMessage)
		.maxMemoryTokens(maxTokenMemory) // set the max memory tokens here
		.build();
	return client;
}

/**
 * Clears the chat memory by resetting the client.
 * @properties={typeid:24,uuid:"C212ABFA-3E9A-4AEB-AD97-D7B2CAB2FDCE"}
 */
function clear(){
	tokenCount = 0;
	responseMessage = '';
	client = null;
}
