package org.naveen.equitymanagement;

enum OrderState {
	NEW, CANCELLED, EDITED, PARTIALLY_EXECUTED, EXECUTED;
	
	void doTransition(OrderContext ctxt) {
		Order order = ctxt.order();
		
		OrderState current = order.currentState();
		OrderState target = ctxt.targetState();
		order.setCurrentState(target);
	}
}
