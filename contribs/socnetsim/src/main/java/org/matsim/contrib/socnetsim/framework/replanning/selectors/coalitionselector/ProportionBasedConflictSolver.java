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

import org.matsim.api.core.v01.population.Plan;

import java.util.Collection;

/**
 * @author thibautd
 */
public class ProportionBasedConflictSolver implements CoalitionSelector.ConflictSolver {

	@Override
	public void attemptToSolveConflicts(
			final CoalitionSelector.RecordsOfJointPlan recordsPerJointPlan) {
		double maxProportion = Double.NEGATIVE_INFINITY;
		Collection<PlanRecord> mostPointed = null;

		for ( final Collection<PlanRecord> records : recordsPerJointPlan.values() ) {
			int nPoint = 0;
			for ( final PlanRecord r : records ) {
				if ( r.getAgent().getPointedPlan() == r.getPlan()  ) nPoint++;
			}

			assert nPoint < records.size();
			final double proportion = nPoint / ((double) records.size());

			if ( proportion > maxProportion ) {
				maxProportion = proportion;
				mostPointed = records;
			}
			if ( nPoint == recordsPerJointPlan.getMaxJointPlanSize() -1 ) break;
		}

		if ( mostPointed == null ) {
			throw new RuntimeException( "could not find least pointed joint plan in "+recordsPerJointPlan );
		}

		for ( final PlanRecord r : mostPointed ) {
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
}
