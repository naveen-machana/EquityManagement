package org.naveen.equitymanagement;

public class OrderContextImpl implements OrderContext {
	
	private Order order;
	private OrderInfo info;
	
	public OrderContextImpl(Order order, OrderInfo info) {
		this.order = order;
	}

	@Override
	public Order order() {
		return null;
	}

}
