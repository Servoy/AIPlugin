/**
 * The name of the model to use, as understood by the model router.
 * Must be a model that supports tool/function calling.
 * @type {String}
 * @properties={typeid:35,uuid:"0AF42A2D-7525-4A2F-9BAF-5E7FCFF52D81"}
 */
var modelName = 'eu.anthropic.claude-sonnet-4-6';

/**
 * Base URL of the Servoy AI model router (OpenAI-compatible endpoint).
 * The API key comes from the 'router_api_key' servoy property via scopes.exampleAIPlugin.getRouterApiKey().
 * @type {String}
 * @properties={typeid:35,uuid:"94DB28CF-D38F-40F8-9973-45B88F4EB179"}
 */
var baseUrl = 'https://genai.servoy-cloud.eu/v1';

/**
 * The system message that turns the LLM into an iterative SQL research agent.
 * The agent is expected to plan and execute MULTIPLE queries (via the runSQL tool)
 * until it has gathered enough evidence, then summarize findings and recommendations.
 * @type {String}
 * @properties={typeid:35,uuid:"09AB64D4-4E90-4BD0-ABF9-5A913A9F1CB7"}
 */
var systemMessage = 'You are an expert SQL Data Researcher. You are given the database product, optional database hints, an index of available skill packs, a list of the available table NAMES, and a user research question. You do NOT get the full schema (columns) or the skill pack bodies up front - load them on demand.\
\n\nWork like a data analyst and investigate DEEPLY using a SERIES of queries - not just one:\
\n1. Review the skill pack index. For any pack that looks relevant to the question, call loadSkill(name) to read its full guidance BEFORE you make assumptions about what tables/columns mean. Skill packs of kind "pack" are authoritative; those of kind "learning" are helpful but lower-confidence hints from past runs - use judgement.\
\n2. From the table names, decide which tables look relevant. Call the describeTables tool (pass a comma-separated list of table names) to get their columns BEFORE you query them. Only describe the tables you actually need - do not describe them all.\
\n3. Use the runSQL tool to execute each query. Always pass a short natural-language "description" of what the query does (e.g. "Summarizing orders by country") along with the SQL - the description is shown to the user. It returns the result set as CSV text (the first row is the column names). Inspect the result and decide what to investigate next.\
\n4. Iterate: load more skills, describe more tables and run as many queries as you need (typically 3 to 8) until you have enough evidence to answer with confidence. Do NOT stop after a single query.\
\n5. If a tool returns a string starting with "ERROR", read the message, correct your input, and try again.\
\n6. When you discover a DURABLE, REUSABLE fact about the semantics of this database (e.g. the meaning of a status code, how revenue is calculated, a non-obvious join or a data-quality caveat), call saveLearning(name, description, content) to record it so future research starts smarter. Keep learnings concise and general - about the schema, not about the answer to this one question. Do NOT save findings, run-specific numbers, or things already covered by an existing skill pack.\
\n\nRules for the SQL you pass to runSQL:\
\n- Only use functions and syntax supported by the given database product.\
\n- Carefully follow the Database Hints (e.g. schema prefixes and identifier quoting) for EVERY query.\
\n- Write exactly ONE read-only SELECT statement per runSQL call. Never write INSERT, UPDATE, DELETE or DDL.\
\n- Do not wrap the SQL in markdown fences or add any commentary; pass only the raw SQL string.\
\n\nWhen you have gathered enough evidence, STOP calling tools and write your final answer as a concise report in markdown with exactly these two sections:\
\n**Key Findings** - the most important insights, each backed by specific numbers from the data you retrieved.\
\n**Recommendations** - concrete, actionable recommendations that follow from the findings.\
\nDo not describe your process or restate the queries; deliver only the insights and recommendations.';

/**
 * System message for generating a ChartJS visualization to support the findings.
 * @type {String}
 * @properties={typeid:35,uuid:"0B29BB6E-DC19-4FDD-9CB9-00FF5FE8DF6A"}
 */
var systemMessageChart = 'You are a data visualization expert. You will be given a user question, a findings summary, and one or more datasets in CSV format that were collected while researching the question. \
You will respond in strict JSON format following the format for well-known ChartJS data-structures. \
Pick the SINGLE most insightful chart that best supports the findings. \
Only return raw JSON. Do NOT wrap it in markdown code fences (no ```json) and do not include any other text or explanations. \
If the data does not lend itself to a meaningful chart, return an empty JSON object {}.';

