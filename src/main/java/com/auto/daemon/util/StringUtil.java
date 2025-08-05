package com.auto.daemon.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

public class StringUtil {

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat DATE_FORMAT_NO_DIV = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final SimpleDateFormat YMD_FORMAT = new SimpleDateFormat("yyyyMMdd");

	public static synchronized String getNow() {
		return DATE_FORMAT.format(Calendar.getInstance().getTime());
	}
	public static synchronized String getNow(Calendar cal) {
		return DATE_FORMAT.format(cal.getTime());
	}

	public static synchronized String getNowNoFmt() {
		return DATE_FORMAT_NO_DIV.format(Calendar.getInstance().getTime());	// yyyyMMddHHmmss (14)
	}
	public static synchronized String getNowNoFmt(Calendar cal) {
		return DATE_FORMAT_NO_DIV.format(cal.getTime());
	}

	public static synchronized String getDate(long time) {
		Calendar nCal = Calendar.getInstance();
		nCal.setTimeInMillis(time);
		return getNow(nCal);
	}
	public static synchronized String getDateNoFmt(long time) {
		Calendar nCal = Calendar.getInstance();
		nCal.setTimeInMillis(time);
		return getNowNoFmt(nCal);
	}

	public static synchronized String getYmdNoFmt(Calendar cal) {
		return getNowNoFmt(cal).substring(0, 8);
	}
	public static synchronized String getYmdNoFmt() {
		return getNowNoFmt().substring(0, 8);	// yyyyMMdd
	}
	public static synchronized String getMdNoFmt() {
		return getNowNoFmt().substring(4, 8);	// MMdd
	}
	public static synchronized String getTmNoFmt() {
		return getNowNoFmt().substring(8, 14);	// HHmmss
	}


	public static synchronized String addSecondsFromNow(int sec) {
		Calendar nCal = Calendar.getInstance();
		nCal.add(Calendar.SECOND, sec);
		return getNow(nCal);
	}
	public static synchronized String addSecondsNoFmtFromNow(int sec) {
		Calendar nCal = Calendar.getInstance();
		nCal.add(Calendar.SECOND, sec);
		return getNowNoFmt(nCal);
	}
	public static synchronized String addMinutesNoFmtFromNow(int min, boolean setZeroSec) {
		Calendar nCal = Calendar.getInstance();
		if(setZeroSec) {
			nCal.set(Calendar.SECOND, 0);
		}
		nCal.add(Calendar.MINUTE, min);
		return getNowNoFmt(nCal);
	}

	public static synchronized String addMinutesNoFmt(String date, int min) {
		Calendar nCal = convertDate(date);
		nCal.add(Calendar.MINUTE, min);
		return getNowNoFmt(nCal);
	}

	public static synchronized String addDaysNoFmtFromNow(int days) {
		Calendar nCal = Calendar.getInstance();
		nCal.add(Calendar.DATE, days);
		return getNowNoFmt(nCal);
	}



	public static synchronized int diffTime(long startTimeMsec) {
		return (int)(System.currentTimeMillis() - startTimeMsec);
	}

	public static synchronized Calendar convertDate(String date) {	// date: yyyyMMddHHmmss
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(DATE_FORMAT_NO_DIV.parse(date));
			return cal;
		} catch (ParseException e) {
			System.err.println("ParseException");
		}
		return null;
	}

	public static synchronized long diffMinutes(String startTime, String endTime) {
		long minute_millis = 60 * 1000;
		return (convertDate(endTime).getTimeInMillis() - convertDate(startTime).getTimeInMillis()) /minute_millis;
	}

	public static synchronized long diffMinutes(Calendar startCal, Calendar endCal) {
		long minute_millis = 60 * 1000;
		return (endCal.getTimeInMillis() - startCal.getTimeInMillis()) /minute_millis;
	}
	public static synchronized long diffSeconds(Calendar endCal) {
		long minute_millis = 1000;
		return (endCal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) /minute_millis;
	}


	/*
	public static synchronized int diffHours(Calendar startCal) {
		long minute_millis = 60 * 1000;
		long hour_millis = 60 * minute_millis;
		return (int) ( ( Calendar.getInstance().getTimeInMillis() - startCal.getTimeInMillis() ) /hour_millis );
	}
	*/




	public static String getNowNoFmt(String now) {
		return now.replaceAll("[-.: ]", "");	// yyyyMMddHHmmss
	}
	public static String getYmdNoFmt(String now) {
		return getNowNoFmt(now).substring(0, 8);	// yyyyMMdd
	}

	public static String fmtYmd(String ymd, String div) {
		if(ymd == null || ymd.length() !=8 ) {
			return ymd;
		}
		return ymd.substring(0,4) + div + ymd.substring(4,6) + div + ymd.substring(6,8);
	}

	public static String fmtYmd(String ymd) {
		return fmtYmd(ymd, "-");
	}


	public static String secToTime(int sec) {
		int MIN = 60;
		int HOUR = MIN * 60;
		int h = 0, m = 0, s = 0;

		/*
		if(sec > HOUR) {
			h = sec / HOUR;
			sec = sec % HOUR;
		}
		if(sec > MIN) {
			m = sec / MIN;
			sec = sec % MIN;
		}
		if(sec < MIN) {
			s = sec;
		}
		*/

		h = sec / HOUR;
		m = (sec % HOUR) / MIN;
		s = (sec % HOUR) % MIN;

		return String.format("%02d:%02d:%02d", h, m, s);
	}


	public static String fmtNumber(int num) {
		return String.format("%,d", num);
	}


	public static boolean isEmpty(Object obj) {
		if(obj == null) { return true; }
		if(obj.toString().trim().isEmpty()) { return true; }
		return false;
	}

	public static String ifnull(Object obj, String dft) {
		if(obj == null) { return dft; }
		return obj.toString();
	}
	public static String ifEmpty(Object obj, String dft) {
		if(obj == null) { return dft; }
		if(obj.toString().trim().isEmpty()) { return dft; }
		return obj.toString();
	}
	public static String ifnull(Object obj) {
		return ifnull(obj, null);
	}

	public static int ifnull(Object obj, int dft) {
		if(obj == null) { return dft; }
		try {
			return Integer.parseInt(obj.toString());
		} catch (NumberFormatException e) {}

		return dft;
	}
	public static long ifnull(Object obj, long dft) {
		if(obj == null) { return dft; }
		try {
			return Long.parseLong(obj.toString());
		} catch (NumberFormatException e) {}

		return dft;
	}

	public static String cleanXSS(String s) {
		if(s != null && s.length() > 0) {
			s = s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
			s = s.replaceAll("\"", "&quot;").replaceAll("'", "&#39;");
			s = s.replaceAll("'", "&apos;").replaceAll("&", "&amp;");
		}
		return s;
	}

	public static String restoreXSS(String s) {
		if(s != null && s.length() > 0 ) {
			s = s.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
			s = s.replaceAll("&quot;", "\"").replaceAll("&#39;", "'");
			s = s.replaceAll("&apos;", "'").replaceAll("&amp;", "&");
		}
		return s;
	}

	public static String leftZeroPadding(int maxdigit, int number) {
		return String.format(("%0" + maxdigit + "d"), number);
	}

	public static String getObjToString(Object obj) {
		return Objects.toString(obj, "");
	}

}