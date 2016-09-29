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
package org.matsim.pt.counts.obsolete;

import java.util.List;

import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.CountSimComparisonTimeFilter;

@Deprecated // we should try to rather make CountSimComparisonKMLWriter more general. kai, dec'13
public abstract class PtCountSimComparisonWriter implements MatsimSomeWriter {
	public enum PtCountsType {
		Boarding, Alighting, Occupancy
	}

	@Deprecated // we should try to rather make CountSimComparisonKMLWriter more general. kai, dec'13
    protected int iter;

	@Deprecated // we should try to rather make CountSimComparisonKMLWriter more general. kai, dec'13
	protected CountSimComparisonTimeFilter boardCountComparisonFilter, alightCountComparisonFilter, occupancyCountComparisonFilter;

	/**
	 * 
	 */
	@Deprecated // we should try to rather make CountSimComparisonKMLWriter more general. kai, dec'13
	public PtCountSimComparisonWriter(final List<CountSimComparison> boardCountSimCompList, final List<CountSimComparison> alightCountSimCompList, final List<CountSimComparison> occupancyCountSimCompList) {
		if ( boardCountSimCompList != null ) {
			this.boardCountComparisonFilter = new CountSimComparisonTimeFilter(boardCountSimCompList);
		}
		if ( alightCountSimCompList!=null ) {
			this.alightCountComparisonFilter = new CountSimComparisonTimeFilter(alightCountSimCompList);
		}
		if ( occupancyCountSimCompList != null ) {
			this.occupancyCountComparisonFilter = new CountSimComparisonTimeFilter(occupancyCountSimCompList);
		}
	}

	@Deprecated // we should try to rather make CountSimComparisonKMLWriter more general. kai, dec'13
	public abstract void writeFile(final String filename);

	@Deprecated // we should try to rather make CountSimComparisonKMLWriter more general. kai, dec'13
	public void setIterationNumber(int iter) {
		this.iter = iter;
	}
}