/**
 * The user's research question.
 * @type {String}
 * @properties={typeid:35,uuid:"9267A793-4CBC-4FEA-AD46-CD586A7E4AED"}
 */
var userMessage = 'Which products and customers drive our revenue, and where are we at risk of losing sales?';

/**
 * Free-form hints about how to query THIS database: dialect quirks, schema prefixes,
 * identifier quoting, join tips, etc. These are injected into the agent's context and
 * strongly influence the SQL it writes. Leave empty for a plain database.
 *
 * Example for a Progress OpenEdge DB whose tables sit in a "PUB" schema:
 *   'Tables live in the "PUB" schema. Always schema-qualify and double-quote identifiers,
 *    e.g. SELECT * FROM PUB."Customer". Identifiers are case-sensitive.'
 *
 * @type {String}
 * @properties={typeid:35,uuid:"1C1FBED4-03C8-4942-A24D-B0E70FCCDB52"}
 */
var dbHints = 'Tables live in the "PUB" schema. Always schema-qualify and double-quote identifiers, e.g. SELECT * FROM "PUB"."Customer". Identifiers are case-sensitive.';

/**
 * The agent's final findings and recommendations report, as raw markdown.
 * Kept around as the canonical form (for copy/export); rendered via answerHtml.
 * @type {String}
 * @properties={typeid:35,uuid:"6D4E242F-25AD-4968-8502-EB82D717BA49"}
 */
var answer = '';

/**
 * The findings rendered to HTML (from the markdown in answer) for display in a label.
 * @type {String}
 * @properties={typeid:35,uuid:"1F9E74C0-15F5-4145-99EC-7D65DE7F32BA"}
 */
var answerHtml = '';

/**
 * When true, the research trace also shows the raw SQL under each step. End users can
 * be insulated from SQL (false) while developers flip it on to see the exact queries.
 * @type {Boolean}
 * @properties={typeid:35,uuid:"E0FDC1C5-1295-40F0-85CD-3A91EB0AF2F6",variableType:-4}
 */
var showSQL = false;

/**
 * The running research trace: a numbered, natural-language description of each step
 * the agent took (and, when showSQL is true, the SQL under each).
 * @type {String}
 * @properties={typeid:35,uuid:"93565589-1911-4F3E-8597-FC066C81B22D"}
 */
var sqlPlan = '';

/**
 * A short status line (query count / time / tokens).
 * @type {String}
 * @properties={typeid:35,uuid:"A0EA32D5-6F6F-4532-A9A7-1A659B1505C2"}
 */
var queryStatus = '';

/**
 * The raw ChartJS JSON returned by the visualization step.
 * @type {String}
 * @properties={typeid:35,uuid:"CF7E1B17-9961-44D1-9ECD-5CFD300603E5"}
 */
var chartData = '';

/**
 * The Servoy database server to research.
 * @type {String}
 * @properties={typeid:35,uuid:"D1BEE57F-ADFA-46F2-AEE5-A83A98B044BB"}
 */
var serverName = 'example';

/**
 * The Servoy database server that holds the "skill_packs" table (curated guidance
 * about this database's semantics, plus write-back agent learnings). Kept separate
 * from the researched database so knowledge is reusable across databases.
 * If the server/table is absent, the skill features degrade gracefully to a no-op.
 * @type {String}
 * @properties={typeid:35,uuid:"4E432010-4E69-4A9F-A452-FED4D2BCA479"}
 */
var skillServerName = 'ttyd';

/**
 * Accumulates each executed query and its CSV result, so the visualization step
 * has the underlying data to chart after the research loop finishes.
 * @type {Array<{description:String, sql:String, csv:String}>}
 * @properties={typeid:35,uuid:"AA7AF100-CE92-445C-9551-5ED7BC56F56B",variableType:-4}
 */
var researchData = [];

/**
 * How many queries the agent has executed in the current run.
 * @type {Number}
 * @properties={typeid:35,uuid:"923A90D6-0D56-44E1-8977-683FD1697202",variableType:8}
 */
var queryCount = 0;

/**
 * Entry point (button action). Kicks off the SQL research agent.
 *
 * The agent is a single LangChain4j tool-calling assistant: we register the runSQL
 * tool and let AiServices run the plan -> query -> observe -> refine loop autonomously
 * until the model decides it has enough evidence and returns its findings report.
 *
 * @properties={typeid:24,uuid:"6D05BB88-EF72-42BF-BDF6-D41D9854C7EE"}
 */
