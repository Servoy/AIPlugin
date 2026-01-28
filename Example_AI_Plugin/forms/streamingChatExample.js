/**
 * The name of the model to use for chat completions.
 * @type {String}
 * @properties={typeid:35,uuid:"91A754CA-94F0-4A11-8113-9BC66DC67BC0"}
 */
var modelName = 'gpt-3.5-turbo';

/**
 * The system message to set the behavior of the model
 * @type {String}
 * @properties={typeid:35,uuid:"6E956938-248F-439E-9BBC-EDB583797162"}
 */
var systemMessage = 'You are a helpful assistant.';

/**
 * The user message or prompt to send to the model
 * @type {String}
 * @properties={typeid:35,uuid:"9D1F1963-B97B-4669-BAF2-5DC5AE0A135E"}
 */
var userMessage = "Describe in detail the morning of the battle of Gettysburg";

/**
 * The response message from the model
 * @type {String}
 * @properties={typeid:35,uuid:"2389E267-2330-4066-B23E-9B777F548B70"}
 */
var responseMessage = '';

/**
 * The time taken for the response
 * @type {String}
 * @properties={typeid:35,uuid:"1761B053-CBDA-46CF-8847-1A0432CD08E0"}
 */
var time = '';

/**
 * The total token usage (input + output)
 * @type {String}
 * @properties={typeid:35,uuid:"CA2236F8-9D51-4AEF-ACC6-CFA17BC55833"}
 */
var tokens = '';

/**
 * Performs a basic chat completion using the specified model, system message, and user message.
 * @properties={typeid:24,uuid:"0D4713BB-099A-4CC3-ACB2-073642D0D228"}
 */
function streamingChat() {
	
	// clear the results
	tokens = '';
	time = '';
	responseMessage = '';
	
	// get the appropriate chat client
	var client = getChatClient();
	
	// start timing.
	const startTime = new Date().getTime();
	
	// send the user message and handle the response
	client.chat(userMessage,
		
		// on partial response (append string)
		function(response){
			responseMessage += response
		},
		
		// on completion
		function(response){
			responseMessage += response.getResponse();
			time = 'Total Time: ' + (new Date().getTime() - startTime) + ' ms';
			tokens = 'Total Tokens: ' + response.getTokenUsage().totalTokenCount();
		},
		// on error
		function(e){
			application.output('Error during streaming chat: ' + e.message, LOGGINGLEVEL.ERROR);
		}
	);
}

/**
 * Convenience method to get the appropriate chat client based on the model name.
 * @private 
 * @return {plugins.ai.ChatClient}
 * @properties={typeid:24,uuid:"9F8DEC52-91D8-4488-9951-30E0CE225CF4"}
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
