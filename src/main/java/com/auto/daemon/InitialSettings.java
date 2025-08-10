package com.auto.daemon;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auto.daemon.domain.Candle;
import com.auto.daemon.domain.UserVO;
import com.auto.daemon.domain.entity.UserInfoEntity;
import com.auto.daemon.service.UserInfoService;
import com.auto.daemon.util.CryptoUtil;
import com.auto.daemon.util.JsonUtil;
import com.auto.daemon.wsk.UpbitCandleFetcher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

@Component
public class InitialSettings {

	@Autowired
	private UserVO user;
	
	@Value("${daemon.api.key}")
	private String apiKey;
	
	@Value("${daemon.api.uri}")
	private String apiUri;
	
//    @Value("${daemon.market}")
    private static final List<String> marketList = Arrays.asList("KRW-ETH", "KRW-SOL", "KRW-XRP");
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final OkHttpClient client = new OkHttpClient();
	private final ObjectMapper mapper = new ObjectMapper();
	private WebSocket webSocket;
	private final Map<String, LinkedList<Candle>> marketCandleBuffer = new ConcurrentHashMap<>();
	private final Map<String, String> lastProcessedCandleTime = new ConcurrentHashMap<>();
	private boolean addFlag = false;
	private String nowAddMarket = "";
	private Algorithm algorithm;
	private String jwtToken = "";
	
	@Autowired
	private UserInfoService userInfoService;
	
	@Autowired
    private UpbitCandleFetcher fetcher;
	
	@PostConstruct
	private void postConstruct() {
		
		
		logger.info("==================== API INFO SETTING START ====================");
		
		try {
			
			// Api Aceess Info Memoty Setting
			UserInfoEntity userEntity = userInfoService.getUserInfo(apiKey, "Y");
			user.setAccessKey(userEntity.getAccessKey());
			user.setSecretKey(CryptoUtil.decrypt(userEntity.getSecretKey(), System.getProperty("crypto.encrypt.key")));
			
	    	algorithm = Algorithm.HMAC256(user.getSecretKey());
	        jwtToken = JWT.create()
	                .withClaim("access_key", user.getAccessKey())
	                .withClaim("nonce", UUID.randomUUID().toString())
	                .sign(algorithm);
			
	        // 특정 시점 데이터 조회
//	        Instant to = Instant.parse("2025-08-10T19:25:00Z");
	        
	        
          // WebSocket 연결
//			Request request = new Request.Builder().url("wss://api.upbit.com/websocket/v1").build();
//			webSocket = client.newWebSocket(request, new UpbitWebSocketListener());
//          // 1분봉 데이터 요청
//	        String codes = marketList.stream()
////	                .map(market -> "\"" + market + ".1\"")
//	        		.map(market -> "\"" + market + "\"")
//	                .collect(Collectors.joining(","));
//	        String message = String.format("[{\"ticket\":\"test\"},{\"type\":\"candle.1m\",\"codes\":[%s]},{\"format\":\"SIMPLE\"}]", codes);
//        
//	        webSocket.send(message);

        // 초기 버퍼 설정
//	        marketList.forEach(market -> {
//	        	marketCandleBuffer.put(market, new LinkedList<>());
//	        	lastProcessedCandleTime.put(market, "");
//	        });
         
		} catch (Exception e) {
			logger.error("************** API INFO SETTING ERROR **************");
			logger.error("Error Content : {}", e);
		}
		
		logger.info("==================== API INFO SETTING END ====================");
		
	}
	
	public class UpbitWebSocketListener extends WebSocketListener{
		
	    @Override
	    public void onMessage(WebSocket webSocket, String text) {
	    	try {
	    		
				Map<String, Object> data = mapper.readValue(text, Map.class);
				String market = (String) data.get("cd");
				if (!marketList.contains(market.replace(".1", ""))) return;
				Candle candle = mapper.convertValue(data, Candle.class);
				LinkedList<Candle> buffer = marketCandleBuffer.get(market.replace(".1", ""));
				synchronized (buffer) {
					// 중복 캔들 방지
					if (!buffer.isEmpty() && buffer.getLast().getCandleDateTimeUtc().equals(candle.getCandleDateTimeUtc())) {
						return;
					}
					buffer.add(candle);
					while (buffer.size() > 14) {
						buffer.removeFirst();
					}
				}
				
			} catch (Exception e) {
				logger.error("onMessge String Error : {}", e);
			}
	    }
	    
	    @Override
	    public void onMessage(WebSocket webSocket, ByteString bytes) {
	    	try {
				
	        	String jsonStr = JsonUtil.fromJson(bytes.string(StandardCharsets.UTF_8), JsonNode.class).toPrettyString();
	            Map<String, Object> data = mapper.readValue(jsonStr, Map.class);
	            String market = (String) data.get("cd");
	            if (!marketList.contains(market.replace(".1", ""))) return;
	            Candle candle = mapper.convertValue(data, Candle.class);
	            LinkedList<Candle> buffer = marketCandleBuffer.get(market.replace(".1", ""));
	            synchronized (buffer) {
	                // 중복 캔들 방지
	                if (!buffer.isEmpty() && buffer.getLast().getCandleDateTimeUtc().equals(candle.getCandleDateTimeUtc())) {
	                    return;
	                }
	                buffer.add(candle);
	                while (buffer.size() > 14) {
	                    buffer.removeFirst();
	                }
	            }
	    		
			} catch (Exception e) {
				logger.error("onMessge ByteString Error : {}", e);
			}	
	    }

