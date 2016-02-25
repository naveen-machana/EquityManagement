package org.naveen.equitymanagement;

import java.util.Map;
import java.util.Set;

public abstract class Order {
	private OrderState currentState;
	private Map<OrderState, Set<OrderState>> validTransitions;
	private Company company;
	private int quantity;
	private String orderId;
	
	public boolean edit() {
		return true;
	}
	
	public OrderState currentState() {
		return currentState;
	}
}
