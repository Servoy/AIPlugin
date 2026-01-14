/**
 * @private 
 * @type {String}
 *
 * @properties={typeid:35,uuid:"B6110FDD-7886-4F47-9B9A-FF81C9C29321"}
 */
var OPENAI_API_KEY = '';

/**
 * @private 
 * @type {String}
 *
 * @properties={typeid:35,uuid:"037F222E-B5C5-4EC7-A68C-F75C62E870C5"}
 */
var GEMINI_API_KEY = '';


/**
 * Gets your OpenAI API Key from Servoy properties.
 * To call this method, set the 'openai_api_key' property in servoy.properties before starting Servoy.
 * @public 
 * @return {String}
 * @properties={typeid:24,uuid:"CD165285-1973-41EC-A0CB-5BF814E9E632"}
 */
function getOpenAIApiKey() {
	if(!OPENAI_API_KEY){
		var value = application.getServoyProperty('openai_api_key');
		if(value){
			OPENAI_API_KEY = value;
			application.output('OpenAI API Key retrieved from Servoy properties.', LOGGINGLEVEL.INFO);
		} else{
			application.output('OpenAI API Key not found in Servoy properties.', LOGGINGLEVEL.WARNING);
		}
	}
	return OPENAI_API_KEY;
}

/**
 * Gets your Gemini API Key from Servoy properties.
 * To call this method, set the 'gemini_api_key' property in servoy.properties before starting Servoy.
 * @public 
 * @return {String}
 * @properties={typeid:24,uuid:"B53580A3-F1EB-4467-BD61-03A4D1E0496A"}
 */
function getGeminiApiKey() {
	if(!GEMINI_API_KEY){
		var value = application.getServoyProperty('gemini_api_key');
		if(value){
			GEMINI_API_KEY = value;
			application.output('Gemini API Key retrieved from Servoy properties.', LOGGINGLEVEL.INFO);
		} else{
			application.output('Gemini API Key not found in Servoy properties.', LOGGINGLEVEL.WARNING);
		}
	}
	return GEMINI_API_KEY;
}