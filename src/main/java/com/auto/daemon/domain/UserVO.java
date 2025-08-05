package com.auto.daemon.domain;

import org.springframework.stereotype.Component;

import lombok.Data;


@Component
@Data
public class UserVO {
	
	private String accessKey;
	private String secretKey;
	
}
