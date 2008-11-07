/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.population;

import org.matsim.basic.v01.BasicPerson;
import org.matsim.utils.customize.Customizable;



/**
 * @author dgrether
 *
 */
public interface Person extends BasicPerson<Plan>, Customizable{
	
	public Plan createPlan(final boolean selected);
	
	/**
	 * Removes all plans except the selected one.
	 */
	public void removeUnselectedPlans();

	/**
	 * Returns a random plan from the list of all plans this agent has.
	 *
	 * @return A random plan, or <code>null</code> if none such plan exists
	 */
	public Plan getRandomPlan();

	/**
	 * Returns a plan with undefined score, chosen randomly among all plans
	 * with undefined score.
	 *
	 * @return A random plan with undefined score, or <code>null</code> if none such plan exists.
	 */
	public Plan getRandomUnscoredPlan();

	public void exchangeSelectedPlan(final Plan newPlan, final boolean appendPlan) ;
	/**
	 * Makes a copy of the currently selected plan, marking the copy as selected.
	 *
	 * @return the copy of the selected plan. Returns null if there is no selected plan.
	 */
	public Plan copySelectedPlan();

	/**
	 * Removes the specified plan from the agent. If the removed plan was the selected one,
	 * a new random plan will be selected.
	 * 
	 * @param plan
	 * @return <code>true</code> if the plan was removed from the person
	 */
	public boolean removePlan(final Plan plan);
	
	/** Removes the plans with the worst score until only <code>maxSize</code> plans are left.
	 * Plans with undefined scores are treated as plans with very bad scores and thus removed
	 * first. If there are several plans with the same bad score, it can not be predicted which
	 * one of them will be removed.<br>
	 * This method insures that if there are different types of plans (see
	 * {@link org.matsim.population.Plan#getType()}),
	 * at least one plan of each type remains. This could lead to the worst plan being kept if
	 * it is the only one of it's type. Plans with type <code>null</code> are handled like any
	 * other type, and are differentiated from plans with the type set to an empty String.<br>
	 *
	 * If there are more plan-types than <code>maxSize</code>, it is not possible to reduce the
	 * number of plans to the requested size.<br>
	 *
	 * If the selected plan is on of the deleted ones, a randomly chosen plan will be selected.
	 *
	 * @param maxSize The number of plans that should be left.
	 */
	public void removeWorstPlans(final int maxSize);

	public Knowledge createKnowledge(final String desc);
	/**
	 * @param visualizerData sets the optional user data for visualizer
	 */
	public void setVisualizerData(final String visualizerData);

	/**
	 *
	 * @return Returns the visualizer data
	 */
	public String getVisualizerData();
}
