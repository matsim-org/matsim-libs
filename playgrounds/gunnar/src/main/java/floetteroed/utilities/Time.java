/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Time {

	// -------------------- CLASS VARIABLES --------------------

	private static final char DEFAULT_SEPARATOR = ':';

	private static final int SEC_PER_MIN = 60;

	private static final int MIN_PER_HOUR = 60;

	private static final int SEC_PER_HOUR = SEC_PER_MIN * MIN_PER_HOUR;

	// -------------------- PRIVATE CONSTRUCTOR --------------------

	private Time() {
	}

	// -------------------- STRING PARSING --------------------

	public static int secFromStr(final String timeStr, final char separator) {
		if (timeStr.indexOf(separator) < 0) {
			return Integer.parseInt(timeStr);
		} else {
			final String[] elements = timeStr.split("\\Q" + separator + "\\E");
			final int h = Integer.parseInt(elements[0]);
			final int m = Integer.parseInt(elements[1]);
			final int s = (elements.length > 2) ? Integer.parseInt(elements[2])
					: 0;
			return h * SEC_PER_HOUR + m * SEC_PER_MIN + s;
		}
	}

	public static int secFromStr(String timeStr) {
		return secFromStr(timeStr, DEFAULT_SEPARATOR);
	}

	// -------------------- STRING FORMATTING --------------------

	public static String strFromSec(int time_s, final char separator) {
		final int h = time_s / 3600;
		time_s -= h * 3600;
		final int m = time_s / 60;
		time_s -= m * 60;
		final int s = time_s;
		return (h < 10 ? "0" : "") + h + separator + (m < 10 ? "0" : "") + m
				+ separator + (s < 10 ? "0" : "") + s;
	}

	public static String strFromSec(final int time_s) {
		return strFromSec(time_s, DEFAULT_SEPARATOR);
	}
}
