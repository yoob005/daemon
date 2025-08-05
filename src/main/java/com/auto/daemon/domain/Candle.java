package com.auto.daemon.domain;

public class Candle {

	public long minuteTimestamp;
    public double closePrice;

    public Candle(long minuteTimestamp, double closePrice) {
        this.minuteTimestamp = minuteTimestamp;
        this.closePrice = closePrice;
    }
	
}