function research() {

	// reset state from any previous run
	answer = '';
	answerHtml = '';
	sqlPlan = '';
	queryStatus = '';
	chartData = '';
	researchData = [];
	queryCount = 0;

	var startTime = new Date().getTime();

	// build the research agent with the runSQL tool
	var client = getResearcherClient();

	// Only the cheap part (table names) goes into the prompt; the agent lazy-loads
	// column details for the tables it needs via the describeTables tool.
	var prompt = 'Database Product: ' + databaseManager.getDatabaseProductName(serverName)
		+ '\n\nDatabase Hints:\n' + (dbHints || '(none)')
		+ '\n\nSkill pack index (call loadSkill with the name to read the full guidance):\n' + listSkills()
		+ '\n\nAvailable tables (use the describeTables tool to get columns for the ones you need):\n' + listTableNames().join(', ')
		+ '\n\nResearch Question:\n' + userMessage;

	plugins.svyBlockUI.show('Researching your data...');
	client.chat(prompt).then(function(response) {
		queryStatus = 'Queries: ' + queryCount
			+ '  |  Time: ' + (new Date().getTime() - startTime) + ' ms'
			+ '  |  Tokens: ' + response.getTokenUsage().totalTokenCount();
		answer = response.getResponse();
		answerHtml = mdToHtml(answer);
		plugins.svyBlockUI.stop();

		// build a chart from the data the agent actually gathered
		generateChart();

	}).catch(function(e) {
		plugins.svyBlockUI.stop();
		answer = 'Error: ' + e.message;
		answerHtml = mdToHtml(answer);
	}).finally(function() {
		// release resources (no MCP here, but good practice per the plugin docs)
		client.close();
	});
}

/**
 * TOOL: executes a single read-only SELECT and returns the result set as CSV text.
 *
 * This is the function the agent calls (potentially many times) to research the data.
 * It logs every statement it runs - both to the server log and to the on-screen
 * research trace - and stashes the result for the later visualization step.
 *
 * Any SQL error is caught and returned to the model as text so the agent can read it
 * and self-correct on the next iteration, rather than aborting the whole run.
 *
 * @param {String} description A short natural-language description of what the query does (shown to the user).
 * @param {String} sql A single read-only SELECT statement.
 * @return {String} The result set as CSV, or an "ERROR: ..." message for the agent to recover from.
 *
 * @properties={typeid:24,uuid:"996E112C-BD96-4F70-A6E2-9DA3E7714463"}
 * @AllowToRunInFind
 */
function runSQL(description, sql) {

	// defensively strip any markdown fences the model may have added
	sql = utils.stringTrim(('' + sql).replace(/```sql/gi, '').replace(/```/g, ''));
	description = utils.stringTrim('' + (description || 'Running a query'));

	queryCount++;

	// The trace shows a plain-English step; the SQL itself is developer-only (showSQL).
	application.output('[SQLResearcher] Query ' + queryCount + ' (' + description + '): ' + sql, LOGGINGLEVEL.INFO);
	sqlPlan += queryCount + '. ' + description + '\n';
	if (showSQL) {
		sqlPlan += '   SQL: ' + sql + '\n';
	}
	sqlPlan += '\n';

	try {
		var ds = databaseManager.getDataSetByQuery(serverName, sql, null, 1000);
		var csv = ds.getAsText(',', '\n', '"', true);
		researchData.push({ description: description, sql: sql, csv: csv });
		application.output('[SQLResearcher] -> ' + ds.getMaxRowIndex() + ' row(s) returned', LOGGINGLEVEL.INFO);
		return csv;
	} catch (e) {
		var msg = 'ERROR: ' + e['message'];
		application.output('[SQLResearcher] ' + msg, LOGGINGLEVEL.WARNING);
		sqlPlan += showSQL ? ('   ' + msg + '\n\n') : '   (query failed - adjusting approach)\n\n';
		return msg;
	}
}

/**
 * Builds the research agent: a chat client with the research tools registered.
 * @private
 * @return {plugins.ai.ChatClient}
 * @properties={typeid:24,uuid:"9A319A0D-624E-4A72-9980-D008C524F3D4"}
 */
