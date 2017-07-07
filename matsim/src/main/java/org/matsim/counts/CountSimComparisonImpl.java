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
	 * the Id of the count station
	 */
	private final String csId;
	
	/**
	 * @param id
	 * @param hour
	 * @param countValue
	 * @param simValue
	 */
	@Deprecated
	public CountSimComparisonImpl(final Id<Link> id, final int hour, final double countValue, final double simValue) {
		this.id = id;
		this.csId = null;
		this.hour = hour;
		this.countValue = countValue;
		this.simulationValue = simValue;
	}

	/**
	 * @param id
	 * @param csId
	 * @param hour
	 * @param countValue
	 * @param simValue
	 */
	public CountSimComparisonImpl(final Id<Link> id, final String csId, final int hour, final double countValue, final double simValue) {
		this.id = id;
		this.csId = csId;
		this.hour = hour;
		this.countValue = countValue;
		this.simulationValue = simValue;
	}
	
	@Override
	public String getCsId() {
		return this.csId;
	}
	
	/**
	 * @see org.matsim.counts.CountSimComparison#calculateRelativeError() 
	 * @return signed relative error
	 */
	@Override
	public double calculateRelativeError() {
		double count = this.getCountValue();
		double sim = this.getSimulationValue();
		if (count > 0) {
			return Math.min((sim - count) / count, 1000d);
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

	/**
	 * @see org.matsim.counts.CountSimComparison#calculateNormalizedRelativeError()
	 * @return normalized relative error
	 */
	@Override
	public double calculateNormalizedRelativeError() {
		final double max = Math.max(this.simulationValue, this.countValue);
		if (max == 0.0) return 0;
		return Math.abs(this.simulationValue - this.countValue) / max;
	}

	/**
	 * @see org.matsim.counts.CountSimComparison#calculateGEHValue()
	 */
	@Override
	public double calculateGEHValue() {
		final double diff = this.simulationValue - this.countValue;
		final double sum = this.simulationValue + this.countValue;
		final double gehV = Math.sqrt(2 * diff * diff / sum);
		return gehV;
	}
}