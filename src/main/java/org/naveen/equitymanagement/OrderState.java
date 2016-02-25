package org.naveen.equitymanagement;

public interface OrderState {
	public void doTransition(OrderContext ctxt);
}
