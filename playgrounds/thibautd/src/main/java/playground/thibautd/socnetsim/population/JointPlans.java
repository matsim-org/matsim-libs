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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
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
	private static final Logger log =
		Logger.getLogger(JointPlans.class);

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
	private final String attName;

	private static AtomicInteger instanceCount = new AtomicInteger( 0 );
	private final JointPlanFactory factory = new JointPlanFactory();
	
	public JointPlans() {
		if (instanceCount.incrementAndGet() > 1) {
			log.warn( "there are several instances of JointPlans. Did you expect it?" );
		}
		attName = "jointPlanReference_" + instanceCount.toString();
	}

	public JointPlan getJointPlan(final Plan indivPlan) {
		// XXX should be synchronized!
		return (JointPlan) indivPlan.getCustomAttributes().get( attName );
	}

	public void removeJointPlan(final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				Object removed = indivPlan.getCustomAttributes().remove( attName );
				if (removed != jointPlan) throw new PlanLinkException( removed+" differs from "+indivPlan );
			}
		}
	}

	public void addJointPlan(final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				Object removed = indivPlan.getCustomAttributes().put( attName , jointPlan );
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
				Object removed = indivPlan.getCustomAttributes().get( attName );
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

