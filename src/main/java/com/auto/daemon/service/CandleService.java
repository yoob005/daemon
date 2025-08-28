package com.auto.daemon.service;

import com.auto.daemon.DaemonProperty;
import com.auto.daemon.domain.Candle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
public class CandleService {
	
	@Autowired
	private DaemonProperty daemonProp;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    /**
     * Upbit REST API로 1분봉 캔들 최대 200개를 조회합니다.
     *
     * @param market 조회할 마켓 (예: "KRW-BTC")
     * @param to     조회 기준 시점 (UTC, ISO 8601 형식, null이면 최신 데이터)
     * @return List<Candle> 최대 200개의 캔들 데이터
     * @throws Exception API 호출 실패 시
     */
    public List<Candle> fetchMinuteCandles(String market, Instant to, OkHttpClient client, String minute) throws Exception {
        int retryCount = 0;
        String url = daemonProp.getApi().getUri() + "/candles/minutes/" + minute + "?market=" + market + "&count=" + daemonProp.getSetting().getMaxCandle();
        if (to != null) {
            url += "&to=" + URLEncoder.encode(to.toString(), "UTF-8");
        }
        while (retryCount < MAX_RETRIES) {
            try {
                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                    	logger.error("REST API failed for " + market + ": " + response.code() + ", " + response.message());
                        retryCount++;
                        Thread.sleep(RETRY_DELAY_MS);
                        continue;
                    }
                    String json = response.body().string();
                    List<Candle> candle = mapper.readValue(json, new TypeReference<List<Candle>>() {});
                    return candle;
                }
            } catch (Exception e) {
            	logger.error("Failed to fetch candles for " + market + (to != null ? " at " + to : "") + ": " + e.getMessage());
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    throw new Exception("Failed to fetch candles after " + MAX_RETRIES + " retries: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        return Collections.emptyList();
    }
}