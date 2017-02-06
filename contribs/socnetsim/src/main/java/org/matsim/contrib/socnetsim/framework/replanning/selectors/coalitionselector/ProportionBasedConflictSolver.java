/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author thibautd
 */
public class ProportionBasedConflictSolver implements CoalitionSelector.ConflictSolver {
	private static final Logger log = Logger.getLogger( ProportionBasedConflictSolver.class );
	private final SocialNetwork socialNetwork;
	private final boolean tabuFriends;

	public ProportionBasedConflictSolver( final SocialNetwork socialNetwork ) {
		this.socialNetwork = socialNetwork;
		// make configurable?
		this.tabuFriends = false;
	}

	@Override
	public void attemptToSolveConflicts(
			final CoalitionSelector.RecordsOfJointPlan recordsPerJointPlan) {
		double maxProportion = Double.NEGATIVE_INFINITY;

		final Collection<Collection<PlanRecord>> currentMostPointed = new ArrayList<>();

		for ( final Collection<PlanRecord> records : recordsPerJointPlan.values() ) {
			final double proportion = getProportionPointed( records , maxProportion - 1E-9 );

			if ( proportion > maxProportion + 1E-9 ) {
				currentMostPointed.clear();
				maxProportion = proportion;
			}
			if ( proportion >= maxProportion - 1E-9 ) {
				currentMostPointed.add( records );
			}
		}

		final TabuList tabu = new TabuList();
		int c = 0;
		int ignoredIndiv = 0;
		int ignoredJoint = 0;
		boolean loop = true;
		while ( loop ) {
			loop = false;
			for ( final Collection<PlanRecord> records : currentMostPointed ) {
				if ( tabuFriends && tabu.addAndCheckTabu( records ) ) continue;

				boolean doneSomething = false;
				for ( final PlanRecord r : records ) {
					if ( r.isPointed() ) continue;
					// for each agent in the joint plan that does not point to the joint plan,
					// we remove the pointed joint plan.
					final Plan pointed = r.getAgent().getPointedPlan();
					final Collection<PlanRecord> recordsInJointPlan = recordsPerJointPlan.getRecords( pointed );

					// can happen if the initially pointed joint plan was removed for a friend,
					// when ignoring tabu restrictions.
					if ( recordsInJointPlan.size() == 1 ) {
						ignoredIndiv++;
						continue;
					}
					// test if greater than max proportion plus small tolerance, to avoid eg problems with proportion 0.5 with
					// plans of size 2
					if ( !tabuFriends && proportionPointedGreaterThan( recordsInJointPlan, maxProportion + 1E-9 ) ) {
						ignoredJoint++;
						continue;
					}

					for ( PlanRecord inPointed : recordsInJointPlan ) {
						inPointed.setInfeasible();
					}

					final Collection<PlanRecord> removed = recordsPerJointPlan.removeJointPlan( pointed );
					assert removed != null;
					doneSomething = true;
					// as long as we can "resolve" something for a plan, we continue...
					loop = true;
				}
				if ( doneSomething ) c++;
			}
		}
		if ( log.isTraceEnabled() ) {
			log.trace( "Handled "+c+" of "+currentMostPointed.size()+" possibly conflicting joint plans" );
			log.trace( ignoredIndiv+" agents were not corrected because they pointed to an individual plan" );
			log.trace( ignoredJoint+" agents were not corrected because they pointed to joint plan with a pointing proportion greater than "+maxProportion );
		}
	}

	private boolean proportionPointedGreaterThan(
			final Collection<PlanRecord> records,
			final double v ) {
		final double minNPointed = v * records.size();

		int nPoint = 0;
		int remaining = records.size();
		for ( final PlanRecord r : records ) {
			remaining--;
			if ( r.getAgent().getPointedPlan() == r.getPlan()  ) nPoint++;
			if ( nPoint >= minNPointed ) return true;
			if ( remaining + nPoint < minNPointed ) return false;
		}

		return false;
	}

	private double getProportionPointed(
			final Collection<PlanRecord> records,
			final double min ) {
		int nPoint = 0;

		final double minNPointed = min * records.size();
		int remaining = records.size();

		// if plan comes in here, it is not fully pointed
		if ( minNPointed > remaining - 1 ) return Double.NEGATIVE_INFINITY;

		for ( final PlanRecord r : records ) {
			remaining--;
			if ( r.getAgent().getPointedPlan() == r.getPlan()  ) nPoint++;

			// we know we will be lower than the minimum to find: abort
			if ( nPoint + remaining < minNPointed ) return Double.NEGATIVE_INFINITY;
		}

		return nPoint / ((double) records.size());
	}

	private class TabuList {
		final Set<Id<Person>> tabuAgents = new HashSet<>();

		public boolean addAndCheckTabu( final Iterable<PlanRecord> records ) {
			boolean tabu = areAltersTabu( records );

			for ( PlanRecord r :records ) {
				tabu = !tabuAgents.add( r.getPlan().getPerson().getId() ) || tabu;
			}

			return tabu;
		}

		// this is done for improving the number of pruned possible conflicts for very big groups.
		private boolean areAltersTabu( final Iterable<PlanRecord> records ) {
			for ( PlanRecord r :records ) {
				final Set<Id<Person>> alters = socialNetwork.getAlters( r.getPlan().getPerson().getId() );
				for ( Id<Person> alter : alters ) {
					if ( tabuAgents.contains( alter ) ) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
