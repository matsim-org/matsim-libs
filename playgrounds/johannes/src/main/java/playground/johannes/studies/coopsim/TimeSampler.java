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

import java.util.Random;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

/**
 * @author illenberger
 * 
 */
public class TimeSampler {

	private final Random random;

	private final int interval;

	private final int min;

	private final UnivariateRealFunction pdf;

	private double norm;

	public TimeSampler(UnivariateRealFunction pdf, int min, int max, Random random) {
		this.pdf = pdf;
		this.random = random;
		this.min = min;
		this.interval = max - min;
		
		norm = 0;
		for(int t = min; t < max; t++) {
			try {
				norm += pdf.value(t);
			} catch (FunctionEvaluationException e) {
				e.printStackTrace();
			}
		}
		
		norm = 1.0/norm;
	}
	
	public int nextSample() {
		while (true) { // for ever
			int t = random.nextInt(interval);

			try {
				double p = pdf.value(t) * norm;

				if (random.nextDouble() < p)
					return min + t;
				
			} catch (FunctionEvaluationException e) {
				e.printStackTrace();
			}
		}
	}
}
