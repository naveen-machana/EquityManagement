package org.naveen.equitymanagement;

public class OrderContextImpl implements OrderContext {
	
	private final Order order;
	private final OrderState targetState;
	
	public OrderContextImpl(Order order, OrderState targetState) {
		this.order = order;
		this.targetState = targetState();
	}

	@Override
	public Order order() {
		return order;
	}

	@Override
	public OrderState targetState() {
		return this.targetState;
	}

}
