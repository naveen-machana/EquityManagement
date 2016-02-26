package org.naveen.equitymanagement;

import java.math.BigDecimal;

public enum OrderType {
	BUY {
		public boolean isMatchablePrice(BigDecimal buyPrice, BigDecimal sellPrice) {
			return buyPrice.compareTo(sellPrice) <= 1;
		}
	}, SELL {
		public boolean isMatchablePrice(BigDecimal buyPrice, BigDecimal sellPrice) {
			return buyPrice.compareTo(sellPrice) >= 1;
		}
	};
	
	public abstract boolean isMatchablePrice(BigDecimal buyPrice, BigDecimal sellPrice);
}
