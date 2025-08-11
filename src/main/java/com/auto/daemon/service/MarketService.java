package com.auto.daemon.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class MarketService {

	@Value("${daemon.api.url}")
	private static String URL; 
		
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ObjectMapper mapper = new ObjectMapper();
	
    /**
     * 특정 마켓의 시장가 전액 매수 API
     *
     * @param market 마켓 코드 (예: "KRW-BTC")
     * @return 주문 결과 JSON 문자열
     * @throws Exception API 호출 실패 시
     */
    public String marketBuyAll(String market, String jwt, OkHttpClient client) throws Exception {
        // KRW 잔고 조회
        double krwBalance = getKrwBalance(client, jwt);
        if (krwBalance <= 0) {
        	logger.error("보유 중인 원화가 0원입니다.");
            throw new Exception("No KRW balance available for buy");
        }

        // 시장가 매수 주문
        Map<String, Object> params = new HashMap<>();
        params.put("market", market);
        params.put("side", "bid");
        params.put("price", String.valueOf(krwBalance)); // 전액 매수
        params.put("ord_type", "price");

        return placeOrder(params, jwt, client);
    }
    
    /**
     * 특정 마켓의 시장가 전액 매도 API
     *
     * @param market 마켓 코드 (예: "KRW-BTC")
     * @return 주문 결과 JSON 문자열
     * @throws Exception API 호출 실패 시
     */
    public String marketSellAll(String market, String jwt, OkHttpClient client) throws Exception {
        // 잔고 조회 (예: BTC 잔고)
        double coinBalance = getCoinBalance(client, jwt, market.split("-")[1]);
        if (coinBalance <= 0) {
        	logger.error("보유 중인 {} 잔고가 없습니다.", market);
            throw new Exception("No coin balance available for sell");
        }

        // 시장가 매도 주문
        Map<String, Object> params = new HashMap<>();
        params.put("market", market);
        params.put("side", "ask");
        params.put("volume", String.valueOf(coinBalance)); // 전액 매도
        params.put("ord_type", "market");

        return placeOrder(params, jwt, client);
    }
	
    private String placeOrder(Map<String, Object> params, String jwt, OkHttpClient client) throws Exception {
        String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(params),
                okhttp3.MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(URL + "/orders")
                .addHeader("Authorization", "Bearer " + jwt)
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
    
    private double getKrwBalance(OkHttpClient client, String jwt) throws Exception {
        List<Map<String, Object>> accounts = getAccounts(client, jwt);
        for (Map<String, Object> account : accounts) {
            if ("KRW".equals(account.get("currency"))) {
                return Double.parseDouble((String) account.get("balance"));
            }
        }
        return 0;
    }
    
    private double getCoinBalance(OkHttpClient client, String jwt, String currency) throws Exception {
        List<Map<String, Object>> accounts = getAccounts(client, jwt);
        for (Map<String, Object> account : accounts) {
            if (currency.equals(account.get("currency"))) {
                return Double.parseDouble((String) account.get("balance"));
            }
        }
        return 0;
    }
    
    /**
     * 내 지갑 조회
     *
     * @param market 마켓 코드 (예: "KRW-BTC")
     * @return 주문 결과 JSON 문자열
     * @throws Exception API 호출 실패 시
     */
    private List<Map<String, Object>> getAccounts(OkHttpClient client, String jwt) throws Exception {

        Request request = new Request.Builder()
                .url(URL + "/accounts")
                .addHeader("Authorization", "Bearer " + jwt)
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