	    @Override
	    public void onOpen(WebSocket webSocket, Response response) {
	    	logger.info("********************* WebSocket 연결 성공 *********************");
	    }

	    @Override
	    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
	    	logger.error("############ WebSocket 연결 실패: " + t.getMessage());
	    }
	    
	}
	
	@Scheduled(fixedRate = 5000) // 5초마다 1분봉 RSI 계산
	public void calcOneMinRSI() {
		
        Instant to = null;
        System.out.println("어디가 문제일까");
        try {
        	
        	System.out.println("주문 가능 금액 : " +  getKrwBalance());
        	//System.out.println(getCoinBalance());
        	
//        	//매수해야할때
//        	if(!addFlag) {
//        		
//        		for(String market : marketList) {
//        			List<Candles> candleResList = fetcher.fetchOneMinuteCandles(market, to);
//        			
//        			double rsi = CalcUtil.calcOneMinRSI(candleResList);
//        			logger.info("############ {} RSI : {}", market, rsi);
//        			
//        			if(rsi < 30 ) {
//        				// 사기
//        				marketSellAll(market);
//        			}
//        		}
//        	}else {
//        		
//        		List<Candles> candleResList = fetcher.fetchOneMinuteCandles(nowAddMarket, to);
//    			double rsi = CalcUtil.calcOneMinRSI(candleResList);
//    			logger.info("############ {} RSI : {}", nowAddMarket, rsi);
//        		if(rsi > 70 ) {
//        			
//    				// 팔기
//        			marketBuyAll(nowAddMarket);
//    			}
//        	}
        	
			logger.info("=============================================================");
		} catch (Exception e) {
			// TODO: handle exception
		}  
        
	}
	
    /**
     * 특정 마켓의 시장가 전액 매수 API
     *
     * @param market 마켓 코드 (예: "KRW-BTC")
     * @return 주문 결과 JSON 문자열
     * @throws Exception API 호출 실패 시
     */
    public String marketBuyAll(String market) throws Exception {
        // KRW 잔고 조회
        double krwBalance = getKrwBalance();
        if (krwBalance <= 0) {
            throw new Exception("No KRW balance available for buy");
        }

        // 시장가 매수 주문
        Map<String, Object> params = new HashMap<>();
        params.put("market", market);
        params.put("side", "bid");
        params.put("price", String.valueOf(krwBalance)); // 전액 매수
        params.put("ord_type", "price");

        return placeOrder(params);
    }
    
    /**
     * 특정 마켓의 시장가 전액 매도 API
     *
     * @param market 마켓 코드 (예: "KRW-BTC")
     * @return 주문 결과 JSON 문자열
     * @throws Exception API 호출 실패 시
     */
    public String marketSellAll(String market) throws Exception {
        // 코인 잔고 조회 (예: BTC 잔고)
        double coinBalance = getCoinBalance(market.split("-")[1]);
        if (coinBalance <= 0) {
            throw new Exception("No coin balance available for sell");
        }

        // 시장가 매도 주문
        Map<String, Object> params = new HashMap<>();
        params.put("market", market);
        params.put("side", "ask");
        params.put("volume", String.valueOf(coinBalance)); // 전액 매도
        params.put("ord_type", "market");

        return placeOrder(params);
    }
	
    private String placeOrder(Map<String, Object> params) throws Exception {
        String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(params),
                okhttp3.MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url("https://api.upbit.com/v1/orders")
                .addHeader("Authorization", "Bearer " + jwtToken)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            if (!response.isSuccessful()) {
                throw new Exception("Failed to place order: " + json);
            }
            return json;
        }
    }
    
    private double getKrwBalance() throws Exception {
        List<Map<String, Object>> accounts = getAccounts();
        for (Map<String, Object> account : accounts) {
            if ("KRW".equals(account.get("currency"))) {
                return Double.parseDouble((String) account.get("balance"));
            }
        }
        return 0;
    }
    
    private double getCoinBalance(String currency) throws Exception {
        List<Map<String, Object>> accounts = getAccounts();
        for (Map<String, Object> account : accounts) {
            if (currency.equals(account.get("currency"))) {
                return Double.parseDouble((String) account.get("balance"));
            }
        }
        return 0;
    }
    
    private List<Map<String, Object>> getAccounts() throws Exception {

        Request request = new Request.Builder()
                .url("https://api.upbit.com/v1/accounts")
                .addHeader("Authorization", "Bearer " + jwtToken)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            if (!response.isSuccessful()) {
                throw new Exception("Failed to get accounts: " + json);
            }
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        }
    }
   
}
