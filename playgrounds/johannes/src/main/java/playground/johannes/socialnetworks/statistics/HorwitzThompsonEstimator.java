/* *********************************************************************** *
 * project: org.matsim.*
 * HorwitzThompsonEstimator.java
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
package playground.johannes.socialnetworks.statistics;

import org.apache.commons.math.stat.descriptive.UnivariateStatistic;
import org.matsim.contrib.sna.math.UnivariatePiStatistic;

/**
 * @author illenberger
 *
 */
public class HorwitzThompsonEstimator implements UnivariatePiStatistic {

	private final double N;
	
	private double[] piValues;

	public HorwitzThompsonEstimator(double N) {
		this.N = N;
	}
	
	@Override
	public void setPiValues(double[] piValues) {
		this.piValues = piValues;
	}

	@Override
	public UnivariateStatistic copy() {
		HorwitzThompsonEstimator ht = new HorwitzThompsonEstimator(N);
		ht.setPiValues(piValues);
		return ht;
	}

	@Override
	public double evaluate(double[] values) {
		return evaluate(values, 0, values.length);
	}

	@Override
	public double evaluate(double[] values, int begin, int length) {
		double sum = 0;
		for(int i = begin; i < (begin + length); i++) {
			sum += values[i]/piValues[i];
		}
		
		return sum/N;
	}

}
