/* *********************************************************************** *
 * project: org.matsim.*
 * LinearDiscretizer.java
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


/**
 * A discretizer with bin of equal width.
 * 
 * @author illenberger
 *
 */
public class LinearDiscretizer implements Discretizer {

	private final double binwidth;
	
	/**
	 * Creates a new discretizer.
	 * 
	 * @param binwidth the bin width.
	 */
	public LinearDiscretizer(double binwidth) {
		this.binwidth = binwidth;
	}
	
	/**
	 * @param value a value.
	 * @return rounds <tt>value</tt> up to the upper bin border.
	 */
	@Override
	public double discretize(double value) {
		return index(value) * binwidth;
	}

	/**
	 * @param value a value.
	 * @return the bin width.
	 */
	@Override
	public double binWidth(double value) {
		return binwidth;
	}

	/**
	 * @see {@link Discretizer#index(double)}
	 */
	@Override
	public double index(double value) {
		return Math.ceil(value/binwidth);
	}

}
