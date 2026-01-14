/**
 * The name of the model to use for chat completions.
 * @type {String}
 * @properties={typeid:35,uuid:"85A5617D-BCA8-41C9-91D2-B7E174CBA171"}
 */
var modelName = 'gpt-4.1';

/**
 * The system message to set the behavior of the model
 * @type {String}
 * @properties={typeid:35,uuid:"CA70B3F4-A6D9-4E1F-A572-6D9769C2BC8B"}
 */
var systemMessage = 'You are a SQL expert. You will always be given a database schema in JSON format as context and then a user question.\
You will respond with a valid SQL query that answers the user question based on the provided database schema.\
The JSON will contain a databaseType. Do not use any functions or syntax which is not supported by the type/product. Write only ONE select statement.\
Only provide the SQL query as the response and nothing else, not even md headings, etc.';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"330B145E-71D6-4EE2-AC7C-BD840B941A07"}
 */
var systemMessageAnswer = 'You are a data expert. You will be given a user question and dataset in, CSV format, for context. \
The dataset will contain column names in the 1st row. \
You will respond with a concise and accurate answer to the user question based on the provided dataset. \
It is okay to expand on the answer and you should include relevant data and observations, and you may explain/justify your answer, but\
only provide the answer and nothing else, not even md headings. Do not repeat the context or mention the dataset directly';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"12743CCF-F01E-4862-B170-AC37E94BBC89"}
 */
var systemMessageChart = 'You are a data visualization expert. You will be given a user question and dataset in, CSV format, for context. \
You will respond in strict JSON format following the format for well-known ChartJS data-structures. \
Only return JSON. Do not include any other text, markdown, or explanations. \
If the question and the dataset do not lend themselves to a meaningful chart, return an empty JSON object {}.';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"3F465A45-A1AB-4A14-9B59-3E9261AE97DE"}
 */
var chartData = '';

/**
 * The user message or prompt to send to the model
 * @type {String}
 * @properties={typeid:35,uuid:"51CF7C4F-92C9-4058-8008-DE17D62BAC31"}
 */
var userMessage = "What are top 5 best products?";

/**
 * The response message from the model
 * @type {String}
 * @properties={typeid:35,uuid:"4804A6C0-AC5D-4D13-8C2A-B55845E57AC2"}
 */
var sqlQuery = '';

/**
 * The time taken for the response
 * @type {String}
 * @properties={typeid:35,uuid:"028ADB5C-E6CD-44B0-A5CA-13405664F637"}
 */
var time = '';

/**
 * The total token usage (input + output)
 * @type {String}
 * @properties={typeid:35,uuid:"4ADC5B47-8155-4D97-85A8-586FA598D435"}
 */
var tokens = '';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"6031954C-873A-43DA-9A28-F88EF2A1AF32"}
 */
var serverName = 'example_data';

/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"D607D8BF-BB70-478C-8A51-DCF68550933B"}
 */
var answer = '';

/**
 * @type {JSDataSet}
 * @properties={typeid:35,uuid:"EDAC9753-C515-40C4-A938-3B16BBA0C885",variableType:-4}
 */
var dataset = null;

/**
 * 
 * @properties={typeid:24,uuid:"0AD82D7B-1AE4-4C77-B56D-D72914E2AE44"}
 */
function getSQL() {
	
	// clear the results
	tokens = '';
	time = '';
	sqlQuery = '';
	answer = '';
	chartData = '';
	
	// get chat client
	var client = getChatClient();
	
	// start timing.
	const startTime = new Date().getTime();
	
	// append the database schema to the user message
	var prompt = 'Database Schema:\n' + JSON.stringify(getSchema()) + '\n\nUser Question:\n' + userMessage;
	
	// send the user message and handle the response
	plugins.svyBlockUI.show('Getting SQL...');
	client.chat(prompt).then(function(response) {
		time = 'Time: ' + (new Date().getTime() - startTime) + ' ms';
		tokens = 'Tokens: ' + response.getTokenUsage().totalTokenCount();
		sqlQuery = response.getResponse();
		plugins.svyBlockUI.stop();
		
		generateAnswer();
		generateChart();
		
	// catch any errors
	}).catch(function(e){
		sqlQuery = 'Error: ' + e.message;
	});
}

