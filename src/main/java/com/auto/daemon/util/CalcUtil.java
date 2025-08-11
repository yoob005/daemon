package com.auto.daemon.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.auto.daemon.domain.Candle;

public class CalcUtil {
	
	public static double calculateRSI(LinkedList<Candle> candles) {
        double sumGain = 0, sumLoss = 0;
        for (int i = candles.size() - 14 + 1; i < candles.size(); i++) {
            double change = candles.get(i).getTradePrice() - candles.get(i - 1).getTradePrice();
            if (change > 0) sumGain += change;
            else sumLoss -= change;
        }
        double avgGain = sumGain / 14;
        double avgLoss = sumLoss / 14;
        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

	public static Map<String, Double> calculateBollingerBands(List<Candle> candles) {
        // 입력 데이터 검증: 최소 20개 필요
        if (candles.size() < 20) {
            throw new IllegalArgumentException("At least 20 candles are required to calculate Bollinger Bands. Provided: " + candles.size());
        }

        // 최근 20개 종가 추출 (200개 중 마지막 20개)
        List<Double> closes = candles.subList(candles.size() - 20, candles.size()).stream()
                .map(Candle::getTradePrice)
                .collect(Collectors.toList());

        // SMA 계산
        double sum = closes.stream().mapToDouble(Double::doubleValue).sum();
        double sma = sum / 20;

        // 표준편차 계산
        double sumSq = closes.stream().mapToDouble(p -> Math.pow(p - sma, 2)).sum();
        double std = Math.sqrt(sumSq / 20);

        // 결과 맵 생성
        Map<String, Double> result = new HashMap<>();
        result.put("sma", sma);
        result.put("upperBand", sma + 2 * std);
        result.put("lowerBand", sma - 2 * std);
        return result;
    }
	
	public static double calcOneMinRSI(List<Candle> candleResList) {
		
        candleResList = candleResList.stream()
                .sorted(Comparator.comparing(Candle::getTimestamp))  // 오름차순 (과거 순)
                .collect(Collectors.toList());  // Sort
        
        double zero = 0;
        List<Double> upList = new ArrayList<>();  // 상승 리스트
        List<Double> downList = new ArrayList<>();  // 하락 리스트
        for (int i = 0; i < candleResList.size() - 1; i++) {
            // 최근 종가 - 전일 종가 = gap 값이 양수일 경우 상승했다는 뜻 / 음수일 경우 하락이라는 뜻
            double gapByTradePrice = candleResList.get(i + 1).getTradePrice() - candleResList.get(i).getTradePrice();
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
        if (!upList.isEmpty()) {
            upEma = upList.get(0).doubleValue();
            if (upList.size() > 1) {
                for (int i = 1 ; i < upList.size(); i++) {
                    upEma = (upList.get(i).doubleValue() * a) + (upEma * (1 - a));
                }
            }
        }

        // AD값 구하기
        double downEma = 0;  // 하락 값의 지수이동평균
        if (!downList.isEmpty()) {
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
	
}
