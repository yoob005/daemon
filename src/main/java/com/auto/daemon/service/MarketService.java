package com.auto.daemon.service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auto.daemon.DaemonProperty;
import com.auto.daemon.domain.UserVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class MarketService {

	@Autowired
	private DaemonProperty daemonProp;
	
	@Autowired
	private UserVO user;
		
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ObjectMapper mapper = new ObjectMapper();
	
    /**
     * 특정 마켓의 시장가 전액 매수 API
     *
     * @param market 마켓 코드 (예: "KRW-BTC")
     * @return 주문 결과 JSON 문자열
     * @throws Exception API 호출 실패 시
     */
    public String marketBuyAll(String market, OkHttpClient client, boolean halfFlag) throws Exception {
        // KRW 잔고 조회
        double krwBalance = getKrwBalance(client);
        if (krwBalance <= 0) {
        	logger.error("보유 중인 원화가 0원입니다.");
            throw new Exception("No KRW balance available for buy");
        }else {
        	// 첫 매수시 보유금액의 절반만 매수
        	if(halfFlag){
        		krwBalance = Math.floor(krwBalance / 2);
        	}
        }

        // 시장가 매수 주문
        Map<String, Object> params = new HashMap<>();
        params.put("market", market);
        params.put("side", "bid");
        params.put("price", String.valueOf(krwBalance));
        params.put("ord_type", "price");
        logger.info("주문정보: {}", params.toString());
        
        return placeOrder(params, client);
    }
    
    /**
     * 특정 마켓의 시장가 전액 매도 API
     *
     * @param market 마켓 코드 (예: "KRW-BTC")
     * @return 주문 결과 JSON 문자열
     * @throws Exception API 호출 실패 시
     */
    public String marketSellAll(String market, OkHttpClient client) throws Exception {
        // 잔고 조회 (예: BTC 잔고)
        double coinBalance = getCoinBalance(client, market.split("-")[1]);
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
        logger.info("주문정보: {}", params.toString());
        
        return placeOrder(params, client);
    }
	
    private String placeOrder(Map<String, Object> params, OkHttpClient clients) throws Exception {
    	
        ArrayList<String> queryElements = new ArrayList<>();
        for(Map.Entry<String, Object> entity : params.entrySet()) {
            queryElements.add(entity.getKey() + "=" + entity.getValue());
        }

        String queryString = String.join("&", queryElements.toArray(new String[0]));

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(queryString.getBytes("UTF-8"));

        String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));

        Algorithm algorithm = Algorithm.HMAC256(user.getSecretKey());
        String jwtToken = JWT.create()
                .withClaim("access_key", user.getAccessKey())
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);

        String authenticationToken = "Bearer " + jwtToken;

            HttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(daemonProp.getApi().getUri() + "/orders");
            request.setHeader("Content-Type", "application/json");
            request.addHeader("Authorization", authenticationToken);
            request.setEntity(new StringEntity(new Gson().toJson(params)));

            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();

        return EntityUtils.toString(entity, "UTF-8");
    }
    
    private double getKrwBalance(OkHttpClient client) throws Exception {
        List<Map<String, Object>> accounts = getAccounts(client);
        for (Map<String, Object> account : accounts) {
            if ("KRW".equals(account.get("currency"))) {
                return Double.parseDouble((String) account.get("balance"));
            }
        }
        return 0;
    }
    
    private double getCoinBalance(OkHttpClient client, String currency) throws Exception {
        List<Map<String, Object>> accounts = getAccounts(client);
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
    public List<Map<String, Object>> getAccounts(OkHttpClient client) throws Exception {

    	Algorithm algorithm = Algorithm.HMAC256(user.getSecretKey());
    	String jwt = JWT.create()
    			.withClaim("access_key", user.getAccessKey())
    			.withClaim("nonce", UUID.randomUUID().toString())
    			.sign(algorithm); 
    	
        Request request = new Request.Builder()
                .url(daemonProp.getApi().getUri() + "/accounts")
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