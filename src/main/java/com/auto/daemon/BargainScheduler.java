package com.auto.daemon;

import javax.annotation.PostConstruct;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auto.daemon.domain.UserVO;
import com.auto.daemon.domain.entity.UserInfoEntity;
import com.auto.daemon.service.BargainService;
import com.auto.daemon.service.UserInfoService;
import com.auto.daemon.util.CryptoUtil;
import com.auto.daemon.util.StringUtil;

@Component
public class BargainScheduler {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private BargainService bargainService;
	
	@Autowired
	private UserInfoService userInfoService;
	
	@Autowired
	private UserVO user;
	
	@Value("${daemon.api.key}")
	private String apiKey;
	
	@PostConstruct
	private void postConstruct() {
		
		
		logger.info("==================== API INFO SETTING START ====================");
		
		try {
			UserInfoEntity userEntity = userInfoService.getUserInfo(apiKey, "Y");
			user.setAccessKey(userEntity.getAccessKey());
			user.setSecretKey(CryptoUtil.decrypt(userEntity.getSecretKey(), System.getProperty("crypto.encrypt.key")));			
		} catch (Exception e) {
			logger.error("************** API INFO SETTING ERROR **************");
			logger.error("Error Content : {}", e);
		}
		
		logger.info("==================== API INFO SETTING END ====================");
		
	}
	
	@Scheduled(initialDelay = 700, fixedDelay = 700)
	public void fixedDelayBarginTriggerJob() throws Exception{
		
		bargainService.proc();
		
	}
}