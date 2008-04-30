/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.facilities.filters;

import org.matsim.facilities.Facility;
import org.matsim.facilities.algorithms.FacilityAlgorithmI;
import org.matsim.plans.Plan;
import org.matsim.plans.filters.FilterI;

/**
 * @author meisterk
 *
 */
public interface FacilityFilter extends FacilityAlgorithmI, FilterI {

	/**
	 * Judges whether the facility will be selected or not.
	 *
	 * @param facility
	 * @return true if the facility meets the criterion of the filter.
	 */
	boolean judge(Facility facility);
	
	/**
	 * Sends the facility to the next algorithm
	 *
	 * @param facility
	 */
	void run(Facility facility);
}
