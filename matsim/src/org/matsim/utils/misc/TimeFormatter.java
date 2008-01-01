/* *********************************************************************** *
 * project: org.matsim.*
 * TimeFormatter.java
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

package org.matsim.utils.misc;

import org.matsim.utils.StringUtils;

/**
 * A utility class to parse times from strings and to convert times to a textual
 * representation.
 *
 * @author mrieser
 */
public class TimeFormatter {

	// Never change this to NaN, as a compare of any valid time
	// to this should result to "greater" for some algorithms to work
	// still we found the name "UNDEFINED" more suitable than TIME_MIN_VALUE
	public final static double UNDEFINED_TIME = Double.NEGATIVE_INFINITY;

	public static final String TIMEFORMAT_HHMM = "HH:mm";
	public static final String TIMEFORMAT_HHMMSS = "HH:mm:ss";
	public static final String TIMEFORMAT_SSSS = "ssss";

	private String timeformat = TIMEFORMAT_HHMMSS;

	/**
	 * Creates a new TimeFormatter with the default time format "HH:mm:ss".
	 */
	public TimeFormatter() {
	}

	/**
	 * Creates a new TimeFormatter with the specified time format
	 *
	 * @param timeformat The time format to use.
	 */
	public TimeFormatter(final String timeformat) {
		this.timeformat = timeformat;
	}

	/**
	 * Parses the given string for a textual representation for time and returns
	 * the time value in seconds past midnight.
	 *
	 * @param time the string describing a time to parse.
	 *
	 * @return the parsed time as seconds after midnight.
	 *
	 * @throws IllegalArgumentException when the string cannot be interpreted as a valid time.
	 */
	public final double parseTime(final String time) {
		if (time == null || time.length() == 0 || time.equals("undefined")) {
			return UNDEFINED_TIME;
		}
		boolean isNegative = (time.charAt(0) == '-');
		String [] strings = StringUtils.explode(time, ':');
		int seconds = 0;
		if (strings.length == 1) {
			seconds = Math.abs(Integer.parseInt(strings[0]));
		} else if (strings.length == 2) {
			int h = Integer.parseInt(strings[0]);
			int m = Integer.parseInt(strings[1]);

			if ((m < 0) || (m > 59)) { throw new IllegalArgumentException("minutes are out of range in " + time); }

			seconds = Math.abs(h)*3600 + m*60;
		} else if (strings.length == 3) {
			int h = Integer.parseInt(strings[0]);
			int m = Integer.parseInt(strings[1]);
			int s = Integer.parseInt(strings[2]);

			if ((m < 0) || (m > 59)) { throw new IllegalArgumentException("minutes are out of range in " + time); }
			if ((s < 0) || (s > 59)) { throw new IllegalArgumentException("seconds are out of range in " + time); }

			seconds = Math.abs(h)*3600 + m*60 + s;
		} else {
			throw new IllegalArgumentException("time format is not valid in " + time);
		}

		if (isNegative) {
			seconds = -seconds;
		}
		return seconds;
	}

	/**
	 * Converts the given time in seconds after midnight into a textual representation
	 *
	 * @param seconds The time to convert, measured in seconds after midnight.
	 * @return A textual representation of the passed time.
	 */
	public final String writeTime(final double seconds) {
		if (this.timeformat.equals(TIMEFORMAT_SSSS)) {
			return Long.toString((long)seconds);
		} else if (seconds < 0) {
			if (seconds == UNDEFINED_TIME) return "undefined";
			return "-" + writeTime(Math.abs(seconds));
		}
		/* else */
		double s = seconds;
		long h = (long)(s / 3600);
		s = s % 3600;
		int m = (int)(s / 60);
		s = s % 60;

		String str_h;
		if (h < 10) { str_h = "0" + Long.toString(h); }
		else { str_h = Long.toString(h); }

		String str_m;
		if (m < 10) { str_m = "0" + Integer.toString(m); }
		else { str_m = Integer.toString(m); }

		if (this.timeformat.equals(TIMEFORMAT_HHMM)) {
			return str_h + ":" + str_m;
		} else if (this.timeformat.equals(TIMEFORMAT_HHMMSS)) {
			String str_s;
			if (s < 10) { str_s = "0" + Integer.toString((int)s); }
			else { str_s = Integer.toString((int)s); }
			return str_h + ":" + str_m + ":" + str_s;
		}
		/* else */
		throw new IllegalArgumentException("The time format (" + this.timeformat + ") is not known.");
	}

}
