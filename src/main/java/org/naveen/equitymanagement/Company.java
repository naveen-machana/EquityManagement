package org.naveen.equitymanagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Company {
	private final String quoteId;
	private int noOfStocks;
	private final BigDecimal initialPrice;
	private BigDecimal currentPrice;
	private BigDecimal offeringPrice;
	private BigDecimal requestedPrice;
	
	private List<Equity> equities;
	public String quoteId() {
		return quoteId;
	}
	
	public Company(String quoteId, int noOfStocks, BigDecimal initialOfferingPrice) {
		this.quoteId = quoteId;
		this.noOfStocks = noOfStocks;
		this.initialPrice = initialOfferingPrice;
		this.currentPrice = initialPrice;
		this.offeringPrice = initialPrice;
		this.requestedPrice = initialPrice;
		
		equities = new ArrayList<>(noOfStocks);
		createEquities();
	}
	
	private void createEquities() {
		for (int i = 0; i < noOfStocks; i++) {
			
		}		
	}
	
	public int noOfShares() {
		return noOfStocks;
	}
	
	public BigDecimal currentPrice() { return currentPrice;	}
	public BigDecimal offeringPrice() { return offeringPrice; }
	public BigDecimal requestedPrice() { return requestedPrice; }
}
