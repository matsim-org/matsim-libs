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

package org.matsim.core.utils.misc;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;


public class Time {
	// yy there is now java.time, which integrates joda.time into the standard
	// jdk.  should we consider looking into this?  kai, dec'17

	
	private Time() {} // namespace only, do not instantiate

	/** 
	 * Never change this to NaN, as a compare of any valid time
	 * to this should result to "greater" for some algorithms to work
	 * still we found the name "UNDEFINED" more suitable than TIME_MIN_VALUE
	 * <br><b><i>Note:</i></b> do not interpret the "UNDEFINED" as "time does 
	 * not matter", as this has implications for, example, routing. If start 
	 * time is given as {@link Time#UNDEFINED_TIME} then {@link Path#travelTime}
	 * will return {@link Double#NaN}, even though the {@link TravelTime#getLinkTravelTime}
	 * is independent of the start time. */
	// of the convention.  kai, nov'17
	final static double UNDEFINED_TIME = Double.NEGATIVE_INFINITY;
	/**
	 * The end of a day in MATSim in seconds
	 */
	public final static double MIDNIGHT = 24 * 3600.0;

	public static final String TIMEFORMAT_HHMM = "HH:mm";
	public static final String TIMEFORMAT_HHMMSS = "HH:mm:ss";
	public static final String TIMEFORMAT_SSSS = "ssss";
	
	public static final String TIMEFORMAT_HHMMSSDOTSS = "HH:mm:ss.ss" ;

	private static String defaultTimeFormat = TIMEFORMAT_HHMMSS;

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

	public static final String writeTime(final OptionalTime time) {
		return writeTime(time.orElse(UNDEFINED_TIME));
	}

	/**
	 * Converts the given time in seconds after midnight into a textual representation
	 *
	 * @param seconds The time to convert, measured in seconds after midnight.
	 * @param timeformat
	 * @param separator
	 * @return A textual representation of the passed time.
	 */
	public static final String writeTime(final double seconds, final String timeformat, final char separator) {
		if (TIMEFORMAT_SSSS.equals(timeformat)) {
			return Long.toString((long)seconds);
		}
		if (seconds == UNDEFINED_TIME) {
			return "undefined";
		}
		if (seconds < 0) {
			return "-" + writeTime(Math.abs(seconds), timeformat, separator);
		}
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

		if (TIMEFORMAT_HHMM.equals(timeformat)) {
			return str.toString();
		}
		if (TIMEFORMAT_HHMMSS.equals(timeformat)) {
			str.append(separator);
			str.append(timeElements[(int)s]);
			return str.toString();
		}
		if ( TIMEFORMAT_HHMMSSDOTSS.equals(timeformat)) {
			str.append(separator);

			if ( s < 10. ) {
				str.append("0") ;
			}
			str.append(s);
			return str.toString();
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
		return parseTime(time, ':').seconds();
	}

	public static final OptionalTime parseOptionalTime(final String time) {
		return parseTime(time, ':');
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
	public static final OptionalTime parseTime(final String time, final char separator) {
		if (time == null || time.length() == 0 || time.equals("undefined")) {
			return OptionalTime.undefined();
		}
		boolean isNegative = (time.charAt(0) == '-');
		String[] strings = (isNegative
				? StringUtils.explode(time.substring(1), separator)
						: StringUtils.explode(time, separator));
		double seconds = 0;
		if (strings.length == 1) {
			seconds = Math.abs(Double.parseDouble(strings[0]));
		} else if (strings.length == 2) {
			long h = Long.parseLong(strings[0]);
			int m = Integer.parseInt(strings[1]);

			if ((m < 0) || (m > 59)) {
				throw new IllegalArgumentException("minutes are out of range in " + time);
			}

			seconds = Math.abs(h) * 3600 + m * 60;
		} else if (strings.length == 3) {
			long h = Long.parseLong(strings[0]);
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
		return seconds == Time.UNDEFINED_TIME ? OptionalTime.undefined() : OptionalTime.defined(seconds);
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
