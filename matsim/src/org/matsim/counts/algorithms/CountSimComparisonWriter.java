/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts.algorithms;

import java.util.List;

import org.matsim.counts.CountSimComparison;

public abstract class CountSimComparisonWriter {

	/**
	 * the time filter in h
	 *  A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m. to 2 a.m. 
	 */
	protected Integer timeFilter;
	/**
	 * The number of the iteration
	 */
	protected int iterationNumber;
	/**
	 * the filter used for timefiltering of the counts
	 */
	protected CountSimComparisonTimeFilter countComparisonFilter;

	public CountSimComparisonWriter(final List<CountSimComparison> countSimCompList) {
		super();
		this.countComparisonFilter = new CountSimComparisonTimeFilter(countSimCompList);
	}

	public abstract void writeFile(final String filename);
	
	
	/**
	 * Sets the time filter
	 * 
	 * @param t
	 *           A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m. to 2 a.m. 
	 */
	public void setTimeFilter(final Integer t) {
		this.timeFilter = t;
	}

	/**
	 * @param iterationNumber the iterationNumber to set
	 */
	public void setIterationNumber(final int iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

}