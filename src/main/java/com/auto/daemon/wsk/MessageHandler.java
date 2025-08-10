package com.auto.daemon.wsk;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auto.daemon.domain.Candle;
import com.auto.daemon.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okio.ByteString;

public class MessageHandler {
	
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, LinkedList<Candle>> marketCandleBuffer = new ConcurrentHashMap<>();
    private static final List<String> SELECTED_MARKETS = Arrays.asList("KRW-ETH", "KRW-BTC", "KRW-DOGE", "KRW-SOL", "KRW-XRP");
    private static final int RSI_PERIOD = 14;
    private static final int MAX_BUFFER_SIZE = RSI_PERIOD;
    
	public void strMsgHandler(String text){
		
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
	
	public void byteStrMsgHandler(ByteString bytes) {
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
	
}
