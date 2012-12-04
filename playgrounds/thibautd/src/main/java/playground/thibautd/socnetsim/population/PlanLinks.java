/* *********************************************************************** *
 * project: org.matsim.*
 * PlanLinks.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.population;

import org.matsim.api.core.v01.population.Plan;


/**
 * Stores links between individual plans,
 * as defined by joint plans.
 * <br>
 * It uses internally a WeakHashMap, so that if an indivudal plan is not "strongly"
 * referenced anywhere, the mapping will be automatically forgotten at the next
 * garbage collection.
 *
 * @author thibautd
 */
public class PlanLinks {
	// Note: using a WeakHashMap is much harder than expected:
	// or you reference a Plan->JointPlan mapping, and the Plans
	// are never finalized, as they are referenced by the JointPlan;
	// or you reference a Plan->WeakReference<JointPlan> mapping,
	// and the joint plans just vanish randomly when they are not referenced
	// anywhere.
	// The easier way to achieve the desired behavior (forget the joint plan
	// when the individual plans in it are not referenced anywhere else)
	// can be achieved by:
	// - weakly referencing the individual plans in JointPlan and use
	// a WeakHashMap here, which is dirty
	// - attaching the JointPlan (strong) reference to the Plan instance,
	// which avoids spreading the WeakReference dirt all over the place,
	// but forces to use the deprecated "custom attributes".
	// If this comes in the way of somebody when trying to remove the
	// "custom attributes", please drop me an e-mail, I'll search for another
	// solution. td, 12.2012
	private static final String ATT_NAME = "jointPlanReference";
	
	PlanLinks() {}

	public JointPlan getJointPlan(final Plan indivPlan) {
		return (JointPlan) indivPlan.getCustomAttributes().get( ATT_NAME );
	}

	public void removeJointPlan(final JointPlan jointPlan) {
		for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
			Object removed = indivPlan.getCustomAttributes().remove( ATT_NAME );
			if (removed != jointPlan) throw new PlanLinkException( removed+" differs from "+indivPlan );
		}
	}

	public void addJointPlan(final JointPlan jointPlan) {
		for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
			Object removed = indivPlan.getCustomAttributes().put( ATT_NAME , jointPlan );
			if (removed != null && removed != jointPlan) {
				throw new PlanLinkException( removed+" was associated to "+indivPlan+
						" while trying to associate "+jointPlan );
			}
		}
	}

	public static class PlanLinkException extends RuntimeException {
		private PlanLinkException( final String msg ) {
			super( msg );
		}
	}


}

