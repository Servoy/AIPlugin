/**
 * The name of the model to use for chat completions.
 * @type {String}
 * @properties={typeid:35,uuid:"BD301977-C111-43F1-A2E1-04A5DA880595"}
 */
var modelName = 'gpt-3.5-turbo';

/**
 * The system message to set the behavior of the model
 * @type {String}
 * @properties={typeid:35,uuid:"236F2C4C-9F42-4003-BA4A-C43392C96001"}
 */
var systemMessage = 'You are a helpful assistant.';

/**
 * The user message or prompt to send to the model
 * @type {String}
 * @properties={typeid:35,uuid:"8AC5840B-86D7-47AC-9D0D-AE901423DB5B"}
 */
var userMessage = "What is the latest version of Servoy?";

/**
 * The response message from the model
 * @type {String}
 * @properties={typeid:35,uuid:"40991783-5F41-4C18-A0A0-B210B3FB8FB8"}
 */
var responseMessage = '';

/**
 * The time taken for the response
 * @type {String}
 * @properties={typeid:35,uuid:"E95F9D83-531E-4F26-8B0F-49EB4C4A5310"}
 */
var time = '';

/**
 * The total token usage (input + output)
 * @type {String}
 * @properties={typeid:35,uuid:"76214D15-FA24-4807-8C29-0F5C31549695"}
 */
var tokens = '';

/**
 * Performs a basic chat completion using the specified model, system message, and user message.
 * @properties={typeid:24,uuid:"700E2935-5F29-4DA3-832C-5E416E4BD79A"}
 */
function basicChat() {
	
	// clear the results
	tokens = '';
	time = '';
	responseMessage = '';
	
	// get the appropriate chat client
	var client = getChatClient();
	
	// start timing.
	const startTime = new Date().getTime();
	
	// send the user message and handle the response
	client.chat(userMessage).then(
		
		/** @param {plugins.ai.ChatResponse} response */
		function(response) {
			time = 'Time: ' + (new Date().getTime() - startTime) + ' ms';
			tokens = 'Tokens: ' + response.getTokenUsage().totalTokenCount();
			responseMessage = response.getResponse();
		
		// handle errors
		}).catch(
			
			/** @param {Error} error */
			function(error) {
				responseMessage = 'Error: ' + error.message;
			}
		);
}

/**
 * Convenience method to get the appropriate chat client based on the model name.
 * @private 
 * @return {plugins.ai.ChatClient}
 * @properties={typeid:24,uuid:"0FFCBE64-6171-47F5-A30D-3E0CBC8A4D70"}
 */
function getChatClient(){
	
	// Gemini models
	if(modelName.startsWith('gemini')){
		return plugins.ai.createGeminiChatBuilder()
		    .apiKey(scopes.exampleAIPlugin.getGeminiApiKey())
			.modelName(modelName)
			.addSystemMessage(systemMessage)
			.build();
	}
	
	// OpenAI models
	return plugins.ai.createOpenAiChatBuilder()
	    .apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName(modelName)
		.addSystemMessage(systemMessage)
		.build();
}