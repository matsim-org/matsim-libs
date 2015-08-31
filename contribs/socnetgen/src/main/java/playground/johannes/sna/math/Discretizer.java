/* *********************************************************************** *
 * project: org.matsim.*
 * Descretizer.java
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
package playground.johannes.sna.math;

/**
 * Discretizes/categorizes values into bins/categories according to a predefined
 * rule.
 * 
 * @author illenberger
 * 
 */
public interface Discretizer {

	/**
	 * 
	 * @param value
	 *            a value (within the bounds defined by the implementing class).
	 * @return the bin/category for <tt>value</tt>.
	 */
	public double discretize(double value);

	/**
	 * 
	 * @param value
	 *            a value (within the bounds defined by the implementing class).
	 * @return the index of the bin/categery.
	 */
	public int index(double value);

	/**
	 * 
	 * @param value
	 *            a value (within the bounds defined by the implementing class).
	 * @return return the width of the bin for <tt>value</tt>.
	 */
	public double binWidth(double value);

}
