/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"1A5FCB6A-E181-49D6-A2EA-DD34B5AFAA11"}
 */
var textToEmbed = 'The quick brown fox jumped over the lazy dog';
/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"40DE318A-31F1-4908-8780-7063D30371A7"}
 */
var searchText = 'animals jumping';

/**
 * @type {Number}
 *
 * @properties={typeid:35,uuid:"98F90961-2955-4A93-8CAC-7A69F7E27FB3",variableType:8}
 */
var score;

/**
 * @type {plugins.ai.EmbeddingStore}
 *
 * @properties={typeid:35,uuid:"3D7D5EE4-0C11-49D1-8C02-1373F620178D",variableType:-4}
 */
var inMemoryVectorStore;

/**
 * @properties={typeid:24,uuid:"7E00EBE7-BC6D-4A99-A0E5-855E90EE3F30"}
 * @AllowToRunInFind
 */
function embed(){
	var embeddingModel = plugins.ai.createOpenAiEmbeddingModelBuilder()
		.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName('text-embedding-3-small')
		.build();
	
	inMemoryVectorStore = embeddingModel.createInMemoryStore();
	inMemoryVectorStore.embed([textToEmbed],[]).then(function(){application.output('Embedding complete');});
}

/**
 * @properties={typeid:24,uuid:"A6B6F85E-0E38-48ED-9DE4-8512117400C7"}
 * @AllowToRunInFind
 */
function search(){
	var results = inMemoryVectorStore.search(searchText,1);
	score = results[0].getScore();
}

/**
 * Callback method for when form is shown.
 *
 * @param {Boolean} firstShow form is shown first time after load
 * @param {JSEvent} event the event that triggered the action
 *
 * @private
 *
 * @properties={typeid:24,uuid:"CD1B9CD7-5145-4A2D-BAB9-4679AF2C271B"}
 */
function onShow(firstShow, event) {
	if(firstShow){
		embed();
	}
}
