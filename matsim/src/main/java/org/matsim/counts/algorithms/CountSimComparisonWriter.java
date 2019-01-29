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

abstract class CountSimComparisonWriter {

	protected int iterationNumber;
	
	protected CountSimComparisonTimeFilter countComparisonFilter;

	public CountSimComparisonWriter(final List<CountSimComparison> countSimCompList) {
		super();
		this.countComparisonFilter = new CountSimComparisonTimeFilter(countSimCompList);
	}

	public abstract void writeFile(final String filename);

	/**
	 * @param iterationNumber the iterationNumber to set
	 */
	public void setIterationNumber(final int iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

}