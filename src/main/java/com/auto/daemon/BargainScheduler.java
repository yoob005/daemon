package com.auto.daemon;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
import com.auto.daemon.config.enums.SiseType;
import com.auto.daemon.domain.Candle;
import com.auto.daemon.domain.UserVO;
import com.auto.daemon.domain.entity.UserInfoEntity;
import com.auto.daemon.service.UserInfoService;
import com.auto.daemon.util.CryptoUtil;
import com.auto.daemon.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

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
	
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, LinkedList<Candle>> marketCandleBuffer = new ConcurrentHashMap<>();
    private WebSocket webSocket;
    private SiseType siseType;

    private static final int RSI_PERIOD = 14;
    private static final int MAX_BUFFER_SIZE = RSI_PERIOD;
    private static final List<String> SELECTED_MARKETS = Arrays.asList("KRW-BTC", "KRW-ETH");
	
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
            
            // WebSocket 연결 설정
            Request request = new Request.Builder().url("wss://api.upbit.com/websocket/v1").build();
            webSocket = client.newWebSocket(request, new UpbitWebSocketListener());
            
            // 1분봉 데이터 요청
            String codes = SELECTED_MARKETS.stream()
                    //.map(market -> "\"" + market + ".1\"")
            		.map(market -> "\"" + market + "\"")
                    .collect(Collectors.joining(","));
            String message = String.format("[{\"ticket\":\"test\"},{\"type\":\"candle.1m\",\"codes\":[%s]},{\"format\":\"SIMPLE\"}]", codes);
            System.out.println(message);
            webSocket.send(message);
			
            // 초기 버퍼 설정
            SELECTED_MARKETS.forEach(market -> marketCandleBuffer.put(market, new LinkedList<>()));
            
		} catch (URISyntaxException e) {
			logger.error("Invalid URI : {}", e.getMessage());
			e.printStackTrace();
			
		} catch (Exception e) {
			logger.error("************** API INFO SETTING ERROR **************");
			logger.error("Error Content : {}", e);
		}
		
		logger.info("==================== API INFO SETTING END ====================");
		
	}
	
    private class UpbitWebSocketListener extends WebSocketListener {
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
            	
                Map<String, Object> data = mapper.readValue(text, Map.class);
                String market = (String) data.get("cd");
                if (!SELECTED_MARKETS.contains(market.replace(".1", ""))) return;
                Candle candle = mapper.convertValue(data, Candle.class);
                LinkedList<Candle> buffer = marketCandleBuffer.get(market.replace(".1", ""));
                synchronized (buffer) {
                    // 중복 캔들 방지
                    if (!buffer.isEmpty() && buffer.getLast().getCandleDateTimeUtc().equals(candle.getCandleDateTimeUtc())) {
                        return;
                    }
                    buffer.add(candle);
                    while (buffer.size() > MAX_BUFFER_SIZE) {
                        buffer.removeFirst();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {       	
            
            try {
            	
            	String jsonStr = JsonUtil.fromJson(bytes.string(StandardCharsets.UTF_8), JsonNode.class).toPrettyString();
                Map<String, Object> data = mapper.readValue(jsonStr, Map.class);
                String market = (String) data.get("cd");
                if (!SELECTED_MARKETS.contains(market.replace(".1", ""))) return;
                Candle candle = mapper.convertValue(data, Candle.class);
                LinkedList<Candle> buffer = marketCandleBuffer.get(market.replace(".1", ""));
                synchronized (buffer) {
                    // 중복 캔들 방지
                    if (!buffer.isEmpty() && buffer.getLast().getCandleDateTimeUtc().equals(candle.getCandleDateTimeUtc())) {
                        return;
                    }
                    buffer.add(candle);
                    while (buffer.size() > MAX_BUFFER_SIZE) {
                        buffer.removeFirst();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
    
    private double calculateRSI(LinkedList<Candle> candles) {
        double sumGain = 0, sumLoss = 0;
        for (int i = candles.size() - RSI_PERIOD + 1; i < candles.size(); i++) {
            double change = candles.get(i).getTradePrice() - candles.get(i - 1).getTradePrice();
            if (change > 0) sumGain += change;
            else sumLoss -= change;
        }
        double avgGain = sumGain / RSI_PERIOD;
        double avgLoss = sumLoss / RSI_PERIOD;
        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
    
    @Scheduled(fixedRate = 1000) // 1초마다 RSI 계산
    public void calculateAndPrintRSI() {
        for (String market : SELECTED_MARKETS) {
            LinkedList<Candle> buffer = marketCandleBuffer.get(market);
            synchronized (buffer) {
                if (buffer.size() >= RSI_PERIOD) {
                    double rsi = calculateRSI(buffer);
                    System.out.println("Market: " + market + " RSI: " + rsi);
                    System.out.println("------------------------");
                }
            }
        }
    }
}



public Double getRsiByMinutes() {
    final int minutes = 30;
    final String market = "KRW-BTC";
    final int maxCount = 200;
    // 업비트 캔들 API 호출 (Docs: https://docs.upbit.com/reference/%EB%B6%84minute-%EC%BA%94%EB%93%A4-1)
    List<MinuteCandleRes> candleResList = marketPriceReaderService.getCandleMinutes(minutes, market, maxCount);
    if (CollectionUtils.isEmpty(candleResList)) {
        return null;
    }
    
    // 지수 이동 평균은 과거 데이터부터 구해주어야 합니다.
    candleResList = candleResList.stream()
            .sorted(Comparator.comparing(CandleRes::getTimestamp))  // 오름차순 (과거 순)
            .collect(Collectors.toList());  // Sort

    double zero = 0;
    List<Double> upList = new ArrayList<>();  // 상승 리스트
    List<Double> downList = new ArrayList<>();  // 하락 리스트
    for (int i = 0; i < candleResList.size() - 1; i++) {
        // 최근 종가 - 전일 종가 = gap 값이 양수일 경우 상승했다는 뜻 / 음수일 경우 하락이라는 뜻
        double gapByTradePrice = candleResList.get(i + 1).getTradePrice().doubleValue() - candleResList.get(i).getTradePrice().doubleValue();
        if (gapByTradePrice > 0) {  // 종가가 전일 종가보다 상승일 경우
            upList.add(gapByTradePrice);
            downList.add(zero);
        } else if (gapByTradePrice < 0) {  // 종가가 전일 종가보다 하락일 경우
            downList.add(gapByTradePrice * -1);  // 음수를 양수로 변환해준다.
            upList.add(zero);
        } else {  // 상승, 하락이 없을 경우 종가 - 전일 종가 = gap은 0이므로 0값을 넣어줍니다.
            upList.add(zero);
            downList.add(zero);
        }
    }

    double day = 14;  // 가중치를 위한 기준 일자 (보통 14일 기준)
    double a = (double) 1 / (1 + (day - 1));  // 지수 이동 평균의 정식 공식은 a = 2 / 1 + day 이지만 업비트에서 사용하는 수식은 a = 1 / (1 + (day - 1))
    
    // AU값 구하기
    double upEma = 0;  // 상승 값의 지수이동평균
    if (CollectionUtils.isNotEmpty(upList)) {
        upEma = upList.get(0).doubleValue();
        if (upList.size() > 1) {
            for (int i = 1 ; i < upList.size(); i++) {
                upEma = (upList.get(i).doubleValue() * a) + (upEma * (1 - a));
            }
        }
    }

    // AD값 구하기
    double downEma = 0;  // 하락 값의 지수이동평균
    if (CollectionUtils.isNotEmpty(downList)) {
        downEma = downList.get(0).doubleValue();
        if (downList.size() > 1) {
            for (int i = 1; i < downList.size(); i++) {
                downEma = (downList.get(i).doubleValue() * a) + (downEma * (1 - a));
            }
        }
    }

    // rsi 계산
    double au = upEma;
    double ad = downEma;
    double rs = au / ad;
    double rsi = 100 - (100 / (1 + rs));

    return rsi;
}