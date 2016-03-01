package org.naveen.equitymanagement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class BuyAgent implements Callable<Void> {
	private final List<Company> companies;
	private final List<EquityOwner> owners;
	
	private static final BigDecimal PLUS_ONE_PERCENT = new BigDecimal("0.1");
	private static final BigDecimal MINUS_ONE_PERCENT = new BigDecimal("-0.1");
	
	public BuyAgent(List<Company> companies, List<EquityOwner> owners) {
		this.companies = companies;
		this.owners = owners;
	}

	@Override
	public Void call() throws Exception {
		return null;
	}
	
	private Order prepareOrder() {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		
		int companyId = r.nextInt(companies.size());
		Company c = companies.get(companyId);
		int quantity = r.nextInt(c.noOfShares());
		BigDecimal price = getPrice(c);
		int ownerId = r.nextInt(owners.size());
		EquityOwner owner = owners.get(ownerId);
		
		return new Order(OrderType.BUY, c, quantity, price, owner);
	}
	
	private BigDecimal getPrice(Company c) {
		ThreadLocalRandom r = ThreadLocalRandom.current();		
		BigDecimal cp = c.currentPrice();
		
		// if nextBoolean() is true, buy at +1% CP else buy at -1% CP
		if (r.nextBoolean()) {
			return cp.add(cp.multiply(PLUS_ONE_PERCENT));
		}
		else {
			return cp.subtract(cp.multiply(MINUS_ONE_PERCENT));
		}
		
	}

}
