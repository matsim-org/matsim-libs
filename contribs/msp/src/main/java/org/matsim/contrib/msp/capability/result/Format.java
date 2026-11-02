package org.matsim.contrib.msp.capability.result;

import org.matsim.api.core.v01.Coord;

import java.util.Locale;

/**
 * How the numbers of the three output levels are written: fixed decimals, a dot as the decimal
 * separator whatever the machine's locale is, and an empty cell rather than {@code NaN} or
 * {@code Infinity} for "there is no number here".
 * <p>
 * Times are seconds after midnight throughout, utilities are MATSim utils.
 */
final class Format {

	private Format() {
	}

	/**
	 * @return a time or duration [s], whole seconds
	 */
	static String time(double seconds) {
		return Double.isFinite(seconds) ? String.format(Locale.ROOT, "%.0f", seconds) : "";
	}

	/**
	 * @return a utility, four decimals - utilities are small numbers whose differences matter
	 */
	static String utils(double value) {
		return Double.isFinite(value) ? String.format(Locale.ROOT, "%.4f", value) : "";
	}

	static String x(Coord coord) {
		return coord == null ? "" : String.format(Locale.ROOT, "%.1f", coord.getX());
	}

	static String y(Coord coord) {
		return coord == null ? "" : String.format(Locale.ROOT, "%.1f", coord.getY());
	}
}
