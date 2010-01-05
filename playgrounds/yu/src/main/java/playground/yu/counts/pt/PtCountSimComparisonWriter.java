/* *********************************************************************** *
 * project: org.matsim.*
 * PtCountSimComparisonWriter.java
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

/**
 * 
 */
package playground.yu.counts.pt;

import java.util.List;

import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.CountSimComparisonTimeFilter;

/**
 * @author yu
 * 
 */
public abstract class PtCountSimComparisonWriter {
	protected int iter;

	protected CountSimComparisonTimeFilter boardCountComparisonFilter,
			alightCountComparisonFilter;

	/**
	 * 
	 */
	public PtCountSimComparisonWriter(
			final List<CountSimComparison> boardCountSimCompList,
			final List<CountSimComparison> alightCountSimCompList) {
		this.boardCountComparisonFilter = new CountSimComparisonTimeFilter(
				boardCountSimCompList);
		this.alightCountComparisonFilter = new CountSimComparisonTimeFilter(
				alightCountSimCompList);
	}

	public abstract void writeFile(final String filename);

	public void setIterationNumber(int iter) {
		this.iter = iter;
	}
}
