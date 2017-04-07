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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * Stores links between individual plans,
 * as defined by joint plans.
 * <br>
 * Using the {@link JointPlanFactory} to create instances of individual
 * plans will allow to cache the JointPlan in the Plan instance,
 * avoiding expensive Map lookups.
 * <br>
 * The caching procedure is able to cope with various instances of jointPlan.
 *
 * @author thibautd
 */
public class JointPlans implements MatsimToplevelContainer {
	private static final Logger log = Logger.getLogger( JointPlans.class );
	public static final String ELEMENT_NAME = "jointPlans";

	private final Map<Plan, JointPlan> planToJointPlan = new ConcurrentHashMap<>();

	private final JointPlanFactory factory = new JointPlanFactory();

	private static AtomicInteger globalInstanceCount = new AtomicInteger( 0 );
	private final int instanceId = globalInstanceCount.getAndIncrement();
	
	public JointPlan getJointPlan(final Plan indivPlan) {
		if ( indivPlan instanceof PlanWithCachedJointPlan ) {
			final PlanWithCachedJointPlan withCache = (PlanWithCachedJointPlan) indivPlan;

			if ( withCache.hasCached( instanceId ) ) {
				return withCache.getJointPlan( instanceId );
			}
			else {
				final JointPlan jp = planToJointPlan.get( indivPlan );
				withCache.setJointPlan( instanceId , jp );
				return jp;
			}
		}
		return planToJointPlan.get( indivPlan );
	}

	public void removeJointPlan(final JointPlan jointPlan) {
		if ( log.isTraceEnabled() ) {
			log.trace( "remove joint plan "+jointPlan+" from JointPlans instance "+instanceId );
		}
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				if ( indivPlan instanceof PlanWithCachedJointPlan ) {
					final PlanWithCachedJointPlan withCache = (PlanWithCachedJointPlan) indivPlan;

					if (withCache.getJointPlan( instanceId ) != jointPlan) throw new PlanLinkException( withCache.getJointPlan( instanceId )+" differs from "+indivPlan );

					withCache.resetJointPlan( instanceId );
				}

				// keep in Map no matter what, as the joint plan may be uncached in the plan.
				// This results in slower removes... Only get is optimized by the cache
				final Object removed = planToJointPlan.remove( indivPlan );
				if (removed != jointPlan) throw new PlanLinkException( removed+" differs from "+indivPlan+" for "+jointPlan );
			}
		}
	}

	public synchronized void clear() {
		// first clear caches
		for ( Plan indivPlan : planToJointPlan.keySet() ) {
			if ( indivPlan instanceof PlanWithCachedJointPlan ) {
				final PlanWithCachedJointPlan withCache = (PlanWithCachedJointPlan) indivPlan;
				withCache.resetJointPlan( instanceId );
			}
		}
		planToJointPlan.clear();
	}

	public void addJointPlan(final JointPlan jointPlan) {
		if ( log.isTraceEnabled() ) {
			log.trace( "add joint plan "+jointPlan+" to JointPlans instance "+instanceId );
		}
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				if ( indivPlan instanceof PlanWithCachedJointPlan ) {
					final PlanWithCachedJointPlan withCache = (PlanWithCachedJointPlan) indivPlan;

					if ( withCache.getJointPlan( instanceId ) != null && withCache.getJointPlan( instanceId ) != jointPlan) {
						throw new PlanLinkException( withCache.getJointPlan( instanceId )+" was associated to "+indivPlan+
								" while trying to associate "+jointPlan );
					}

					withCache.setJointPlan( instanceId , jointPlan );
				}

				// keep in Map no matter what, as the joint plan may be uncached in the plan
				// This results in slower additions... Only get is optimized by the cache
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
				if ( indivPlan instanceof PlanWithCachedJointPlan ) {
					if ( ((PlanWithCachedJointPlan) indivPlan).getJointPlan( instanceId ) == jointPlan ) {
						return true;
					}
				}
				else {
					final Object removed = planToJointPlan.get( indivPlan );
					if (removed != null && removed == jointPlan) {
						return true;
					}
				}
			}
			return false;
		}
	}

	public void addJointPlans(final Iterable<JointPlan> jointPlans) {
		for (JointPlan jp : jointPlans) addJointPlan( jp );
	}

	public Collection<JointPlan> getJointPlansOfPerson( final Person person ) {
		final Collection<JointPlan> jps = new ArrayList<>( person.getPlans().size() );
		for ( Plan p : person.getPlans() ) {
			final JointPlan jp = getJointPlan( p );
			if ( jp != null ) jps.add( jp );
		}
		return jps;
	}

	public Collection<JointPlan> getJointPlansOfSameComposition( final JointPlan jointPlan ) {
		final Person person = jointPlan.getIndividualPlans().values().iterator().next().getPerson();

		final Collection<JointPlan> jps = new ArrayList<>( person.getPlans().size() );
		for ( Plan p : person.getPlans() ) {
			final JointPlan jp = getJointPlan( p );

			if ( jp != null &&
					jp.getIndividualPlans().keySet().equals(
							jointPlan.getIndividualPlans().keySet() ) ) {
				jps.add( jp );
			}
		}
		return jps;
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

