/**
 * @properties={typeid:24,uuid:"60D7C7A7-9604-4EB1-ADD6-37804B55DA95"}
 */
function test(){
	var model = plugins.ai.createOpenAiEmbeddingModelBuilder()
	.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
	.modelName('text-embedding-3-small')
	.build();
	
	var store = model.createServoyEmbeddingStoreBuilder()
		.serverName('example_data')
		.tableName('file_embeddings')
		.recreate(true)
		.addText(true)
		.metaDataColumn().name('filename').columnType(JSColumn.TEXT).add()
		.build();
}