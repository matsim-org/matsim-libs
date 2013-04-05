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

import java.util.Collection;

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
	 * @return a collection containing all the incompatible plans.
	 * The general contract is that:
	 * <ul>
	 * <li> if a plan in the collection is part of a joint plan, all plans of
	 * the joint plan should pertain to the collection.
	 * <li> the plans in the list should be plans of other agents (the plans
	 * of the same agent are trivially mutually incompatible)
	 * <li> the plans incompatible because of joint plans constraint do not have
	 * to pertain to the collection.
	 * <li> if a plan A pertains to the collection returned for a plan B,
	 * the plan B must pertain to the collection returned for the plan A.
	 * </ul>
	 */
	public Collection<Plan> identifyIncompatiblePlans(Plan plan);
}

