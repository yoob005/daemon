package com.auto.daemon;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
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
import com.auto.daemon.util.CalcUtil;
import com.auto.daemon.util.CryptoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BargainScheduler {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
//	@Autowired
//	private BargainService bargainService;
	
	@Autowired
	private UserInfoService userInfoService;
	
	@Autowired
	private UserVO user;
	
	@Value("${daemon.api.key}")
	private String apiKey;
	
	@Value("${daemon.api.uri}")
	private String apiUri;
	
	private WebSocketClient client;
	private final ConcurrentLinkedQueue<String> dataQueue = new ConcurrentLinkedQueue<>();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Map<String, List<Candle>> marketCandles = new HashMap<>();
    private final Map<String, Long> marketCurrentMinute = new HashMap<>();
    private final Map<String, Double> marketLastPrice = new HashMap<>();
	
	@PostConstruct
	private void postConstruct() {
		
		
		logger.info("==================== API INFO SETTING START ====================");
		
		try {
			
			// API ����� ���� ����
			UserInfoEntity userEntity = userInfoService.getUserInfo(apiKey, "Y");
			user.setAccessKey(userEntity.getAccessKey());
			user.setSecretKey(CryptoUtil.decrypt(userEntity.getSecretKey(), System.getProperty("crypto.encrypt.key")));
			// JWT 토큰 생성
            String nonce = UUID.randomUUID().toString();
            Algorithm algorithm = Algorithm.HMAC256(user.getSecretKey());
            
            String jwtToken = JWT.create()
            					 .withClaim("access_key", user.getAccessKey())
            					 .withClaim("nonce", nonce)
            					 .sign(algorithm);

            // 커스텀 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + jwtToken);			
			
			client = new WebSocketClient(new URI(apiUri), headers) {				

				@Override
				public void onOpen(ServerHandshake handshakedata) {
					logger.info("Connect Success"); 
					
					//String subscription = "[{\"ticket\":\"" + UUID.randomUUID().toString() + "\"}," + "{\"type\":\"ticker\",\"codes\":[\"KRW-BTC\"]}," + "{\"format\":\"SIMPLE\"}]";
					//String subscription = "[{"ticket":"UNIQUE_TICKET"},{"type":"ticker","codes":["KRW-BTC","KRW-ETH"]},{"format":"SIMPLE"}]"
					//send(subscription);
					sendSubscription();
				}
				
				@Override
				public void onMessage(String message) {
					// ���ŵ� �����͸� ť�� ����
					logger.info("요기요ㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛㅛ...");
					dataQueue.add(message);
					logger.info("사이즈 : " + dataQueue.size());
				}
				
				@Override
				public void onError(Exception ex) {
					logger.error("Connect Fail : {}", ex);
					
				}
				
				@Override
				public void onClose(int code, String reason, boolean remote) {
					logger.info("DisConnected: {} (Code: {})", reason, code);
					
					//5�� �� �翬�� �õ�
					new Thread(() -> {
						try {
							Thread.sleep(5000);
							logger.info("Reconnecting .......");
							client.reconnect();
						} catch (Exception e) {
							logger.error("Reconnect Fail : {}", e);
						}
					}).start();					
				}
				
                private void sendSubscription() {
                    String subscription = "["
                            + "{\"ticket\":\"" + UUID.randomUUID().toString() + "\"},"
                            + "{\"type\":\"ticker\",\"codes\":[\"KRW-BTC\",\"KRW-ETH\"]},"
                            + "{\"format\":\"SIMPLE\"}"
                            + "]";
                    System.out.println("Sending subscription: " + subscription);
                    send(subscription);
                }
			};
			
			// Web Socket ����
			client.connect();
		} catch (URISyntaxException e) {
			logger.error("Invalid URI : {}", e.getMessage());
			e.printStackTrace();
			
		} catch (Exception e) {
			logger.error("************** API INFO SETTING ERROR **************");
			logger.error("Error Content : {}", e);
		}
		
		logger.info("==================== API INFO SETTING END ====================");
		
	}
	
	// �ֱ������� ���� ���� Ȯ�� (30�ʸ���)
    @Scheduled(fixedRate = 30000)
    public void checkConnection() {
        if (client == null || client.isClosed()) {
            logger.info("WebSocket is disconnected. Reconnecting...");
            postConstruct();
        } else if (client.isOpen()) {
        	logger.info("WebSocket is connected...");
        }
    }
	
    // 0.8�ʸ��� ������ ó��
	@Scheduled(fixedRate = 800)
	public void processMarketData() {
		
		while(!dataQueue.isEmpty()) {
			String message = dataQueue.poll();
			try {				
				//JSON �Ľ� �� ������ ó��
				JsonNode node = objectMapper.readTree(message);
				String type = node.get("type").asText();
				if(!"trade".equals(type)) {
					continue;
				}
				String code = node.get("code").asText();
				double tradePrice = node.get("trade_price").asDouble();
				double tradeVolume = node.get("trade_volume").asDouble();
				long timestamp = node.get("timestamp").asLong();
				
				marketCandles.computeIfAbsent(code, k -> new ArrayList<>());
				marketCurrentMinute.computeIfAbsent(code, k -> -1L);
				marketLastPrice.computeIfAbsent(code, k -> 0.0);
				
				// 1�к� ����: Ÿ�ӽ������� 1�� ������ ����ȭ
                long minuteTimestamp = timestamp / 60000 * 60000;
                if (marketCurrentMinute.get(code) != minuteTimestamp) {
                    if (marketCurrentMinute.get(code) != -1 && marketLastPrice.get(code) != 0.0) {
                        // ���� 1�к� ����
                        marketCandles.get(code).add(new Candle(marketCurrentMinute.get(code), marketLastPrice.get(code)));
                        // RSI ��� (�ּ� 14�� ĵ�� �ʿ�)
                        if (marketCandles.get(code).size() >= 14) {
                            double rsi = CalcUtil.calculateRSI(marketCandles.get(code), 14);
                            System.out.printf("1-Minute RSI for %s: %.2f%n", code, rsi);
                        }
                        // ������ ĵ�� ���� (�޸� ����)
                        if (marketCandles.get(code).size() > 100) {
                            marketCandles.get(code).subList(0, marketCandles.get(code).size() - 100).clear();
                        }
                    }
                    marketCurrentMinute.put(code, minuteTimestamp);
                }
                marketLastPrice.put(code, tradePrice);			
				
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
	}
	
	@PreDestroy
	public void disconnect() {
		if(client != null && !client.isClosed()) {
			client.close();
			logger.info("WebSocket Client Closed.....");
		}
	}
}