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
	foundset.loadAllRecords();
	
	var model = plugins.ai.createOpenAiEmbeddingModelBuilder()
	
		.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName('text-embedding-3-small')
		.build();
	
	store = model.createInMemoryStore();
	
//	store = model.createServoyEmbeddingStoreBuilder()
//		.recreate(true)
//		.addText(true)
//		.dataSource(foundset.getDataSource())
//		.tableName('product_embeddings')
//		.build();

	store.embedAll(foundset,'productname')
		.then(
			function(){application.output('embedded success')},
			function(){application.output('embedded failed')}
		);
}

/**
 * @properties={typeid:24,uuid:"95409DC9-2262-469E-8B84-61FF819AF1D3"}
 * @AllowToRunInFind
 */
function search(){
	if(!searchText || searchText.trim().length == 0){
        foundset.loadAllRecords();
        return;
    }
	var results = store.search(searchText,1000);
	
	var ids = [];
	for(var i in results){
		if(results[i].getScore() > .7){
			ids.push(results[i].getMetadata().get('productid'))
		}
	}
	foundset.loadRecords(databaseManager.convertToDataSet(ids));
}


