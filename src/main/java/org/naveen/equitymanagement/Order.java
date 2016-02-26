package org.naveen.equitymanagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class Order {
	private static AtomicLong idProducer = new AtomicLong();
	private OrderState currentState;
	private static final Map<OrderState, Set<OrderState>> validTransitions = validOrderStateTransitions();
	private final Company company;
	private int quantity;
	private final String orderId;
	private BigDecimal price;
	private final OrderType orderType;
	private final EquityOwner orderPlacer; 
	private int quantityExecuted;
	private final List<OrderInfo> orderExecutionHistory = new ArrayList<>();
	
	public Order(OrderType orderType, Company company, 
			int quantity, BigDecimal price, EquityOwner user) {
		this.company = company;
		this.orderPlacer = user;
		this.orderId = generateOrderId();
		this.quantity = quantity;
		this.price = price;
		this.orderType = orderType;
	}
	
	private String generateOrderId() {
		long id = idProducer.getAndIncrement();
		return company.quoteId() + id;
	}
	
	// TODO: validate if this is a valid change.
	private boolean isValidEdit(BigDecimal price, int quantity) {
		return true;
	}
	
	public boolean edit(BigDecimal price, int quantity) {	
		
		if (isValidTransition(currentState, OrderState.EDITED) &&
			isValidEdit(price, quantity)) {
			OrderContext ctxt = new OrderContextImpl(this, OrderState.EDITED);
			currentState.doTransition(ctxt);
			this.price = price;
			this.quantity = quantity;
			return true;
		}
		
		return false;		
	}
	
	public boolean cancel() {
		if (isValidTransition(currentState, OrderState.CANCELLED)) {
			OrderContext ctxt = new OrderContextImpl(this, OrderState.CANCELLED);
			currentState.doTransition(ctxt);
			return true;
		}
		return false;
	}
	
	private boolean partiallyExecute(BigDecimal price, int quantity) {
		if (isValidTransition(currentState, OrderState.PARTIALLY_EXECUTED)) {
			OrderContext ctxt = new OrderContextImpl(this, OrderState.PARTIALLY_EXECUTED);
			currentState.doTransition(ctxt);
			return true;
		}
		return false;
	}
	
	public boolean execute(BigDecimal price, int quantity) {
		
		// if quantity sent to this method is less than quantity needed for this 
		// order then partially execute this order
		//if(quantity < this.quantity && orderType.isMatchablePrice(buyPrice, sellPrice))
		
		if (isValidTransition(currentState, OrderState.EXECUTED) //&& isValidExecutionPredicate(price, quantity)
			) {
			OrderContext ctxt = new OrderContextImpl(this, OrderState.EXECUTED);
			currentState.doTransition(ctxt);
			this.quantityExecuted += quantity;
			return true;
		}
		return false;
	}
	
	public OrderState currentState() {
		return currentState;
	}
	
	public boolean isValidTransition(OrderState from, OrderState to) {
		Set<OrderState> states = validTransitions.get(from);
		return states.contains(to);
	}
	
	public void setCurrentState(OrderState targetState) {
		this.currentState = targetState;
	}
	
	public static class OrderInfo {
		private final int quantityExecuted;
		private final BigDecimal priceExecuted;
		
		public OrderInfo(int quantityExecuted, BigDecimal priceExecuted) {
			this.quantityExecuted = quantityExecuted;
			this.priceExecuted = priceExecuted;
		}
		
		public int quantityExecuted() { return quantityExecuted;}
		public BigDecimal priceExecuted() {return priceExecuted;}
	}  
	
	private static Map<OrderState, Set<OrderState>> validOrderStateTransitions() {
		Map<OrderState, Set<OrderState>> orderStates = new EnumMap<>(OrderState.class);
		
		Set<OrderState> newStates = EnumSet.of(OrderState.CANCELLED, OrderState.EDITED, 
												OrderState.EXECUTED, OrderState.PARTIALLY_EXECUTED);
		
		orderStates.put(OrderState.NEW, newStates);
		
		Set<OrderState> editStates = EnumSet.of(OrderState.EDITED, OrderState.CANCELLED,
												OrderState.EXECUTED, OrderState.PARTIALLY_EXECUTED);
		
		orderStates.put(OrderState.EDITED, editStates);
		
		Set<OrderState> partExecStates = EnumSet.of(OrderState.EDITED, OrderState.CANCELLED,
													OrderState.PARTIALLY_EXECUTED, OrderState.EXECUTED);
		
		orderStates.put(OrderState.PARTIALLY_EXECUTED, partExecStates);		
		
		orderStates.put(OrderState.EXECUTED, new HashSet<OrderState>());
		orderStates.put(OrderState.CANCELLED, new HashSet<OrderState>());
		return orderStates;
	}
}
