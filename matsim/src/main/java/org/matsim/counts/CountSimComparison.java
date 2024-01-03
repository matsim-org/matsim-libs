/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparison.java
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

/**
 * Classes implementing this interface can be used to access the data
 * needed to compare traffic counts and simulation traffic at a specific
 * link for a one hour time step.
 *
 * @author dgrether
 */
public interface CountSimComparison<T> {

	/**
	 * @return The Id of the link
	 */
	public Id<T> getId();

	/**
	 *
	 * @return The Id of the count station
	 */
	public String getCsId();

	/**
	 * The time at which the data was measured.
	 * @return A value in 1..24, 1 means 0 - 1 am, 2 means 1 - 2 am and so on
	 */
	public int getHour();

	/**
	 * @return The real traffic amount
	 */
	public double getCountValue();

	/**
	 * @return The traffic amount of the simulation
	 */
	public double getSimulationValue();

	/**
	 * Calculates the relative error.
	 * @return the relative error
	 */
	public double calculateRelativeError();

	/**
	 * Calculates the normalized relative error.
	 * @return the normalized relative error
	 */
	public double calculateNormalizedRelativeError();

	/**
	 * Calculates the GEH value
	 * @return the GEH value
	 */
	public double calculateGEHValue();
}
