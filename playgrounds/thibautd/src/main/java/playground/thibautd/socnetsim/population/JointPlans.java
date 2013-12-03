/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlans.java
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimToplevelContainer;

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
public class JointPlans implements MatsimToplevelContainer {
	public static final String ELEMENT_NAME = "jointPlans";

	private final Map<Plan, JointPlan> planToJointPlan = new ConcurrentHashMap<Plan, JointPlan>();

	private final JointPlanFactory factory = new JointPlanFactory();
	
	public JointPlan getJointPlan(final Plan indivPlan) {
		return planToJointPlan.get( indivPlan );
	}

	public void removeJointPlan(final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				final Object removed = planToJointPlan.remove( indivPlan );
				if (removed != jointPlan) throw new PlanLinkException( removed+" differs from "+indivPlan );
			}
		}
	}

	public void addJointPlan(final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				final Object removed = planToJointPlan.put( indivPlan , jointPlan );
				if (removed != null && removed != jointPlan) {
					throw new PlanLinkException( removed+" was associated to "+indivPlan+
							" while trying to associate "+jointPlan );
				}
			}
		}
	}
	
	public boolean contains( final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				final Object removed = planToJointPlan.get( indivPlan );
				if (removed != null && removed == jointPlan) {
					return true;
				}
			}
			return false;
		}
	}

	public void addJointPlans(final Iterable<JointPlan> jointPlans) {
		for (JointPlan jp : jointPlans) addJointPlan( jp );
	}

	@Override
	public JointPlanFactory getFactory() {
		return factory;
	}

	public static class PlanLinkException extends RuntimeException {
		private static final long serialVersionUID = -6189128092802956514L;

		private PlanLinkException( final String msg ) {
			super( msg );
		}
	}


}

