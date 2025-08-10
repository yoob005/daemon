package com.auto.daemon.wsk;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auto.daemon.domain.Candle;
import com.auto.daemon.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class UpbitWebSocketListener extends WebSocketListener{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());	
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, LinkedList<Candle>> marketCandleBuffer = new ConcurrentHashMap<>();
    private static final List<String> SELECTED_MARKETS = Arrays.asList("KRW-ETH", "KRW-BTC", "KRW-DOGE", "KRW-SOL", "KRW-XRP");
    private static final int RSI_PERIOD = 14;
    private static final int MAX_BUFFER_SIZE = RSI_PERIOD;
	
	
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
			logger.error("onMessge String Error : {}", e);
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
