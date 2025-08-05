package com.auto.daemon.util;

import java.util.List;

import com.auto.daemon.domain.Candle;

public class CalcUtil {
	
	// RSI 계산 메서드
    public static double calculateRSI(List<Candle> candles, int period) {
        if (candles.size() < period) {
            return 0.0; // 충분한 데이터가 없으면 0 반환
        }

        double totalGain = 0.0;
        double totalLoss = 0.0;
        int count = 0;

        // 최근 period 개의 캔들로 상승/하락폭 계산
        for (int i = candles.size() - period; i < candles.size() - 1; i++) {
            double priceDiff = candles.get(i + 1).closePrice - candles.get(i).closePrice;
            if (priceDiff > 0) {
                totalGain += priceDiff;
            } else if (priceDiff < 0) {
                totalLoss += Math.abs(priceDiff);
            }
            count++;
        }

        // 평균 상승폭과 하락폭
        double avgGain = count > 0 ? totalGain / period : 0.0;
        double avgLoss = count > 0 ? totalLoss / period : 0.0;

        // RS와 RSI 계산
        if (avgLoss == 0.0) {
            return avgGain == 0.0 ? 50.0 : 100.0; // 하락이 없으면 RSI=100, 둘 다 0이면 중립
        }
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }	
	
}
