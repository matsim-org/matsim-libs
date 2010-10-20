/* *********************************************************************** *
 * project: org.matsim.*
 * PowerDiscretizer.java
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

/**
 * @author illenberger
 *
 */
public class LogDiscretizer implements Discretizer {

	private double base;
	
	private double discretization;
	
	private double max = Double.POSITIVE_INFINITY;
	
	public LogDiscretizer(double base) {
		this(base, 1.0);
	}
	
	public LogDiscretizer(double base, double discretization) {
		this(base, discretization, Double.POSITIVE_INFINITY);
	}
	
	public LogDiscretizer(double base, double discretization, double max) {
		this.base = base;
		this.discretization = discretization;
		this.max = max;
	}
	
	@Override
	public double discretize(double value) {
		if(value == 0)
			System.err.println();
		value = Math.min(value, max);
		double bin = getBin(value, discretization);
		if(bin < 0)
			return 0;
		else {
			return Math.pow(base, bin) * discretization;
		}
	}

	public double getBin(double value, double discretization) {
		double bin = Math.ceil(Math.log(value/discretization)/Math.log(base));
//		return Math.max(bin, 0);
		return bin;
	}
}
