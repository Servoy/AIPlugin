/**
 * @properties={type:8,typeid:36,uuid:"FD3D4190-6BAA-41EA-9299-C649B1199A57"}
 */
function total()
{
	var sum = 0;
	for (var i = 1; i <= orders_to_order_details.getSize(); i++) {
		var record = orders_to_order_details.getRecord(i);
		sum += record.subtotal;
	}
	return sum;
}
