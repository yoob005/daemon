package com.auto.daemon;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auto.daemon.domain.Candle;
import com.auto.daemon.domain.UserVO;
import com.auto.daemon.domain.entity.UserInfoEntity;
import com.auto.daemon.service.CandleService;
import com.auto.daemon.service.MarketService;
import com.auto.daemon.service.UserInfoService;
import com.auto.daemon.util.CalcUtil;
import com.auto.daemon.util.CryptoUtil;
import com.auto.daemon.util.StringUtil;
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
	
	private double downLimit;
	private double upLimit;
	private String candleMinute;
	
	private boolean addFlag;
	private String nowAddMarket = "";
	
	private List<String> marketList;
	
	private long lastDownTime = 0L;
	private int touchDownCnt = 0;
	
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
			
			// Trade Info Setting
	        marketList = daemonProp.getMarket();
	        
	        upLimit = daemonProp.getSetting().getRsiUpLimit();
	        downLimit = daemonProp.getSetting().getRsiDownLimit();
	        candleMinute = daemonProp.getSetting().getCandleMinute();
	        
		} catch (Exception e) {
			logger.error("************** API INFO SETTING ERROR **************");
			logger.error("Error Content : {}", e);
		}
		
		logger.info("==================== API INFO SETTING END ====================");
		
	}
	
	
	@Scheduled(fixedRate = 2500) // 2.5초 주기로 분봉 RSI 계산
	public void calcOneMinRSI() {
		
        try {       	
        	
        	Map<String, Object> result = new HashMap<String,Object>();
        	List<Candle> candleResList = null;
        	double rsi = 0L;
        	     	        	
        	// 매수한 종목 없을때
        	if(!addFlag) {
        		
        		for(String market : marketList) {
        			candleResList = candleService.fetchMinuteCandles(market, null, client, candleMinute);
        			
        			rsi = CalcUtil.calcOneMinRSI(candleResList);
        			logger.info("############ {} RSI : {}", market, rsi);
        			
        			if((rsi > 0 && rsi < downLimit) && touchDownCnt == 0) {
        				 
    					// 시드 65% 매수
    					result = mapper.readValue(marketService.marketBuyAll(market, client, true), new TypeReference<Map<String,Object>>(){}) ;
    					logger.info("1st 주문결과 : {}", result.toString());
    					if(!"".equals(StringUtil.getObjToString(result.get("uuid")))) {
    						addFlag = true;
    						nowAddMarket = market;
    						touchDownCnt++;
    						
    						Date date_now = new Date(System.currentTimeMillis());
    				    	SimpleDateFormat fourteen_format = new SimpleDateFormat("yyyyMMddHHmm");
    				    	
    				    	lastDownTime = Long.parseLong(fourteen_format.format(date_now));
    					}        					
        			}
        		}
        	// 매도해야할때
        	}else {
        		
        		candleResList = candleService.fetchMinuteCandles(nowAddMarket, null, client, candleMinute);
    			rsi = CalcUtil.calcOneMinRSI(candleResList);
    			logger.info("############ {} RSI : {} ", nowAddMarket, rsi);
    			
				Date date_now = new Date(System.currentTimeMillis());
		    	SimpleDateFormat fourteen_format = new SimpleDateFormat("yyyyMMddHHmm");
		    	Long now = Long.parseLong(fourteen_format.format(date_now));
		    	
		    	//RSI 기준 매매
		    	if(rsi < (downLimit - 3) && 60 > (now - lastDownTime) && touchDownCnt < 2) {
    				
					// 남은 절반 매수
					result = mapper.readValue(marketService.marketBuyAll(nowAddMarket, client, false), new TypeReference<Map<String,Object>>(){}) ;
					logger.info("전량 주문결과 : {}", result.toString());
					if(!"".equals(StringUtil.getObjToString(result.get("uuid")))) {
						addFlag = true;				
						touchDownCnt++;
					} 
		    	}else if(rsi < downLimit && 3 > (now - lastDownTime)){
	    				
	    			lastDownTime = now;    				
	    					    		
    			}else if(rsi > upLimit){
        	        
    				// 전량매도
        			result = mapper.readValue(marketService.marketSellAll(nowAddMarket, client), new TypeReference<Map<String,Object>>(){}) ;
        			logger.info("주문결과 : {}", result.toString());
        			if(!"".equals(StringUtil.getObjToString(result.get("uuid")))) {
    					addFlag = false;
    					nowAddMarket = "";
    					touchDownCnt = 0;
    					lastDownTime = 0L;
    				}
    			}
		    	
		    	// 수익률 기준 매도 로직
		    	
        	}
        	
			logger.info("=============================================================");
			
		} catch (Exception e) {
			logger.error("자동 매매 스케줄러 작동 중 오류 발생 : {}" ,e);
		}  
        
	}
	
	@Scheduled(fixedRate = 10000) // 수동으로 매도했을 경우를 고려해 작동하는 상태확인 스케줄러
	public void checkAccount() {
		
		try {
			
			if(addFlag && !"".equals(nowAddMarket)) {
				
				String chkMarket = nowAddMarket.split("-")[1];
				
				List<Map<String, Object>> accounts = marketService.getAccounts(client);
				boolean chkSell = false;
		        for (Map<String, Object> account : accounts) {
		        	logger.info(account.toString());
		            if (chkMarket.equals(account.get("currency"))) {
		            	chkSell = true;
		            }
		        }		
		        
		        if(!chkSell) {
		        	addFlag = false;	        	
		        	nowAddMarket = "";
		        	touchDownCnt = 0;
		        	lastDownTime = 0L;
		        	logger.info("########### 수동 매도로 인한 마켓 초기화 작업 실행 ###########");
		        }
			}
			
		} catch (Exception e) {
			logger.error("마켓 초기화 스케줄러 작동 중 오류 발생 : {}", e);
		}
				
	}
   
}
