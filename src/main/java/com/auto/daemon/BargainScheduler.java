package com.auto.daemon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auto.daemon.service.BargainService;

@Component
public class BargainScheduler {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private BargainService bargainService;
	
//	@PostConstruct
//	private void postConstruct() throws Exception {
//		
//		logger.info("==================== postConstrunct start ====================");
//		
//		logger.info("==================== postConstrunct end ====================");
//		
//	}
	
	@Scheduled(initialDelay = 5000, fixedDelay = 5000)
	public void fixedDelayBarginTriggerJob() throws Exception{
		
		logger.info("==================== fixedDelayBarginTriggerJob start ====================");
		
		bargainService.proc();
		
		logger.info("==================== fixedDelayBarginTriggerJob end ====================");
		
	}
}