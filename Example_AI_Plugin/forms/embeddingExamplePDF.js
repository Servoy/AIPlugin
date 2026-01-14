/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"6A591603-FFED-4CC2-A4EB-C66246EA637B"}
 */
var filePath = '';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"C3F176AA-E282-4E0A-ACC2-7CEA4AC87FE9"}
 */
var question = '';

/**
 * @type {plugins.ai.EmbeddingStore}
 *
 * @properties={typeid:35,uuid:"EB5BA712-259C-492B-A630-F05ADE4E963A",variableType:-4}
 */
var store;

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"A5A71904-142A-4BF2-A927-6A3ACEFDA25E"}
 */
var answer = '';

/**
 * @type {Number}
 *
 * @properties={typeid:35,uuid:"26292D74-1567-47C0-BDBF-EDD9144E1318",variableType:8}
 */
var confidence;

/**
 *
 * @properties={typeid:24,uuid:"1BAACB0D-DCC0-4A5A-9CAC-F17619AC49AC"}
 * @AllowToRunInFind
 */
function selectFile(){
	plugins.file.showFileOpenDialog(function(files){
		
		/** @type {plugins.file.JSFile} */
		let file = files[0];
		if(!file) return;
		
		filePath = file.getName();
		
		// Create the in-memory embedding store
		store = plugins.ai.createOpenAiEmbeddingModelBuilder()
	        .apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
	        .modelName('text-embedding-3-small').build()
	        .createInMemoryStore();
		
		// Embed the file with a chunk size of 500 and an overlap of 50
		plugins.svyBlockUI.show("Embedding file, please wait...");
	    store.embed(file,500,50).finally(function(){
	    	plugins.svyBlockUI.stop();
	    });
	});
}

/**
 * @properties={typeid:24,uuid:"DB3D820A-F841-403E-A342-F085BAE1C47D"}
 * @AllowToRunInFind
 */
function askQuestion(){
	if(!store || !question || question.trim().length == 0){
		return;
	}
	
	// Search the embedding store for the most relevant chunk
	var results = store.search(question,1);
    for(var i in results){
    	var result = results[i];
    	confidence = result.getScore();
    	application.output('RESULT: ' + result.getScore() + ": " + result.getText()+'\n');
    }
    
    // build a chat client to ask the question with context
    var chatClient = plugins.ai.createOpenAiChatBuilder()
		.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
	    .modelName('gpt-4o')
		.addSystemMessage('You are great at answering questions about products when given context directly from the product manual.')
		.build();
	
    // build the prompt with the context from the most relevant chunk
    var prompt = 'Use the following context to answer the question.\n\n\
    The question is: "' + question + '"\n\n\
    The context is:\n\n"' + results[0].getText();
    
    // Ask the question
    plugins.svyBlockUI.show("Asking the Auntie, please wait...");
    chatClient.chat(prompt).then(function(response){
    	answer = response.getResponse();
    }).finally(function(){
    	plugins.svyBlockUI.stop();
    });
}