function getResearcherClient() {
	return plugins.ai.createOpenAiChatBuilder()
		.baseUrl(baseUrl)
		.apiKey(scopes.exampleAIPlugin.getRouterApiKey())
		.modelName(modelName)
		.addSystemMessage(systemMessage)
		.createTool(loadSkill, 'loadSkill',
			'Loads the full markdown guidance of a skill pack by its name (from the skill pack index). '
			+ 'Returns the pack content, or a string starting with "ERROR" if the name is unknown. Call this before relying on assumptions about table/column semantics.')
			.addStringParameter('name', 'The exact name of the skill pack to load, taken from the skill pack index.', true)
			.build()
		.createTool(saveLearning, 'saveLearning',
			'Records a durable, reusable fact about the semantics of this database so future research is smarter. '
			+ 'Upserts by name (re-saving the same name refines it). Use for schema semantics, NOT for run-specific findings. Returns a status string.')
			.addStringParameter('name', 'A short kebab-case slug identifying the learning, e.g. "order-status-codes".', true)
			.addStringParameter('description', 'A one-line summary shown in the skill pack index.', true)
			.addStringParameter('content', 'The full learning as concise markdown.', true)
			.build()
		.createTool(describeTables, 'describeTables',
			'Returns column metadata (name, type, length, primary key, foreign key, title, description) as JSON for one or more tables. '
			+ 'Pass a comma-separated list of table names taken from the available tables list. Call this to inspect a table before you query it.')
			.addStringParameter('tableNames', 'Comma-separated list of table names to describe, e.g. "orders,order_details".', true)
			.build()
		.createTool(runSQL, 'runSQL',
			'Executes a single read-only SQL SELECT statement against the database and returns the result set as CSV text (first row = column names). '
			+ 'Always provide "description" (a short plain-English summary of what the query does, shown to the user) as well as the SQL. '
			+ 'Use this repeatedly to research the data. If the SQL is invalid it returns a string starting with "ERROR" - read it, fix the SQL and retry.')
			.addStringParameter('description', 'A short natural-language description of what this query does, e.g. "Summarizing orders by country". Shown to the user.', true)
			.addStringParameter('sql', 'A single valid read-only SQL SELECT statement for the target database product.', true)
			.build()
		.build();
}

/**
 * Generates a ChartJS visualization from the data the agent gathered during research.
 * Mirrors the approach used in chatWithYourData: ask the model for strict ChartJS JSON.
 * @private
 * @properties={typeid:24,uuid:"A71EA20C-FFE4-42E3-8C1A-903E80BAB6D7"}
 */
function generateChart() {

	if (researchData.length == 0) {
		return;
	}

	// assemble the datasets the agent collected as labeled CSV blocks
	var context = '';
	for (var i = 0; i < researchData.length; i++) {
		context += 'Query ' + (i + 1) + ': ' + researchData[i].sql + '\n'
			+ researchData[i].csv + '\n\n';
	}

	var client = plugins.ai.createOpenAiChatBuilder()
		.baseUrl(baseUrl)
		.apiKey(scopes.exampleAIPlugin.getRouterApiKey())
		.modelName(modelName)
		.addSystemMessage(systemMessageChart)
		.build();

	var chartPrompt = 'User Question:\n' + userMessage
		+ '\n\nFindings Summary:\n' + answer
		+ '\n\nDatasets collected during research (CSV):\n' + context;

	plugins.svyBlockUI.show('Generating visualization...');
	client.chat(chartPrompt).then(function(response) {
		chartData = response.getResponse();
		application.output('Chart Data:\n' + chartData);

		// The model sometimes wraps the JSON in ```json fences or adds stray prose.
		// Extract just the JSON object (first '{' to last '}') before parsing.
		var start = chartData.indexOf('{');
		var end = chartData.lastIndexOf('}');
		if (start < 0 || end <= start) {
			application.output('No chart JSON returned; skipping chart.', LOGGINGLEVEL.INFO);
			return;
		}
		var data = JSON.parse(chartData.substring(start, end + 1));

		elements.chart.setData(data);
		elements.chart.setOptions(data.options);
		elements.chart.refreshChart();
	}).catch(function(e) {
		application.output('Error generating chart data: ' + e.message, LOGGINGLEVEL.WARNING);
	}).finally(function() {
		plugins.svyBlockUI.stop();
		client.close();
	});
}

/**
 * Returns the list of available table names for the configured server.
 * This is the CHEAP part of the schema and is injected into the agent's prompt so it
 * knows what exists without paying the token cost of every column of every table.
 * @private
 * @return {Array<String>}
 * @properties={typeid:24,uuid:"641111B7-C592-4E27-A191-11C9BA95DD51"}
 */
