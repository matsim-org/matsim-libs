/* *********************************************************************** *
 * project: org.matsim.*
 * Histogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.math;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleFunction;

import java.util.Arrays;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * @author illenberger
 *
 */
public class Histogram {

	public static TDoubleDoubleHashMap createHistogram(DescriptiveStatistics stats, Discretizer discretizer) {
		if(stats instanceof DescriptivePiStatistics)
			return createHistogram((DescriptivePiStatistics)stats, discretizer);
		else
			return createHistogram(stats.getValues(), discretizer);
	}
	
	public static TDoubleDoubleHashMap createHistogram(DescriptivePiStatistics stats, Discretizer discretizer) {
		double[] piValues = stats.getPiValues();
		double[] weights = new double[piValues.length];
		for(int i = 0; i < piValues.length; i++) {
			weights[i] = 1/piValues[i];
		}
		
		return createHistogram(stats.getValues(), weights, discretizer);
	}
	
	public static TDoubleDoubleHashMap createHistogram(double[] values, Discretizer discretizer) {
		double[] weights = new double[values.length];
		Arrays.fill(weights, 1.0);
		return createHistogram(values, weights, discretizer);
	}
	
	public static TDoubleDoubleHashMap createHistogram(double[] values, double[] weights, Discretizer discretizer) {
		TDoubleDoubleHashMap histogram = new TDoubleDoubleHashMap();
		for(int i = 0; i < values.length; i++) {
			double bin = discretizer.discretize(values[i]);
			double weight = weights[i]/discretizer.binWidth(values[i]);
			histogram.adjustOrPutValue(bin, weight, weight);
		}
		return histogram;
	}
	
	public static TDoubleDoubleHashMap normalize(TDoubleDoubleHashMap histogram) {
		double sum = 0;
		double[] values = histogram.getValues();
		
		for(int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		
		final double norm = 1/sum;
		
		TDoubleFunction fct = new TDoubleFunction() {
			public double execute(double value) {
				return value * norm;
			}
		
		};
		
		histogram.transformValues(fct);
		
		return histogram;
	}
}
