/* *********************************************************************** *
 * project: org.matsim.*
 * IncompatiblePlansIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

/**
 * Allows to define incomaptibility relations between plans.
 * Two incompatible plans cannot be selected at the same time.
 * This is useful for instance when using vehicle ressources,
 * to enforce that only one joint plan related to one given vehicle
 * is selected at the same time.
 *
 * @author thibautd
 */
public interface IncompatiblePlansIdentifier {
	/**
	 * @param plan the plan for which the incompatible plans are to identify
	 * @return a collection containing ids of "incompatibility groups".
	 * Two plans are considered incompatible if they have at least one group
	 * in common.
	 * The general contract is that:
	 * <ul>
	 * <li> all plans of a joint plan should pertain to the same groups
	 * <li> the plans incompatible because of joint plans constraint do not have
	 * to be identified as such here (but can)
	 * </ul>
	 */
	public Set<Id> identifyIncompatibilityGroups(Plan plan);
}

