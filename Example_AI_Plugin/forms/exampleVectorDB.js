/**
 * @type {plugins.ai.EmbeddingStore}
 *
 * @properties={typeid:35,uuid:"23E7F3E0-6F89-42BC-8A68-0CAB26C9D879",variableType:-4}
 */
var store;

/**
 * @properties={typeid:35,uuid:"D765F66B-185C-4731-9B2F-2B961F0474D9",variableType:-4}
 */
var fileList = [];

/**
 * @properties={typeid:24,uuid:"60D7C7A7-9604-4EB1-ADD6-37804B55DA95"}
 */
function createStore(){
	var model = plugins.ai.createOpenAiEmbeddingModelBuilder()
	.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
	.modelName('text-embedding-3-small')
	.build();
	
	store = model.createServoyEmbeddingStoreBuilder()
		.serverName('example_data')
		.tableName('file_embeddings')
		.recreate(true)
		.addText(true)
		.metaDataColumn().name('filename').columnType(JSColumn.TEXT).add()
		.build();
}

/**
 * @properties={typeid:24,uuid:"20154909-5B27-43BE-869B-A31912D8CF56"}
 */
function addFile(){
	plugins.file.showFileOpenDialog(function(files){
		
		/** @type {plugins.file.JSFile} */
		let file = files[0];
		if(!file) return;

		// Embed the file with a chunk size of 500 and an overlap of 50, use metadata
		plugins.svyBlockUI.show("Embedding file, please wait...");
		store.embed(file,500,50,{filename:file.getName()})
		.catch(function(e){
			application.output('Error embedding file ' + file.getName() + ': ' + e);
		})
		.finally(function(){
	    	plugins.svyBlockUI.stop();
	    	application.output('Embedding of file ' + file.getName() + ' completed.');
	    });
		
		
		fileList.push(file.getName());
	});
}
/**
 * Callback method for when form is shown.
 *
 * @param {Boolean} firstShow form is shown first time after load
 * @param {JSEvent} event the event that triggered the action
 *
 * @private
 *
 * @properties={typeid:24,uuid:"CC666E71-182F-412E-B22A-3B3A8219C7E2"}
 */
function onShow(firstShow, event) {
	if(firstShow){
		createStore();
	}
}
