package com.auto.daemon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BargainService {
		
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public void proc() {
		
		logger.info("==================== bargain.proc start ====================");
		
		logger.info("==================== bargain.proc end ====================");
	}
	
}