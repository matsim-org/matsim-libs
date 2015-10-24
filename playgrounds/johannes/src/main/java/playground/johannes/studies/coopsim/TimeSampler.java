/* *********************************************************************** *
 * project: org.matsim.*
 * TimeSampler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.coopsim;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

import java.util.Arrays;
import java.util.Random;

/**
 * @author illenberger
 * 
 */
public class TimeSampler {

	private final Random random;

	private final int resolution = 60;
	
	private double[] y;

	public TimeSampler(UnivariateRealFunction pdf, int max, Random random) {
		this.random = random;
		max = max/resolution;
		try {
			y = new double[max];
			y[0] = 0;//pdf.value(0); FIXME
			int i = 1;
			for (int t = resolution; t < max*resolution; t += resolution) {
				y[i] = pdf.value(t) + y[i - 1];
				i++;
			}

			for (i = 0; i < max; i++) {
				y[i] = y[i] / y[max - 1];
			}
//			System.out.println("last index = " + y[max-1]);
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
	}

	public int nextSample() {
		double p = random.nextDouble();
		int idx = Arrays.binarySearch(y, p);
		if (idx < 0)
			idx = -idx - 1;
		return Math.max(resolution, idx*resolution);
		// while (true) { // for ever
		// int t = random.nextInt(max);
		//
		// try {
		// double p = pdf.value(t)/norm;
		//
		// if (random.nextDouble() < p)
		// return t;
		//
		// } catch (FunctionEvaluationException e) {
		// e.printStackTrace();
		// }

	}
}
