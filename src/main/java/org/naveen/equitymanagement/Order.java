package org.naveen.equitymanagement;

import java.util.Map;
import java.util.Set;

public abstract class Order {
	private OrderState currentState;
	private Map<OrderState, Set<OrderState>> validTransitions;
	private Company company;
	private int quantity;
	private String orderId;
	
	public boolean edit(OrderInfo info) {
		OrderContext ctxt = new OrderContextImpl(this, info);
		currentState.doTransition(ctxt);
		return true;
	}
	
	public boolean cancel() {
		//OrderContext
		//currentState.doTransition(ctxt);
		return false;
	}
	
	public OrderState currentState() {
		return currentState;
	}
}
