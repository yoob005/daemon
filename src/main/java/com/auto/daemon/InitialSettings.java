package com.auto.daemon;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auto.daemon.domain.Candle;
import com.auto.daemon.domain.UserVO;
import com.auto.daemon.domain.entity.UserInfoEntity;
import com.auto.daemon.service.CandleService;
import com.auto.daemon.service.MarketService;
import com.auto.daemon.service.UserInfoService;
import com.auto.daemon.util.CalcUtil;
import com.auto.daemon.util.CryptoUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import okhttp3.OkHttpClient;

@Component
public class InitialSettings {

	@Autowired
	private UserVO user;
	
	@Autowired
	private CandleService candleService;
	
	@Autowired
	private MarketService marketService;
	
	@Autowired
	private DaemonProperty daemonProp;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ObjectMapper mapper = new ObjectMapper();
	private boolean addFlag = false;
	private String nowAddMarket = "";
	private Algorithm algorithm;
	private String jwt = "";
	private List<String> marketList;
	
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
	
	@Autowired
	private UserInfoService userInfoService;
	
	@PostConstruct
	private void postConstruct() {
		
		
		logger.info("==================== API INFO SETTING START ====================");
		
		try {
				
			// Api Aceess Info Memoty Setting
			UserInfoEntity userEntity = userInfoService.getUserInfo(daemonProp.getApi().getKey(), "Y");
			user.setAccessKey(userEntity.getAccessKey());
			user.setSecretKey(CryptoUtil.decrypt(userEntity.getSecretKey(), System.getProperty("crypto.encrypt.key")));
			
			//Jwt Token Setting
	    	algorithm = Algorithm.HMAC256(user.getSecretKey());
	        jwt = JWT.create()
	                .withClaim("access_key", user.getAccessKey())
	                .withClaim("nonce", UUID.randomUUID().toString())
	                .sign(algorithm);
	        
	        marketList = daemonProp.getMarket();
	        
		} catch (Exception e) {
			logger.error("************** API INFO SETTING ERROR **************");
			logger.error("Error Content : {}", e);
		}
		
		logger.info("==================== API INFO SETTING END ====================");
		
	}
	
	
	@Scheduled(fixedRate = 1000) // 1초 주기로 1분봉 RSI 계산
	public void calcOneMinRSI() {
		
        try {       	
        	
        	Map<String, Object> result = new HashMap<String,Object>();
        	List<Candle> candleResList = null;
        	double rsi = 0L;
        	        	
        	//매수해야할때
        	if(!addFlag) {
        		
        		for(String market : marketList) {
        			candleResList = candleService.fetchOneMinuteCandles(market, null, client);
        			
        			rsi = CalcUtil.calcOneMinRSI(candleResList);
        			logger.info("############ {} RSI : {}", market, rsi);
        			
        			if(!addFlag && (rsi > 0 && rsi < 30)) {
        				// 전량매수
        				result = mapper.readValue(marketService.marketBuyAll(market, jwt, client), new TypeReference<Map<String,Object>>(){}) ;
        				if("".equals(result.get(""))) {
        					addFlag = true;
        					nowAddMarket = market;
        				}
        			}
        		}
        	// 매도해야할때
        	}else {
        		
        		candleResList = candleService.fetchOneMinuteCandles(nowAddMarket, null, client);
    			rsi = CalcUtil.calcOneMinRSI(candleResList);
    			logger.info("############ {} RSI : {}", nowAddMarket, rsi);
        		if(rsi > 70 ) {    			
    				// 전량매도
        			result = mapper.readValue(marketService.marketSellAll(nowAddMarket, jwt, client), new TypeReference<Map<String,Object>>(){}) ;
    				if("".equals(result.get(""))) {
    					addFlag = false;
    					nowAddMarket = "";
    				}
    			}
        	}
        	
			logger.info("=============================================================");
		} catch (Exception e) {
			// TODO: handle exception
		}  
        
	}
   
}
