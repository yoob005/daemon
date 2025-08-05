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
	private static Integer cnt = 0;
	private static long time_stt = 0L;
	
	public void proc() {
		
		if(cnt == 0) {
			time_stt = System.currentTimeMillis();
			logger.info("first access_key : " + user.getAccessKey());
			logger.info("first secret_key : " + user.getSecretKey());
		}
		
//		logger.info("==================== bargain.proc start ====================");
		if (cnt > 100) {
			logger.info("last_cnt : " + cnt);
			logger.info("dif time : " + this.diffTime(time_stt));
			logger.info("last access_key : " + user.getAccessKey());
			logger.info("last secret_key : " + user.getSecretKey());
			return;
		}else {
			logger.info("cnt : " + cnt);
			cnt ++;
		}
//		logger.info("==================== bargain.proc end ====================");
	}
	
	public static int diffTime(long startTimeMsec) {
		return (int)(System.currentTimeMillis() - startTimeMsec);
	}
}