/**
 * @type {String}
 *
 * @properties={typeid:35,uuid:"2CBE6784-806B-486F-A0A0-56F9326BB8D7"}
 */
var content = 'Hi Bob,\n\nI would like to order 100 more units Chai.\n\nThanks,\nAna Trujillo';

/**
 * @type {plugins.ai.EmbeddingStore}
 *
 * @properties={typeid:35,uuid:"12A222AE-1F0E-40AE-A455-D4F41344C21C",variableType:-4}
 */
var productStore;

/**
 * @properties={typeid:24,uuid:"CAA85EED-6CB6-4E00-BE38-DBB441E060DD"}
 */
function testTool(){
	/** @type {plugins.ai.ChatClient} */
	let model = plugins.ai.createOpenAiChatBuilder()
		.modelName('gpt-4.1')
		.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.createTool(productLookUp,'productLookUp','Looks up a product by name and returns the product ID (Integer). If not found, returns -1.')
			.addStringParameter('productName','The name of the product',true)
			.build()
		.createTool(customerLookUp,'customerLookUp','Looks up a customer by name and returns the customer ID (String). If not found, returns an empty string.')
			.addStringParameter('customerName','The name of the customer',true)
			.build()
		.createTool(createOrder,'createOrder','Creates a new order for the given customer ID.')
			.addStringParameter('customerId','The ID of the customer',true)
			.build()
		.createTool(addItemToOrder,'addItemToOrder','Adds an item to the current order with the given product ID and quantity.')
			.addNumberParameter('productId','The ID of the product',true)
			.addNumberParameter('quantity','The quantity of the product to add',true)
			.build()
		.build();
	
	let instruction = 'You are an automated ordering system. Your task is to process customer orders received via email.\n---\n' + content;
	plugins.svyBlockUI.show("Processing order, please wait...");
	model.chat(instruction).then(function(response){
		application.output('Response: ' + response.getResponse());
	}).catch(function(err){
		application.output('Error: ' + err);
	}).finally(function(){
		plugins.svyBlockUI.stop();
	});
}


/**
 * 
 * @param productName
 * @return {Number} the ID of the product
 * @properties={typeid:24,uuid:"381C5C21-BCB1-4BF7-9790-00CBF41FFDCC"}
 * @AllowToRunInFind
 */
function productLookUp(productName){
	application.output('TOOL CALLED');
	var fs = datasources.db.example_data.products.getFoundSet();
	fs.find();
	fs.productname = '#%' + productName + '%';
	if(fs.search() > 0){
		application.output('Found product: ' + fs.productname + ' with ID ' + fs.productid);
		return fs.productid;
	}
	
	// try vector search
	application.output('No match. Fallback vector search for product: ' + productName);
	let results = productStore.search(productName, 5);
	let id = results[0].getMetadata()['id'];
	let score = results[0].getScore();
	application.output('Vector search found product ID ' + id + ' with score ' + score);
	if(score > 0.7){
		return id;
	}
	
	application.output('Product not found: ' + productName);
	return -1;
}

/**
 * TODO generated, please specify type and doc for the params
 * @param customerName
 * @return {String} the ID of the customer
 * @properties={typeid:24,uuid:"9C674E27-9B37-49E0-93F5-447CEC141028"}
 * @AllowToRunInFind
 */
function customerLookUp(customerName){
	application.output('TOOL CALLED: Look up customer');
	var fs = datasources.db.example_data.customers.getFoundSet();
	fs.find();
	fs.companyname = '#%' + customerName + '%';
	fs.newRecord();
	fs.contactname = '#%' + customerName + '%';
	if(fs.search() > 0){
		application.output('Found customer: ' + fs.companyname + ' with ID ' + fs.customerid);
		return fs.customerid;
	}
	application.output('Customer not found: ' + customerName);
	return '';
}


/**
 * TODO generated, please specify type and doc for the params
 * @param customerId
 *
 * @properties={typeid:24,uuid:"CABC7A34-5B53-4AE1-9C0A-2A9EF2C43092"}
 */
function createOrder(customerId){
	application.output('TOOL CALLED: Create order for customer ID ' + customerId);
	foundset.newRecord()
	foundset.customerid = customerId;
	application.output('Created order for customer ID: ' + customerId);
}

/**
 * TODO generated, please specify type and doc for the params
 * @param productId
 * @param quantity
 *
 * @properties={typeid:24,uuid:"88938FBD-C6AE-4FDF-9D99-A92BA3F61502"}
 */
function addItemToOrder(productId, quantity){
	application.output('TOOL CALLED: Add item to order');
	orders_to_order_details.newRecord();
	orders_to_order_details.productid = productId;
	orders_to_order_details.quantity = quantity;
	orders_to_order_details.relookupPrice();
	application.output('Added product ID ' + productId + ' with quantity ' + quantity + ' to order.');
}
/**
 * Embed prodsuct names on form show.
 *
 * @param {Boolean} firstShow form is shown first time after load
 * @param {JSEvent} event the event that triggered the action
 *
 * @private
 *
 * @properties={typeid:24,uuid:"72276663-9779-4D6C-A8C9-7941516285AA"}
 */
function onShow(firstShow, event) {
	foundset.clear();
	if(firstShow){
		embedProductNames();
	}
}

/**
 * @private 
 * @properties={typeid:24,uuid:"0245D8A2-914A-4515-949F-9E1E7C5EC5AD"}
 */
function embedProductNames(){
	productStore = plugins.ai.createOpenAiEmbeddingModelBuilder()
		.apiKey(scopes.exampleAIPlugin.getOpenAIApiKey())
		.modelName('text-embedding-3-small')
		.build()
		.createInMemoryStore();
	
	var fs = datasources.db.example_data.products.getFoundSet();
	fs.loadAllRecords();
	var texts = [];
	var metadata = [];
	for (var record of fs) {
		texts.push('The product name is: ' + record.productname);
        metadata.push({'id':record.productid, 'productname':record.productname});
	}
	
	productStore.embed(texts,metadata);
}
/**
 * Fired when the button is clicked.
 *
 * @param {JSEvent} event
 *
 * @private
 *
 * @properties={typeid:24,uuid:"8E9C9D9E-3E8C-447C-9AD6-163310FC9633"}
 */
function clear(event) {
	foundset.revertEditedRecords();
}
