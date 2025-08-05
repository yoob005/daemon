package com.auto.daemon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auto.daemon.domain.UserVO;

@Service
public class BargainService {
	
	@Autowired
	private UserVO user;
		
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public void proc() {
		
		//HttpRe
		
	}
	
	public static int diffTime(long startTimeMsec) {
		return (int)(System.currentTimeMillis() - startTimeMsec);
	}
}