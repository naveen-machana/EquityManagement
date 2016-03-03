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

public class Order implements Comparable<Order> {
	private static AtomicLong idProducer = new AtomicLong();
	private OrderState currentState;
	private static final Map<OrderState, Set<OrderState>> VALID_TRANSITIONS = validOrderStateTransitions();
	private static final Set<OrderState> TERMINATED_STATES = EnumSet.of(OrderState.CANCELLED, OrderState.EXECUTED);
	private final Company company;
	private final int quantity;
	private int quantityExecuted = 0;
	private final String orderId;
	private final BigDecimal price;
	private OrderType orderType;
	private final EquityOwner orderPlacer; 
	private final Order parent;
	
	private final List<OrderInfo> orderExecutionHistory = new ArrayList<>();
	private final List<OrderInfo> unmodifiableOrderHistory 
			= Collections.unmodifiableList(orderExecutionHistory);
	
	private Order forkedOrder = null;
	
	public Order(OrderType orderType, Company company, 
			int quantity, BigDecimal price, EquityOwner user) {
		
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Invalid value provided for price");
		}
		
		if (quantity <= 0) {
			throw new IllegalArgumentException("Invalid value provided for quantity");
		}
		
		this.company = company;
		this.orderPlacer = user;
		this.orderId = generateOrderId();
		this.quantity = quantity;
		this.price = price;
		this.orderType = orderType;
		this.parent = null;
		this.currentState = OrderState.NEW;
	}
	
	private Order(OrderType orderType, Company company, 
			int quantity, BigDecimal price, EquityOwner user, Order parent) {
		
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Invalid value provided for price");
		}
		
		if (quantity <= 0) {
			throw new IllegalArgumentException("Invalid value provided for quantity");
		}
		this.company = company;
		this.orderPlacer = user;
		this.orderId = generateOrderId();
		this.quantity = quantity;
		this.price = price;
		this.orderType = orderType;
		this.parent = parent;
		this.currentState = OrderState.NEW;
	}
	
	private String generateOrderId() {
		long id = idProducer.getAndIncrement();
		return company.quoteId() + id;
	}
	
	private int deltaForEdit(int quantityEdit) {
		return quantityEdit - quantityExecuted;
	}
	
	private Order copyPropertiesIntoNew(BigDecimal price, int quantity) {
		return new Order(orderType, company, quantity, price, orderPlacer, this);
	}
	
	public Order edit(BigDecimal price, int editQuantity){	
		
		if (isValidTransition(OrderState.EDITED)) {
			if (editQuantity > quantityExecuted) {
				doTransition(currentState, OrderState.EDITED);
				setParentState(currentState);
				int deltaQuantity = deltaForEdit(editQuantity);				
				Order newO = copyPropertiesIntoNew(price, deltaQuantity);
				forkedOrder = newO;
				return newO;
			}
			else {
				cancel();
				return this;
			}				
		}	
		throw new IllegalStateException();
	}
	
	public boolean cancel() {
		if (isValidTransition(OrderState.CANCELLED)) {
			doTransition(currentState, OrderState.CANCELLED);
			setParentState(currentState);
			orderExecutionHistory.add(new OrderInfo(0, BigDecimal.ZERO, "Order cancelled."));
			return true;
		}
		throw new IllegalStateException();
	}
	
	public boolean execute(BigDecimal price, int quantityE) {
		
		if (TERMINATED_STATES.contains(currentState)) {
			adjustOrderQuantities(quantityE);
			//orderExecutionHistory.add(new OrderInfo(quantityToBeExec, price, ""));
			return true;
		}
		
		throw new IllegalStateException();
	}
	
	private void adjustOrderQuantities(int q) {
		int neededQuantity = neededQuantity();		
		OrderState targetState = quantity >= neededQuantity ? 
				 OrderState.EXECUTED : 
				 OrderState.PARTIALLY_EXECUTED;
		int quantityToBeExecuted  = Math.min(q, neededQuantity);
		this.quantityExecuted += quantityToBeExecuted;
		doTransition(currentState, targetState);	
		setParentState(targetState);
	}
	
	private void setParentState(OrderState targetState) {
		if (parent != null) {			
			parent.doTransition(currentState, targetState);
			parent.setParentState(targetState);
		}
	}
	
	public List<OrderInfo> orderHistory() {
		return unmodifiableOrderHistory;
	}
	
	public Order childOrder() {
		return forkedOrder;
	}
	
	public OrderState currentState() {
		return currentState;
	}
	
	public int neededQuantity() {
		return quantity - quantityExecuted;
	}
	
	private boolean isValidTransition(OrderState to) {
		OrderState from = currentState; 
		Set<OrderState> states = VALID_TRANSITIONS.get(from);
		return states.contains(to);
	}
	
	public void flipOrderType() {
		orderType = orderType == OrderType.BUY ? OrderType.SELL : OrderType.BUY;
	}
	
	private void setCurrentState(OrderState targetState) {
		this.currentState = targetState;
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
		
		int thisNeededQuantity = neededQuantity();
		int otherNeededQuantity = neededQuantity();
		
		int oq = Integer.compare(thisNeededQuantity, otherNeededQuantity);
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
		if (this.neededQuantity() != o.neededQuantity()) return false;
		if (!orderId.equals(o.orderId)) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		int r = 17;
		r = r * 31 + orderType.hashCode();
		r = r * 31 + Float.floatToIntBits(price.floatValue());
		r = r * 31 + this.neededQuantity();
		r = r * 31 + orderId.hashCode();
		return r;
	}
}
