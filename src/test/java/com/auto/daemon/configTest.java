package com.auto.daemon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;

import com.auto.daemon.domain.entity.PriceData;

public class configTest {
	
	@Value("${daemon.api.marketList}")
	private List<String> marketArr;
	
	@Value("${daemon.api.key}")
	private String apikey;
	
	public void main(String[] args) {
		
		List<PriceData> priceDataList = new ArrayList<PriceData>();
		PriceData pr = new PriceData();
		pr.setId(0L);
		
		// 종가 배열 생성 (오름차순으로 정렬)
        double[] closes = priceDataList.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp())) // 타임스탬프 오름차순
                .mapToDouble(PriceData::getClosePrice)
                .toArray();

        // 상승폭과 하락폭 계산
        double[] gains = new double[14];
        double[] losses = new double[14];

        for (int i = 1; i < 14; i++) {
            double change = closes[i] - closes[i - 1];
            gains[i] = change > 0 ? change : 0;
            losses[i] = change < 0 ? Math.abs(change) : 0;
        }

        // 평균 상승폭과 하락폭 계산 (단순이동평균)
        double avgGain = IntStream.range(1, 14)
                .mapToDouble(i -> gains[i])
                .average()
                .orElse(0.0);
        double avgLoss = IntStream.range(1, 14)
                .mapToDouble(i -> losses[i])
                .average()
                .orElse(0.0);

        // RS 계산
        double rs = avgLoss == 0 ? Double.POSITIVE_INFINITY : avgGain / avgLoss;

        // RSI 계산
        System.out.println(100 - (100 / (1 + rs)));
	}
	
}


