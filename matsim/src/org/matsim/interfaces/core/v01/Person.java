/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.interfaces.core.v01;

import java.util.TreeSet;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.population.BasicPerson;
import org.matsim.population.Desires;
import org.matsim.population.Knowledge;
import org.matsim.utils.customize.Customizable;

/**
 * @author dgrether
 */
public interface Person extends BasicPerson<Plan>, Customizable{
	
	public void addPlan(final Plan plan);
	
	public Plan createPlan(final boolean selected);
	
	public Plan getSelectedPlan();

	/**
	 * Sets the selected plan of a person. If the plan is not part of the person,
	 * nothing is changed.
	 *
	 * @param selectedPlan the plan to be the selected one of the person
	 */
	public void setSelectedPlan(final Plan selectedPlan);
	
	
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
	 * {@link org.matsim.interfaces.core.v01.Plan#getType()}),
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
	
	public void setHousehold(Household hh);
	
	public Household getHousehold();

	
//TODO [kai]: Would make more sense to be to have something like "getAttributes" and "getPlans".  Current version seems a bit
//over-specified to me.  kai, feb09


	public String getSex();

	public int getAge();

	public String getLicense();

	public boolean hasLicense();

	public String getCarAvail();

	/**
	 * @return "yes" if the person has a job
	 * @deprecated use {@link #isEmployed()}
	 */
	@Deprecated
	public String getEmployed();

	public boolean isEmployed();
	// TODO [kn]: need setter

	public void setAge(final int age);

	public void setSex(final String sex);

	public void setLicence(final String licence);

	public void setCarAvail(final String carAvail);

	public void setEmployed(final String employed);

//	public Knowledge createKnowledge(final String desc);

	@Deprecated // should either be in Builder, or not there at all (too new)
	public Desires createDesires(final String desc);

	public void addTravelcard(final String type);

	public TreeSet<String> getTravelcards();

	@Deprecated // not yet well enough understood
	public Knowledge getKnowledge();

	@Deprecated // not yet well enough understood
	public Desires getDesires();

	public Id getFiscalHouseholdId();

	
	
}

