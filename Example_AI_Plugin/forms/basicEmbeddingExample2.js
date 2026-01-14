/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"0F33490B-424A-480B-9BBA-502CAA93E7FD"}
 */
var textToEmbed = 'This morning I took a walk in the crisp, cool air and warm sunshine\n\
The girl felt better after some hot soup on such a cold, windy day\n\
The woman went for a ride on her sleek, new bike after work\n\
The boy enjoys playing soccer in the freezing rain and snow';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"B25051BB-C5A5-49A2-8FC1-CFA5C8D1E45F"}
 */
var searchText = 'Descriptions of the weather';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"5192F615-79A1-4FA4-BD0F-433EE253D5D9"}
 */
var results = '';

/**
 * @type {plugins.ai.EmbeddingStore}
 *
 * @properties={typeid:35,uuid:"E662C25B-CACA-4A80-883B-DEF5580B499B",variableType:-4}
 */
var inMemoryVectorStore;

/**
 * @properties={typeid:24,uuid:"EE445D9F-14E4-484F-BA9D-2A31486154EC"}
 * @AllowToRunInFind
 */
function embed(){
	var textArray = textToEmbed.split('\n');
	
	var embeddingModel = plugins.ai.createOpenAiEmbeddingModelBuilder()
		.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName('text-embedding-3-small')
		.build();
	
	inMemoryVectorStore = embeddingModel.createInMemoryStore();
	inMemoryVectorStore.embed(textArray, []).then(function(){application.output('Embedding complete');});
}

/**
 * @properties={typeid:24,uuid:"2F20DF02-BA2A-454E-AA40-6F81E1B104D2"}
 * @AllowToRunInFind
 */
function search(){
	var searchResults = inMemoryVectorStore.search(searchText, 100);
	results = searchResults.map(function(res) {
		return 'Text: ' +res.getText().substr(0,30)+ '..., Score: ' + res.getScore().toFixed(4);
	}).join('\n');
}
