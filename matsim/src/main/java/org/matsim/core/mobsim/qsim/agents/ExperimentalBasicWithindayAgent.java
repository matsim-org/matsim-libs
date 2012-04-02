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

package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PlanImpl;

/**This class is an attempt to provide access to the internals of DefaultPersonDriverAgent
 * in a way that it can be used for within-day replanning.
 * <p/>
 * <i>The class is, as its name states, experimental.  Use at your own risk, and expect even less support than with other pieces
 * of matsim. </i>
 * @author nagel
 */
public class ExperimentalBasicWithindayAgent extends PersonDriverAgentImpl implements PlanBasedWithinDayAgent {

	public static ExperimentalBasicWithindayAgent createExperimentalBasicWithindayAgent(
			Person p, Netsim simulation) {
		ExperimentalBasicWithindayAgent agent = new ExperimentalBasicWithindayAgent(p, simulation);
		return agent;
	}
	
	protected ExperimentalBasicWithindayAgent(Person p, Netsim simulation) {
		/*
		 * Create a copy of the person's selected plan.
		 * Notice that the executedPlan has a pointer to the person that is
		 * simulated with this Agent but is NOT added to the person's list of plans!
		 * The simulation should still score the person's selected plan. 
		 */
		super(p, copySelectedPlan(p), simulation);
	}

	private static Plan copySelectedPlan(Person p) {
		Plan executedPlan = new PlanImpl(p);
		((PlanImpl)executedPlan).copyPlan(p.getSelectedPlan());
		return executedPlan;
	}

	@Override
	public final Integer getCurrentPlanElementIndex() {
		return this.currentPlanElementIndex ;
	}

	@Override
	public final Integer getCurrentRouteLinkIdIndex() {
		return this.currentLinkIdIndex ;
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
	public final Leg getCurrentLeg() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return (Leg) currentPlanElement;
	}

}
