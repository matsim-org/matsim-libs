/* *********************************************************************** *
 * project: org.matsim.*
 * LeastAverageWeightJointPlanPruningConflictSolver.java
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
package playground.thibautd.socnetsim.framework.replanning.selectors.coalitionselector;

import java.util.Collection;
import java.util.Map;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector.ConflictSolver;

/**
 * @author thibautd
 */
public class LeastAverageWeightJointPlanPruningConflictSolver implements ConflictSolver {

	@Override
	public void attemptToSolveConflicts(
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan) {
		double minAvg = Double.POSITIVE_INFINITY;
		Map.Entry<JointPlan , Collection<PlanRecord>> toMark = null;

		for ( final Map.Entry<JointPlan , Collection<PlanRecord>> entry : recordsPerJointPlan.entrySet() ) {
			final Collection<PlanRecord> records = entry.getValue();

			double avg = 0;
			boolean isPointed = false;
			for ( final PlanRecord r : records ) {
				if ( !r.isFeasible() ) throw new RuntimeException( "should not get unfeasible plans!" );
				isPointed = isPointed || r.getAgent().getPointedPlan() == r.getPlan();
				avg += r.getWeight();
			}
			avg /= records.size();

			if ( avg < minAvg ) {
				minAvg = avg;
				toMark = entry;
			}
		}

		if ( toMark == null ) {
			throw new RuntimeException( "could not find pointed joint plan of minimum average weight in "+recordsPerJointPlan );
		}

		for ( final PlanRecord r : toMark.getValue() ) r.setInfeasible();
		recordsPerJointPlan.remove( toMark.getKey() );
	}
}

