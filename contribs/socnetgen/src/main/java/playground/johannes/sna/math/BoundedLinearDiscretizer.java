/* *********************************************************************** *
 * project: org.matsim.*
 * BoundedLinearDiscretizer.java
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
package playground.johannes.sna.math;

/**
 * @author illenberger
 *
 */
public class BoundedLinearDiscretizer extends LinearDiscretizer {
	
	private final double lowerBound;
	
	private final double upperBound;

	public BoundedLinearDiscretizer(double binwidth, double lower, double upper) {
		super(binwidth);
		this.lowerBound = lower;
		this.upperBound = upper;
	}

	@Override
	public double discretize(double value) {
		value = truncate(value);
		return super.discretize(value);
	}

	@Override
	public double binWidth(double value) {
		value = truncate(value);
		return super.binWidth(value);
	}

	@Override
	public int index(double value) {
		value = truncate(value);
		return super.index(value);
	}

	private double truncate(double value) {
		value = Math.max(value, lowerBound);
		value = Math.min(value, upperBound);
		return value;
	}
}
