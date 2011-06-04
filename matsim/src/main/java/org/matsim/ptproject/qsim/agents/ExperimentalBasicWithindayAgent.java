/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalBasicWithindayAgent.java
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

package org.matsim.ptproject.qsim.agents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.ptproject.qsim.interfaces.Netsim;

/**This class is an attempt to provide access to the internals of DefaultPersonDriverAgent
 * in a way that it can be used for within-day replanning.
 * <p/>
 * <i>The class is, as its name states, experimental.  Use at your own risk, and expect even less support than with other pieces
 * of matsim. </i>
 * @author nagel
 */
public class ExperimentalBasicWithindayAgent extends PersonDriverAgentImpl implements WithinDayAgent {

	private Plan executedPlan;
	
	public ExperimentalBasicWithindayAgent(Person p, Netsim simulation) {
		super(p, simulation);
		
		/*
		 * Create a copy of the person's selected plan.
		 * Notice that the executedPlan has a pointer to the person that is
		 * simulated with this Agent but is NOT added to the person's list of plans!
		 * The simulation should still score the person's selected plan. 
		 */
		executedPlan = new PlanImpl(p);
		((PlanImpl)executedPlan).copyPlan(p.getSelectedPlan());
	}

//	public final List<PlanElement> getModifiablePlanElements() {
//		return this.person.getSelectedPlan().getPlanElements() ;
//	}
	// use getModifiablePlan.  kai, nov'10

	@Override
	public final Integer getCurrentPlanElementIndex() {
		return this.currentPlanElementIndex ;
	}

	@Override
	public final Integer getCurrentRouteLinkIdIndex() {
		return this.currentLinkIdIndex ;
	}

	protected void setCachedNextLinkId(Id cachedNextLinkId) {
		// yyyy I am not convinced that this method really makes sense.  Needed for DgWithinday...  .  kai, oct'10

		this.cachedNextLinkId = cachedNextLinkId;
	}

	protected Id getCachedNextLinkId() {
		return this.cachedNextLinkId;
	}

	@Override
	public final void calculateDepartureTime( Activity act ) {
		super.calculateDepartureTime( act ) ;
	}

	@Override
	public final void resetCaches() {
		super.resetCaches() ;
	}

	@Override
	public Plan getSelectedPlan() {
		return executedPlan;
	}
	
	@Override
	//yyyy suggest merging with getExecutedPlan(). cdobler, feb'10
	public final Plan getModifiablePlan() {
		// yyyy for the time being, this returns the Person's selected plan, but this should be changed. kai, nov'10
		return this.person.getSelectedPlan();
	}

}
