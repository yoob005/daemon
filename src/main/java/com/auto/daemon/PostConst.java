//package com.auto.daemon;
//
//import java.net.URISyntaxException;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//import javax.annotation.PostConstruct;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.auto.daemon.config.enums.SiseType;
//import com.auto.daemon.domain.Candle;
//import com.auto.daemon.domain.UserVO;
//import com.auto.daemon.domain.entity.UserInfoEntity;
//import com.auto.daemon.service.UserInfoService;
//import com.auto.daemon.util.CryptoUtil;
//import com.auto.daemon.util.JsonUtil;
//import com.auto.daemon.wsk.UpbitWebSocketListener;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//import okhttp3.WebSocket;
//import okhttp3.WebSocketListener;
//import okio.ByteString;
//
//@Component
//public class BargainScheduler {
//	
//	private final Logger logger = LoggerFactory.getLogger(this.getClass());
//	
//	@Autowired
//	private UserInfoService userInfoService;
//	
//	@Autowired
//	private UserVO user;
//	
//	@Value("${daemon.api.key}")
//	private String apiKey;
//	
//	@Value("${daemon.api.uri}")
//	private String apiUri;
//	
//    private final OkHttpClient client = new OkHttpClient();
//    private WebSocket webSocket;
//    private SiseType siseType;
//	
//	@PostConstruct
//	private void postConstruct() {
//		
//		
//		logger.info("==================== API INFO SETTING START ====================");
//		
//		try {
//			
//			// API ����� ���� ����
//			UserInfoEntity userEntity = userInfoService.getUserInfo(apiKey, "Y");
//			user.setAccessKey(userEntity.getAccessKey());
//			user.setSecretKey(CryptoUtil.decrypt(userEntity.getSecretKey(), System.getProperty("crypto.encrypt.key")));
//			// JWT 토큰 생성
//            String nonce = UUID.randomUUID().toString();
//            Algorithm algorithm = Algorithm.HMAC256(user.getSecretKey());
//            
//            String jwtToken = JWT.create()
//            					 .withClaim("access_key", user.getAccessKey())
//            					 .withClaim("nonce", nonce)
//            					 .sign(algorithm);
//
//            // 커스텀 헤더 설정
//            Map<String, String> headers = new HashMap<>();
//            headers.put("Authorization", "Bearer " + jwtToken);		
//            
//            // WebSocket 연결 설정
//            Request request = new Request.Builder().url("wss://api.upbit.com/websocket/v1").build();
//            webSocket = client.newWebSocket(request, new UpbitWebSocketListener());
//            
//            // 1분봉 데이터 요청
////            String codes = SELECTED_MARKETS.stream()
////                    //.map(market -> "\"" + market + ".1\"")
////            		.map(market -> "\"" + market + "\"")
////                    .collect(Collectors.joining(","));
////            String message = String.format("[{\"ticket\":\"test\"},{\"type\":\"candle.1m\",\"codes\":[%s]},{\"format\":\"SIMPLE\"}]", codes);
//            
////            webSocket.send(message);
//			
//            // 초기 버퍼 설정
////            SELECTED_MARKETS.forEach(market -> marketCandleBuffer.put(market, new LinkedList<>()));
//            
//		} catch (URISyntaxException e) {
//			logger.error("Invalid URI : {}", e.getMessage());
//			e.printStackTrace();
//			
//		} catch (Exception e) {
//			logger.error("************** API INFO SETTING ERROR **************");
//			logger.error("Error Content : {}", e);
//		}
//		
//		logger.info("==================== API INFO SETTING END ====================");
//		
//	}
//    
////    private double calculateRSI(LinkedList<Candle> candles) {
////        double sumGain = 0, sumLoss = 0;
////        for (int i = candles.size() - RSI_PERIOD + 1; i < candles.size(); i++) {
////            double change = candles.get(i).getTradePrice() - candles.get(i - 1).getTradePrice();
////            if (change > 0) sumGain += change;
////            else sumLoss -= change;
////        }
////        double avgGain = sumGain / RSI_PERIOD;
////        double avgLoss = sumLoss / RSI_PERIOD;
////        if (avgLoss == 0) return 100;
////        double rs = avgGain / avgLoss;
////        return 100 - (100 / (1 + rs));
////    }
//    
////    @Scheduled(fixedRate = 1000) // 1초마다 RSI 계산
////    public void calculateAndPrintRSI() {
////        for (String market : SELECTED_MARKETS) {
////            LinkedList<Candle> buffer = marketCandleBuffer.get(market);
////            synchronized (buffer) {
////                if (buffer.size() >= RSI_PERIOD) {
////                    double rsi = calculateRSI(buffer);
////                    System.out.println("Market: " + market + " RSI: " + rsi);
////                    System.out.println("------------------------");
////                }
////            }
////        }
////    }
//
//	}
//}
//
//
//