/**
 * @properties={typeid:24,uuid:"C0410BE7-BD0F-4529-9923-123236B613D7"}
 */
function generateAnswer(){
	
	dataset = databaseManager.getDataSetByQuery(serverName, sqlQuery, null, 1000);
	var csv = dataset.getAsText(',','\n','"',true);
	
	var client = plugins.ai.createOpenAiChatBuilder()
	    .apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName(modelName)
		.addSystemMessage(systemMessageAnswer)
		.build();
	
	plugins.svyBlockUI.show('Generating Answer...');
	client.chat('User Question:\n' + userMessage + '\n\nDataset in CSV format:\n' + csv).then(function(response) {
		answer = response.getResponse();
	}).catch(function(e){
		application.output('Error generating answer: ' + e.message);
	}).finally(function(){
		plugins.svyBlockUI.stop();
	});
}

/**
 * @private 
 * @param {String} sql
 *
 * @properties={typeid:24,uuid:"480BE1EB-3644-4449-B8CA-57AAD91EA470"}
 */
function loadDataGrid(sql) {
	
	elements.grid.removeAllColumns();
	
	ds.getColumnNames().forEach(function(colName) {
		var col = elements.grid.newColumn(colName);
		col.headerTitle = colName;
	});
	
	ds.createDataSource('custom_query');
	elements.grid.refreshData();
	
}

/**
 * @properties={typeid:24,uuid:"7A48A4ED-9AAA-4433-9969-7807BB2F13A8"}
 */
function generateChart(){
	var csv = dataset.getAsText(',','\n','"',true);
	var client = plugins.ai.createOpenAiChatBuilder()
	    .apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName(modelName)
		.addSystemMessage(systemMessageChart)
		.build();
	
	plugins.svyBlockUI.show('Generating Chart Data...');
	client.chat('User Question:\n' + userMessage + '\n\nDataset in CSV format:\n' + csv).then(function(response) {
		chartData = response.getResponse();
		application.output('Chart Data:\n' + chartData);
		var data = JSON.parse(chartData);
		elements.chart.setData(data);
		elements.chart.setOptions(data.options);
		elements.chart.refreshChart();
		plugins.svyBlockUI.stop();
	}).catch(function(e){
		application.output('Error generating chart data: ' + e.message);
	});
}

/**
 * Convenience method to get the appropriate chat client based on the model name.
 * @private 
 * @return {plugins.ai.ChatClient}
 * @properties={typeid:24,uuid:"8D2CC2FE-58B2-4273-AF80-C57FF466AC94"}
 */
function getChatClient(){
	return plugins.ai.createOpenAiChatBuilder()
	    .apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName(modelName)
		.addSystemMessage(systemMessage)
		.build();
}

/**
 * Fetches the database schema for the specified server in JSON format.
 * @private 
 * @return {Object}
 * @properties={typeid:24,uuid:"197860B2-1A61-4A0A-98F1-4C89D79F8BA2"}
 */
function getSchema() {
	let schema = {databaseType:databaseManager.getDatabaseProductName(serverName),tables: []};
	let tableNames = databaseManager.getTableNames(serverName);
	for (let i = 0; i < tableNames.length; i++) {
		let tableName = tableNames[i];
		let tableInfo = {table: tableName, columns: []};
		let table = databaseManager.getTable(serverName,tableName);
		let PKColumns = table.getRowIdentifierColumnNames();
		let columns = table.getColumnNames();
		for (let j = 0; j < columns.length; j++) {
			let columnName = columns[j];
			let column = table.getColumn(columnName);
			tableInfo.columns.push({
				name: columnName,
				type: column.getTypeAsString(),
				length: column.getLength(),
				description : column.getDescription(),
				title : column.getTitle(),
				fkForTableName: column.getForeignType(),
				isPK: PKColumns.indexOf(columnName) != -1
			});
		}
		schema.tables.push(tableInfo);
	}
	return schema;
}
