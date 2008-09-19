/* *********************************************************************** *
 * project: org.matsim.*
 * PowerLawFit.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.statistics;

import java.util.Arrays;

import gnu.trove.TDoubleDoubleHashMap;

import org.apache.log4j.Logger;
import org.matsim.utils.collections.Tuple;

/**
 * @author illenberger
 *
 */
public class PowerLawFit {

	private static final Logger logger = Logger.getLogger(PowerLawFit.class);
	
	public static Tuple<Double, Double> fit(double[] values) {
		double[] weights = new double[values.length];
		Arrays.fill(weights, 1.0);
		return fit(values, weights);
	}
	
	public static Tuple<Double, Double> fit(double[] values, double[] weights) {
		WeightedStatistics stats = new WeightedStatistics();
		stats.addAll(values, weights);
		TDoubleDoubleHashMap dist = stats.absoluteDistribution();
		double[] keys = dist.keys();
		Arrays.sort(keys);
		double xmin = 0;
		double maxFreq = 0;
		for(double key : keys) {
			if(dist.get(key) > maxFreq) {
				maxFreq = dist.get(key); 
				xmin = key;
			}
		}
		
		if(xmin <= 0) {
			logger.warn(String.format("xmin adjusted to Double.MIN_VALUE, was %1$s", xmin));
			xmin = Double.MIN_VALUE;
		}
		double gamma = fit(values, weights, xmin);
		
		return new Tuple<Double, Double>(xmin, gamma);
	}
	
	public static double fit(double[] values, double xmin) {
		double[] weights = new double[values.length];
		Arrays.fill(weights, 1.0);
		return fit(values, weights, xmin);
	}
	
	public static double fit(double[] values, double[] weights, double xmin) {
		double logsum = 0;
		double wsum = 0;
		for(int i = 0; i < values.length; i++) {
			if(values[i] >= xmin) {
				logsum += Math.log((values[i]/xmin)) * weights[i];
				wsum += weights[i];
			}
		}
		return 1 + (wsum/logsum);
	}
}
