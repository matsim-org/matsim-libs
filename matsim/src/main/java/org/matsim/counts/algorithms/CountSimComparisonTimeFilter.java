/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonTimeFilter.java
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
import java.util.Vector;

import org.matsim.counts.CountSimComparison;

/**
 * This class can be used to return filtered views on a List of
 * CountSimComparison objects.
 *
 * @author dgrether
 *
 */
public class CountSimComparisonTimeFilter {
	/**
	 * this array maps the time steps to Lists of CountSimComparison objects
	 */
	private final List<CountSimComparison>[] countSimComparisonTimeMap;

	/**
	 * The list containing the comparisons
	 */
	private final List<CountSimComparison> countSimComparisons;

	/**
	 * Fills the Array for each hour with the appropriate CountSimComparison objects
	 * from the list given as parameter.
	 * @param countSimComparisons
	 *
	 */
	public CountSimComparisonTimeFilter(
			final List<CountSimComparison> countSimComparisons) {
		this.countSimComparisons = countSimComparisons;
		int countsPerHour = this.countSimComparisons.size() / 24;
		// initialize array
		this.countSimComparisonTimeMap = new List[24];
		for (int i = 0; i < 24; i++) {
			this.countSimComparisonTimeMap[i] = new Vector<CountSimComparison>(countsPerHour);
		}
		// and add the data
		for (CountSimComparison csc : this.countSimComparisons) {
			this.countSimComparisonTimeMap[csc.getHour() - 1].add(csc);
		}
	}

	/**
	 * @param timefilter
	 *          A time step to filter the counts. Time may be null to avoid
	 *          filtering. A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m.
	 *          to 2 a.m. If null nothing is filtered.
	 *
	 * @return All counts if the parameter is null, else a subset of all counts for the given hour
	 */
	public List<CountSimComparison> getCountsForHour(final Integer timefilter) {
		// only need to do this once
		if (timefilter == null) {
			return this.countSimComparisons;
		}
		return this.countSimComparisonTimeMap[timefilter.intValue() - 1];
	}
}