function listTableNames() {
	return databaseManager.getTableNames(serverName);
}

/**
 * TOOL: returns the column metadata for one or more tables as JSON, on demand.
 *
 * This is the lazy-load counterpart to listTableNames: the agent only pays the token
 * cost for the columns of the tables it actually decides to investigate. Metadata comes
 * from Servoy's database abstraction, so it is portable across database products.
 *
 * @param {String} tableNames A comma-separated list of table names to describe.
 * @return {String} JSON describing the requested tables' columns (or a per-table error).
 *
 * @properties={typeid:24,uuid:"85DA80E3-4F48-406D-A089-07DED9327477"}
 */
function describeTables(tableNames) {
	application.output('[SQLResearcher] describeTables: ' + tableNames, LOGGINGLEVEL.INFO);
	var result = { databaseType: databaseManager.getDatabaseProductName(serverName), tables: [] };
	var names = ('' + tableNames).split(',');
	for (var i = 0; i < names.length; i++) {
		var name = utils.stringTrim(names[i]);
		if (!name) {
			continue;
		}
		result.tables.push(buildTableInfo(name));
	}
	return JSON.stringify(result);
}

/**
 * Builds the column metadata object for a single table.
 * @private
 * @param {String} tableName
 * @return {Object}
 * @properties={typeid:24,uuid:"3C6945A2-0387-4858-89D9-ECD49C676E0F"}
 */
function buildTableInfo(tableName) {
	var table = databaseManager.getTable(serverName, tableName);
	if (!table) {
		return { table: tableName, error: 'Table not found. Use one of the names from the available tables list.' };
	}
	var tableInfo = { table: tableName, columns: [] };
	var PKColumns = table.getRowIdentifierColumnNames();
	var columns = table.getColumnNames();
	for (var j = 0; j < columns.length; j++) {
		var columnName = columns[j];
		var column = table.getColumn(columnName);
		tableInfo.columns.push({
			name: columnName,
			type: column.getTypeAsString(),
			length: column.getLength(),
			description: column.getDescription(),
			title: column.getTitle(),
			fkForTableName: column.getForeignType(),
			isPK: PKColumns.indexOf(columnName) != -1
		});
	}
	return tableInfo;
}

/**
 * Builds the always-in-context skill pack index: one cheap line per enabled pack
 * (name + kind + one-line description). The full body is NOT loaded here - the agent
 * pulls it on demand via loadSkill. Degrades to a friendly message if the skill server
 * or table is not present, so the form keeps working without it.
 * @private
 * @return {String}
 * @properties={typeid:24,uuid:"ECC19A97-D47E-41E7-A2D9-1C113E4C299B"}
 */
function listSkills() {
	try {
		var ds = databaseManager.getDataSetByQuery(skillServerName,
			'SELECT name, description, kind, confidence, enabled FROM skill_packs ORDER BY kind, name', null, 200);
		var lines = [];
		for (var i = 1; i <= ds.getMaxRowIndex(); i++) {
			var row = ds.getRowAsArray(i); // [name, description, kind, confidence, enabled]
			if (row[4] === false || row[4] === 0) {
				continue; // disabled
			}
			var tag = row[2] == 'learning' ? ('learning' + (row[3] != null ? ', confidence ' + row[3] : '')) : 'pack';
			lines.push('- ' + row[0] + ' [' + tag + ']: ' + row[1]);
		}
		return lines.length > 0 ? lines.join('\n') : '(no skill packs available)';
	} catch (e) {
		application.output('[SQLResearcher] skill index unavailable: ' + e['message'], LOGGINGLEVEL.INFO);
		return '(skill packs not configured)';
	}
}

/**
 * TOOL: loads the full markdown body of a skill pack by name and bumps its usage count.
 * @param {String} name The skill pack name (from the index).
 * @return {String} The pack content, or an "ERROR: ..." message.
 * @properties={typeid:24,uuid:"38727A55-7FBA-4A66-BC29-40273A602E5A"}
 * @AllowToRunInFind
 */
