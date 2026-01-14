/**
 * @properties={typeid:24,uuid:"8C66ECEB-1FE7-44D2-B311-CDE3327B4A9F"}
 */
function relookupPrice()
{
	getSelectedRecord().unitprice = getSelectedRecord().order_details_to_products.unitprice;
	
}
