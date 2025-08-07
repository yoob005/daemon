package com.auto.daemon.util;

import java.util.List;

import com.auto.daemon.domain.Candle;

public class CalcUtil {
	
//	// RSI ��� �޼���
//    public static double calculateRSI(List<Candle> candles, int period) {
//        if (candles.size() < period) {
//            return 0.0; // ����� �����Ͱ� ������ 0 ��ȯ
//        }
//
//        double totalGain = 0.0;
//        double totalLoss = 0.0;
//        int count = 0;
//
//        // �ֱ� period ���� ĵ��� ���/�϶��� ���
//        for (int i = candles.size() - period; i < candles.size() - 1; i++) {
//            double priceDiff = candles.get(i + 1).closePrice - candles.get(i).closePrice;
//            if (priceDiff > 0) {
//                totalGain += priceDiff;
//            } else if (priceDiff < 0) {
//                totalLoss += Math.abs(priceDiff);
//            }
//            count++;
//        }
//
//        // ��� ������� �϶���
//        double avgGain = count > 0 ? totalGain / period : 0.0;
//        double avgLoss = count > 0 ? totalLoss / period : 0.0;
//
//        // RS�� RSI ���
//        if (avgLoss == 0.0) {
//            return avgGain == 0.0 ? 50.0 : 100.0; // �϶��� ������ RSI=100, �� �� 0�̸� �߸�
//        }
//        double rs = avgGain / avgLoss;
//        return 100.0 - (100.0 / (1.0 + rs));
//    }	
	
}