function loadSkill(name) {
	name = utils.stringTrim('' + name);
	application.output('[SQLResearcher] loadSkill: ' + name, LOGGINGLEVEL.INFO);
	sqlPlan += '[skill] load: ' + name + '\n\n';
	try {
		var fs = databaseManager.getFoundSet('db:/' + skillServerName + '/skill_packs');
		fs.find();
		fs.name = name;
		if (fs.search() == 0) {
			return 'ERROR: no skill pack named "' + name + '". Use a name from the skill pack index.';
		}
		var rec = fs.getRecord(1);
		rec.usage_count = (rec.usage_count || 0) + 1;
		databaseManager.saveData(rec);
		return rec.content;
	} catch (e) {
		application.output('[SQLResearcher] loadSkill error: ' + e['message'], LOGGINGLEVEL.WARNING);
		return 'ERROR: could not load skill pack: ' + e['message'];
	}
}

/**
 * TOOL: records a durable, reusable learning about the database semantics.
 * Upserts by name, but only ever touches kind='learning' rows - curated 'pack' rows
 * are protected from being overwritten by the agent.
 * @param {String} name        Kebab-case slug identifying the learning.
 * @param {String} description One-line summary shown in the index.
 * @param {String} content     The full learning as markdown.
 * @return {String} A status string (or an "ERROR: ..." message).
 * @properties={typeid:24,uuid:"97E68BFA-81C1-4CAE-8E95-708513E16353"}
 * @AllowToRunInFind
 */
function saveLearning(name, description, content) {
	name = utils.stringTrim('' + name);
	application.output('[SQLResearcher] saveLearning: ' + name, LOGGINGLEVEL.INFO);
	sqlPlan += '[learning] save: ' + name + '\n\n';
	try {
		var fs = databaseManager.getFoundSet('db:/' + skillServerName + '/skill_packs');
		fs.find();
		fs.name = name;
		var rec;
		if (fs.search() > 0) {
			rec = fs.getRecord(1);
			if (rec.kind == 'pack') {
				return 'ERROR: "' + name + '" is a curated skill pack and must not be overwritten. Choose a different learning name.';
			}
		} else {
			rec = fs.getRecord(fs.newRecord());
			rec.name = name;
			rec.kind = 'learning';
			rec.confidence = 60;
			rec.source = 'agent:sqlResearcher';
			rec.enabled = true;
			rec.usage_count = 0;
			rec.created_at = new Date();
		}
		rec.description = ('' + description).substring(0, 500);
		rec.content = '' + content;
		rec.modified_at = new Date();
		databaseManager.saveData(rec);
		return 'Saved learning "' + name + '".';
	} catch (e) {
		application.output('[SQLResearcher] saveLearning error: ' + e['message'], LOGGINGLEVEL.WARNING);
		return 'ERROR: could not save learning: ' + e['message'];
	}
}

/**
 * Minimal, dependency-free markdown -> HTML converter for the findings display.
 * Handles the subset the agent emits: headings, bold, inline code, unordered lists
 * and paragraphs. The source is HTML-escaped FIRST, so any stray markup the model
 * produces is rendered as text and cannot inject into the label.
 * @private
 * @param {String} md The markdown text.
 * @return {String} HTML safe to bind to a label.
 * @properties={typeid:24,uuid:"29CB916E-4CCE-4C9D-8C72-87F367E96EB9"}
 */
function mdToHtml(md) {
	if (!md) {
		return '';
	}
	// escape first - prevents any raw HTML in the model output from being interpreted
	var esc = ('' + md).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
	var lines = esc.split(/\r?\n/);
	var html = [];
	var inList = false;

	for (var i = 0; i < lines.length; i++) {
		// inline formatting: **bold** and `code`
		var line = lines[i]
			.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
			.replace(/`([^`]+)`/g, '<code>$1</code>');
		var trimmed = line.replace(/^\s+/, '');

		var heading = trimmed.match(/^(#{1,6})\s+(.*)$/);
		var listItem = trimmed.match(/^[-*]\s+(.*)$/);

		if (heading) {
			if (inList) { html.push('</ul>'); inList = false; }
			var level = Math.min(heading[1].length + 2, 6); // # -> h3
			html.push('<h' + level + '>' + heading[2] + '</h' + level + '>');
		} else if (listItem) {
			if (!inList) { html.push('<ul>'); inList = true; }
			html.push('<li>' + listItem[1] + '</li>');
		} else if (trimmed === '') {
			if (inList) { html.push('</ul>'); inList = false; }
		} else {
			if (inList) { html.push('</ul>'); inList = false; }
			html.push('<p>' + line + '</p>');
		}
	}
	if (inList) {
		html.push('</ul>');
	}
	return html.join('');
}
