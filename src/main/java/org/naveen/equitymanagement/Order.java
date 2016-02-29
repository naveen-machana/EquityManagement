package org.naveen.equitymanagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.naveen.equitymanagement.exception.EditCompletesOrderException;
import org.naveen.equitymanagement.exception.InvalidEditException;

public class Order implements Comparable<Order> {
	private static AtomicLong idProducer = new AtomicLong();
	private OrderState currentState;
	private static final Map<OrderState, Set<OrderState>> VALID_TRANSITIONS = validOrderStateTransitions();
	private static final Set<OrderState> TERMINATED_STATES = EnumSet.of(OrderState.CANCELLED, OrderState.EXECUTED);
	private final Company company;
	private int quantity;
	private int quantityExecuted;
	private int quantityNeeded;
	private final String orderId;
	private BigDecimal price;
	private final OrderType orderType;
	private final EquityOwner orderPlacer; 
	private final Order parent;
	
	private final List<OrderInfo> orderExecutionHistory = new ArrayList<>();
	private final List<OrderInfo> unmodifiableOrderHistory 
			= Collections.unmodifiableList(orderExecutionHistory);
	
	private final List<Order> childOrders = new ArrayList<>();
	private final List<Order> unmodifiableChildOrders = Collections.unmodifiableList(childOrders);
	
	public Order(OrderType orderType, Company company, 
			int quantity, BigDecimal price, EquityOwner user,
			Order parent) {
		this.company = company;
		this.orderPlacer = user;
		this.orderId = generateOrderId();
		this.quantity = quantity;
		this.quantityNeeded = quantity;
		this.price = price;
		this.orderType = orderType;
		this.parent = parent;
	}
	
	private String generateOrderId() {
		long id = idProducer.getAndIncrement();
		return company.quoteId() + id;
	}
	
	// TODO: validate if this is a valid change.
	private boolean isValidEdit(BigDecimal price, int quantity) {
		if (TERMINATED_STATES.contains(currentState)) return false;
		if (quantityExecuted > quantity) return false;
		return true;
	}
	
	private int deltaForEdit(int quantityEdit) {
		return quantityEdit - quantityExecuted;
	}
	
	private Order copyPropertiesIntoNew(BigDecimal price, int quantity) {
		return new Order(orderType, company, quantity, price, orderPlacer, this);
	}
	
	public Order edit(BigDecimal price, int quantity) throws InvalidEditException {	
		
		if (isValidTransition(currentState, OrderState.EDITED) &&
			isValidEdit(price, quantity)) {			
				doTransition(currentState, OrderState.EDITED);
				int deltaQuantity = deltaForEdit(quantity);
				if (deltaQuantity == 0) {
					doTransition(currentState, OrderState.EXECUTED);
					orderExecutionHistory.add(new OrderInfo(0, BigDecimal.ZERO, "Edit completes the order"));
					return this;
				}
				Order newO = copyPropertiesIntoNew(price, deltaQuantity);
				childOrders.add(newO);
				return newO;
		}
		
		throw new InvalidEditException();		
	}
	
	public boolean cancel() {
		if (isValidTransition(currentState, OrderState.CANCELLED)) {
				doTransition(currentState, OrderState.CANCELLED);
				orderExecutionHistory.add(new OrderInfo(0, BigDecimal.ZERO, "Order cancelled."));
				return true;
		}
		return false;
	}
	
	public boolean execute(BigDecimal price, int quantity) {
		int neededQuantity = neededQuantity();
		OrderState targetState = quantity >= neededQuantity ? 
								 OrderState.EXECUTED : 
								 OrderState.PARTIALLY_EXECUTED;
		int quantityToBeExec  = Math.min(quantity, neededQuantity);
		
		if (isValidTransition(currentState, targetState)) {
			doTransition(currentState, targetState);
			this.quantityExecuted += quantityToBeExec;
			orderExecutionHistory.add(new OrderInfo(quantityToBeExec, price, ""));
			return true;
		}
		
		return false;
	}
	
	public List<OrderInfo> orderHistory() {
		return unmodifiableOrderHistory;
	}
	
	public List<Order> forkedOrders() {
		return unmodifiableChildOrders;
	}
	
	public OrderState currentState() {
		return currentState;
	}
	
	public int neededQuantity() {
		return quantity - quantityExecuted;
	}
	
	private boolean isValidTransition(OrderState from, OrderState to) {
		Set<OrderState> states = VALID_TRANSITIONS.get(from);
		return states.contains(to);
	}
	
	private void setCurrentState(OrderState targetState) {
		this.currentState = targetState;
	}
	
	private void adjustOrderQuantities(int quantity) {
		this.quantityExecuted += quantity;
		this.quantityNeeded -= quantity;
	}
	
	private void delegateToParentQuantity(int quantity) {
		if (parent != null) {			
			parent.adjustOrderQuantities(quantity);
			parent.doTransition(parent.currentState, OrderState.EXECUTED);			
		}
	}
	
	public static class OrderInfo {
		private final int quantityExecuted;
		private final BigDecimal priceExecuted;
		private final String historyMessage;
		
		public OrderInfo(int quantityExecuted, BigDecimal priceExecuted, String message) {
			this.quantityExecuted = quantityExecuted;
			this.priceExecuted = priceExecuted;
			this.historyMessage = message;
		}
		
		public int quantityExecuted() { return quantityExecuted;}
		public BigDecimal priceExecuted() {return priceExecuted;}
		public String message() {return historyMessage; }
	}
	
	private void doTransition(OrderState current, OrderState target) {		
		this.setCurrentState(target);
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

	
	@Override
	public int compareTo(Order o) {
		int ot = orderType.compareTo(o.orderType); 
		if (ot != 0) {
			return ot;
		}
		
		int op = price.compareTo(o.price); 
		if (op != 0) {
			return op;
		}		
		
		int oq = Integer.compare(quantityNeeded, o.quantityNeeded);
		if (oq != 0) {
			return oq;
		}
		return orderId.compareTo(o.orderId);
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof Order)) return false;
		Order o = (Order)other;
		if (orderType != o.orderType) return false;
		if (price != o.price) return false;
		if (quantityNeeded != o.quantityNeeded) return false;
		if (!orderId.equals(o.orderId)) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		int r = 17;
		r = r * 31 + orderType.hashCode();
		r = r * 31 + Float.floatToIntBits(price.floatValue());
		r = r * 31 + quantityNeeded;
		r = r * 31 + orderId.hashCode();
		return r;
	}
}
