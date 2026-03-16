package com.thesis.stocktradingsimulator.dto;

public class ChartPoint {
    public long timestamp;
    public String time;
    public double price;

    public ChartPoint(long timestamp, String time, double price) {
        this.timestamp = timestamp;
        this.time = time;
        this.price = price;
    }
}