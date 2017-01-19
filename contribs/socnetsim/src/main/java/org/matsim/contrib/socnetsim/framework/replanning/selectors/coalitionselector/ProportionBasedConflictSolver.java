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

	public ProportionBasedConflictSolver( final SocialNetwork socialNetwork ) {
		this.socialNetwork = socialNetwork;
	}

	@Override
	public void attemptToSolveConflicts(
			final CoalitionSelector.RecordsOfJointPlan recordsPerJointPlan) {
		double maxProportion = Double.NEGATIVE_INFINITY;

		final Collection<Collection<PlanRecord>> currentToRemove = new ArrayList<>();

		for ( final Collection<PlanRecord> records : recordsPerJointPlan.values() ) {
			int nPoint = 0;
			for ( final PlanRecord r : records ) {
				if ( r.getAgent().getPointedPlan() == r.getPlan()  ) nPoint++;
			}

			assert nPoint < records.size();
			final double proportion = nPoint / ((double) records.size());

			if ( proportion > maxProportion + 1E-9 ) {
				currentToRemove.clear();
				maxProportion = proportion;
			}
			if ( proportion >= maxProportion - 1E-9 ) {
				currentToRemove.add( records );
				break;
			}
		}

		final TabuList tabu = new TabuList();
		int c = 0;
		for ( final Collection<PlanRecord> records : currentToRemove ) {
			if ( tabu.addAndCheckTabu( records ) ) continue;
			c++;
			for ( final PlanRecord r : records ) {
				if ( r.isPointed() ) continue;
				// for each agent in the joint plan that does not point to the joint plan,
				// we remove the pointed joint plan.
				final Plan pointed = r.getAgent().getPointedPlan();
				for ( PlanRecord inPointed : recordsPerJointPlan.getRecords( pointed ) ) {
					inPointed.setInfeasible();
				}

				final Collection<PlanRecord> removed = recordsPerJointPlan.removeJointPlan( pointed );
				assert removed != null;
			}
		}
		if ( log.isTraceEnabled() ) log.trace( "Handled "+c+" of "+currentToRemove.size()+" possibly conflicting joint plans" );
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
