/* *********************************************************************** *
 * project: org.matsim.*
 * Time.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.sergioo.weeklySimulation.util.misc;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.StringUtils;


public class Time {
	public enum Week {
		MONDAY(0, "mon"),
		TUESDAY(1, "tue"),
		WEDNESDAY(2, "wed"),
		THURSDAY(3, "thu"),
		FRIDAY(4, "fri"),
		SATURDAY(5, "sat"),
		SUNDAY(6, "sun");
		private int position;
		private String shortName;
		private Week(int pos, String shortName) {
			this.position = pos;
			this.shortName = shortName;
		}
		private static double getDayPos(String string) {
			for(Week weekDay:Week.values())
				if(string.equals(weekDay.shortName))
					return weekDay.position;
			return Time.UNDEFINED_TIME;
		}
		private static String getDayName(int pos) {
			return Week.values()[pos].shortName;
		}
		public String getShortName() {
			return shortName;
		}
	}
	
	public enum Period {
		
		EARLY_MORNING(0, 7*3600),
		MORNING_PEAK(7*3600, 10*3600),
		BEFORE_LUNCH(10*3600, 13*3600),
		AFTER_LUNCH(13*3600, 18*3600),
		EVENING_PEAK(18*3600, 21*3600),
		NIGHT(21*3600, 24*3600);
		
		//Constants
		private static final double PERIODS_TIME = 24*3600;
		
		//Attributes
		private final double startTime;
		private final double endTime;
	
		//Constructors
		private Period(double startTime, double endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}
		public static Period getPeriod(double time) {
			time%=24*3600;
			for(Period period:Period.values())
				if(period.isPeriod(time))
					return period;
			return null;
		}
		protected boolean isPeriod(double time) {
			time = time%PERIODS_TIME;
			if(startTime<=time && time<endTime)
				return true;
			return false;
		}
		public double getStartTime() {
			return startTime;
		}
		public double getEndTime() {
			return endTime;
		}
		public double getMiddleTime() {
			return (startTime+endTime)/2;
		}
	}

	private Time() {} // namespace only, do not instantiate

	/** 
	 * Never change this to NaN, as a compare of any valid time
	 * to this should result to "greater" for some algorithms to work
	 * still we found the name "UNDEFINED" more suitable than TIME_MIN_VALUE
	 * <br><b><i>Note:</i></b> do not interpret the "UNDEFINED" as "time does 
	 * not matter", as this has implications for, example, routing. If start 
	 * time is given as {@link Time#UNDEFINED_TIME} then {@link Path#travelTime}
	 * will return {@link Double#NaN}, even though the {@link TravelTime#getLinkTravelTime()}
	 * is independent of the start time. */
	public final static double UNDEFINED_TIME = Double.NEGATIVE_INFINITY;
	/**
	 * The end of a day in MATSim in seconds
	 */
	public final static double MIDNIGHT = 24 * 3600.0;

	public static final String TIMEFORMAT_EEEHHMM = "EEE,HH:mm";
	public static final String TIMEFORMAT_EEEHHMMSS = "EEE,HH:mm:ss";
	public static final String TIMEFORMAT_EEESSSS = "EEE,ssss";
	
	public static final String TIMEFORMAT_HHMMSSDOTSS = "HH:mm:ss.ss" ;

	private static String defaultTimeFormat = TIMEFORMAT_EEEHHMMSS;

	private final static String[] timeElements;

	static {
		timeElements = new String[60];
		for (int i = 0; i < 10; i++) {
			timeElements[i] = "0" + i;
		}
		for (int i = 10; i < 60; i++) {
			timeElements[i] = Integer.toString(i);
		}
	}

	/**
	 * Sets the default time format to be used for conversion of seconds to a string-representation
	 * ({@link #writeTime(double)}). If nothing is set, {@link #TIMEFORMAT_HHMMSS} is used as default.
	 *
	 * @param format
	 */
	public static final void setDefaultTimeFormat(final String format) {
		defaultTimeFormat = format;
	}

	public static final String writeTime(final double seconds, final String timeformat) {
		return writeTime(seconds, timeformat, ':');
	}

	public static final String writeTime(final double seconds, final char separator) {
		return writeTime(seconds, defaultTimeFormat, separator);
	}

	public static final String writeTime(final double seconds) {
		return writeTime(seconds, defaultTimeFormat, ':');
	}

	/**
	 * Converts the given time in seconds after midnight into a textual representation
	 *
	 * @param seconds The time to convert, measured in seconds after midnight.
	 * @param timeformat
	 * @param separator
	 * @return A textual representation of the passed time.
	 */
	public static final String writeTime(double seconds, final String timeformat, final char separator) {
		if (seconds < 0) {
			if (seconds == UNDEFINED_TIME)
				return "undefined";
			return "-" + writeTime(Math.abs(seconds), timeformat, separator);
		}
		int day = (int) (seconds/MIDNIGHT);
		seconds %= MIDNIGHT;
		if (TIMEFORMAT_EEESSSS.equals(timeformat))
			return Week.getDayName(day)+","+Long.toString((long)(seconds));
		double s = seconds;
		long h = (long)(s / 3600);
		s = s % 3600;
		int m = (int)(s / 60);
		s = s % 60;

		StringBuilder str = new StringBuilder(10);

		if (h < timeElements.length) {
			str.append(timeElements[(int) h]);
		} else {
			str.append(Long.toString(h));
		}

		str.append(separator);
		str.append(timeElements[m]);

		if (TIMEFORMAT_EEEHHMM.equals(timeformat)) {
			return Week.getDayName(day)+","+str.toString();
		}
		if (TIMEFORMAT_EEEHHMMSS.equals(timeformat)) {
			str.append(separator);
			str.append(timeElements[(int)s]);
			return Week.getDayName(day)+","+str.toString();
		}
		if ( TIMEFORMAT_HHMMSSDOTSS.equals(timeformat)) {
			str.append(separator);

			if ( s < 10. ) {
				str.append("0") ;
			}
			str.append(s);
			return Week.getDayName(day)+","+str.toString();
		}

		throw new IllegalArgumentException("The time format (" + timeformat + ") is not known.");
	}

	/**
	 * Parses the given string for a textual representation for time and returns
	 * the time value in seconds past midnight. It is the same as {@link #parseTime(String, char)}
	 * with the separator set to ':'.
	 *
	 * @param time the string describing a time to parse.
	 *
	 * @return the parsed time as seconds after midnight.
	 *
	 * @throws IllegalArgumentException when the string cannot be interpreted as a valid time.
	 */
	public static final double parseTime(final String time) {
		if(time == null)
			return UNDEFINED_TIME;
		String[] dayTime = time.split(",");
		if(dayTime.length==2)
			return parseDay(dayTime[0])*MIDNIGHT+parseTime(dayTime[1], ':');
		else
			return parseTime(dayTime[0], ':');
	}

	private static double parseDay(String string) {
		return Week.getDayPos(string);
	}

	/**
	 * Parses the given string for a textual representation for time and returns
	 * the time value in seconds past midnight. The following formats are recognized:
	 * HH:mm:ss, HH:mm, ssss.
	 *
	 * @param time the string describing a time to parse.
	 * @param separator the character used between hours and minutes, and minutes and seconds.
	 *
	 * @return the parsed time as seconds after midnight.
	 *
	 * @throws IllegalArgumentException when the string cannot be interpreted as a valid time.
	 */
	public static final double parseTime(final String time, final char separator) {
		if (time == null || time.length() == 0 || time.equals("undefined")) {
			return Time.UNDEFINED_TIME;
		}
		boolean isNegative = (time.charAt(0) == '-');
		String[] strings = (isNegative
				? StringUtils.explode(time.substring(1), separator)
						: StringUtils.explode(time, separator));
		double seconds = 0;
		if (strings.length == 1) {
			seconds = Math.abs(Double.parseDouble(strings[0]));
		} else if (strings.length == 2) {
			int h = Integer.parseInt(strings[0]);
			int m = Integer.parseInt(strings[1]);

			if ((m < 0) || (m > 59)) {
				throw new IllegalArgumentException("minutes are out of range in " + time);
			}

			seconds = Math.abs(h) * 3600 + m * 60;
		} else if (strings.length == 3) {
			int h = Integer.parseInt(strings[0]);
			int m = Integer.parseInt(strings[1]);
			double s = Double.parseDouble(strings[2]);

			if ((m < 0) || (m > 59)) {
				throw new IllegalArgumentException("minutes are out of range in " + time);
			}
			if ((s < 0) || (s >= 60)) {
				throw new IllegalArgumentException("seconds are out of range in " + time);
			}

			seconds = Math.abs(h) * 3600 + m * 60 + s;
		} else {
			throw new IllegalArgumentException("time format is not valid in " + time);
		}

		if (isNegative) {
			seconds = -seconds;
		}
		return seconds;
	}

	/**
	 * Converts a number like 1634 to the time value of 16:34:00.
	 * 
	 * @param hhmm the time-representing number to convert.
	 * @return the time as seconds after midnight.
	 */
	public static double convertHHMMInteger(int hhmm) {
		int h = hhmm / 100;
		int m = hhmm - (h * 100);
		double seconds = Math.abs(h) * 3600 + m * 60;
		return seconds;
	}

}
