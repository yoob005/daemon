package com.auto.daemon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartStopConfig implements InitializingBean, DisposableBean{

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void afterPropertiesSet() throws Exception{
		
		logger.info("========== InitializingBean afterPropertiesSet ==========");
		
	}
	
	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		logger.info("========== DisposableBean destroy ==========");
	}
	
}
