package com.auto.daemon.domain;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Candle {

	@JsonProperty("ty") private String type;
	@JsonProperty("cd") private String code;
    @JsonProperty("cdttmu") private String candleDateTimeUtc;
    @JsonProperty("cdttmk") private String candleDateTimeKr;
    @JsonProperty("op") private double openingPrice;
    @JsonProperty("hp") private double highPrice;
    @JsonProperty("lp") private double lowPrice;
    @JsonProperty("tp") private double tradePrice;
    @JsonProperty("catv") private double catv;
    @JsonProperty("catp") private double catp;
    @JsonProperty("tms") private Timestamp timestamp;
    @JsonProperty("st") private String realtime;
	
}
