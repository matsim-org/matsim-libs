/* *********************************************************************** *
 * project: org.matsim.*
 * HTEstimator.java
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
package playground.johannes.socialnetworks.snowball2.sim;

/**
 * @author illenberger
 *
 */
public class HTEstimator implements PopulationEstimator {

	private final int N;
	
	public HTEstimator(int N) {
		this.N = N;
	}
	
	@Override
	public double mean(double[] values, double[] weights) {
		double sum = 0;
		for(int i = 0; i < values.length; i++) {
			sum += values[i] * weights[i];
		}
		return sum/(double)N;
	}

}
