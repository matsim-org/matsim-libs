/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.util;

public class Interpolation {
	public interface Interpolator {
		double interpolate(double time);
	}

	public static double interpolate(double[] vals, int timeInterval, double time) {
		int idx0 = (int)time / timeInterval;
		int idx1 = idx0 + 1;

		double weight1 = time % timeInterval;
		double weight0 = timeInterval - weight1;

		double weightedSum = weight0 * vals[idx0] + weight1 * vals[idx1];
		return weightedSum / timeInterval;
	}

	public static double interpolate(int[] vals, int timeInterval, double time) {
		int idx0 = (int)time / timeInterval;
		int idx1 = idx0 + 1;

		double weight1 = time % timeInterval;
		double weight0 = timeInterval - weight1;

		double weightedSum = weight0 * vals[idx0] + weight1 * vals[idx1];// int -> double
		return weightedSum / timeInterval;
	}

	public static Interpolator createInterpolator(final int[] values, final int timeInterval) {
		return new Interpolator() {
			public double interpolate(double time) {
				return Interpolation.interpolate(values, timeInterval, time);
			}
		};
	}

	public static Interpolator createInterpolator(final double[] values, final int timeInterval) {
		return new Interpolator() {
			public double interpolate(double time) {
				return Interpolation.interpolate(values, timeInterval, time);
			}
		};
	}
}
