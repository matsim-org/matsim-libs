/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonImpl.java
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

package org.matsim.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Implementation of the CountSimComparison Interface.
 *
 * @author dgrether
 */
public class CountSimComparisonImpl implements CountSimComparison {
	/**
	 * Attributes holding the data stored by this class
	 */
	private final int hour;
	/**
	 * Attributes holding the data stored by this class
	 */
	private final double countValue, simulationValue;
	/**
	 * the Id of the link
	 */
	private final Id<Link> id;

	/**
	 * @param id
	 * @param hour
	 * @param countValue2
	 * @param simValue
	 */
	public CountSimComparisonImpl(final Id<Link> id, final int hour, final double countValue2, final double simValue) {
		this.id = id;
		this.hour = hour;
		this.countValue = countValue2;
		this.simulationValue = simValue;
	}

	/**
	 * @see org.matsim.counts.CountSimComparison#calculateRelativeError() return
	 *      signed rel error
	 */
	@Override
	public double calculateRelativeError() {
		double count = this.getCountValue();
		double sim = this.getSimulationValue();
		if (count > 0) {
			return Math.min(100 * (sim - count) / count, 1000d);
		}
		if (sim > 0) {
			return 1000d;
		}
		return 0;
	}

	/**
	 * @see org.matsim.counts.CountSimComparison#getCountValue()
	 */
	@Override
	public double getCountValue() {
		return this.countValue;
	}

	/**
	 * @see org.matsim.counts.CountSimComparison#getHour()
	 */
	@Override
	public int getHour() {
		return this.hour;
	}

	/**
	 * @see org.matsim.counts.CountSimComparison#getId()
	 */
	@Override
	public Id<Link> getId() {
		return this.id;
	}

	/**
	 * @see org.matsim.counts.CountSimComparison#getSimulationValue()
	 */
	@Override
	public double getSimulationValue() {
		return this.simulationValue;
	}

}
