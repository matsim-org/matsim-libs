/* *********************************************************************** *
 * project: org.matsim.*
 * DummyDiscretizer.java
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
package org.matsim.contrib.common.stats;

/**
 * A dummy discretizer that returns just the input value.
 * 
 * @author jillenberger
 * 
 */
public class DummyDiscretizer implements Discretizer {

	private static DummyDiscretizer instance;

	/**
	 * Singleton access.
	 *
	 * @return an instance of <tt>DummyDiscretizer</tt> creating a new one if necessary
	 */
	public static DummyDiscretizer getInstance() {
		if(instance == null)
			instance = new DummyDiscretizer();
		
		return instance;
	}
	
	/**
	 * This method has no meaning here.
	 *
	 * @throws {@link UnsupportedOperationException}
	 */
	@Override
	public double binWidth(double value) {
		throw new UnsupportedOperationException(
				"It is no obvious what to return here. Probably you want to use a linear discretizer with bin width 1.0");
	}

	/**
	 * Just returns <tt>value</tt>.
	 *
	 * @return <tt>value</tt>
	 */
	@Override
	public double discretize(double value) {
		return value;
	}

	/**
	 * This method has no meaning here.
	 *
	 * @throws {@link UnsupportedOperationException}
	 */
	@Override
	public int index(double value) {
		throw new UnsupportedOperationException(
				"It is no obvious what to return here. Probably you want to use a linear discretizer with bin width 1.0");
	}

}
