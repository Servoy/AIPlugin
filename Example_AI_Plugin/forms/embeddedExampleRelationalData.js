/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"758A34EB-92D6-452C-BB40-7BE09E2C3B77"}
 */
var searchText = '';

/**
 * @type {plugins.ai.EmbeddingStore}
 *
 * @properties={typeid:35,uuid:"53398F43-6AF3-40A9-B77A-D95CB41CEB81",variableType:-4}
 */
var store;

/**
 * @properties={typeid:24,uuid:"9277DFBD-FD03-487A-852F-8B741B3787C0"}
 */
function createEmbeddings(){
	
	// Load all records to embed
	foundset.loadAllRecords();
	
	// Create the embedding model
	var model = plugins.ai.createOpenAiEmbeddingModelBuilder()
		.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName('text-embedding-3-small')
		.build();
	
	// Create an in-memory store
	store = model.createInMemoryStore();

	// Embed all records in the foundset
	store.embedAll(foundset,'productname')
		.then(
			function(){application.output('embedded success')},
			function(e){application.output('embedded failed: ' + e)}
		);
}

/**
 * @properties={typeid:24,uuid:"95409DC9-2262-469E-8B84-61FF819AF1D3"}
 * @AllowToRunInFind
 */
function search(){
	
	// If no search text, load all records
	if(!searchText || searchText.trim().length == 0){
        foundset.loadAllRecords();
        return;
    }
	
	// Search the store
	var results = store.search(searchText,1000);
	
	// Get the ids of the results with a score > .7 and load them into the foundset
	var ids = [];
	for(var i in results){
		if(results[i].getScore() > .7){
			ids.push(results[i].getMetadata().productid)
		}
	}
	foundset.loadRecords(databaseManager.convertToDataSet(ids));
}



/**
 * Callback method for when form is shown.
 *
 * @param {Boolean} firstShow form is shown first time after load
 * @param {JSEvent} event the event that triggered the action
 *
 * @private
 *
 * @properties={typeid:24,uuid:"BDD162CE-B636-4B20-97CC-CD916F355E19"}
 */
function onShow(firstShow, event) {
	if(firstShow){
		createEmbeddings();
	}
}
