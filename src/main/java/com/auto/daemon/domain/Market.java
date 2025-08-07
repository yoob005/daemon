package com.auto.daemon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Market {

	private String market;
    @JsonProperty("korean_name") private String koreanName;
    @JsonProperty("english_name") private String englishName;
	
}
