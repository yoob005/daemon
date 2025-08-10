package com.auto.daemon.wsk;

import com.auto.daemon.domain.Candles;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class UpbitCandleFetcher {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_CANDLE_COUNT = 200; // Upbit 최대 캔들 수
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
    public List<Candles> fetchOneMinuteCandles(String market, Instant to) throws Exception {
        int retryCount = 0;
        String url = "https://api.upbit.com/v1/candles/minutes/1?market=" + market + "&count=" + MAX_CANDLE_COUNT;
        if (to != null) {
            url += "&to=" + URLEncoder.encode(to.toString(), "UTF-8");
        }

        while (retryCount < MAX_RETRIES) {
            try {
                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.err.println("REST API failed for " + market + ": " + response.code() + ", " + response.message());
                        retryCount++;
                        Thread.sleep(RETRY_DELAY_MS);
                        continue;
                    }
                    String json = response.body().string();
//                    System.out.println("REST API response for " + market + (to != null ? " at " + to : "") + ": " + json.substring(0, Math.min(json.length(), 200)) + "...");
                    List<Candles> candle = mapper.readValue(json, new TypeReference<List<Candles>>() {});
//                    System.out.println("Fetched " + candle.size() + " candles for " + market);
                    return candle;
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch candles for " + market + (to != null ? " at " + to : "") + ": " + e.getMessage());
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