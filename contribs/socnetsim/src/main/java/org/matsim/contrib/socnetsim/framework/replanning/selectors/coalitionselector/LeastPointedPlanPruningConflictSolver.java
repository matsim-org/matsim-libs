/* *********************************************************************** *
 * project: org.matsim.*
 * LeastPointedPlanPruningConflictSolver.java
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

import java.util.Collection;
import java.util.Map;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector.ConflictSolver;

/**
 * @author thibautd
 */
public class LeastPointedPlanPruningConflictSolver implements ConflictSolver {

	@Override
	public void attemptToSolveConflicts(
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan) {
		int minPoint = Integer.MAX_VALUE;
		Map.Entry<JointPlan , Collection<PlanRecord>> toMark = null;
		for ( final Map.Entry<JointPlan , Collection<PlanRecord>> entry : recordsPerJointPlan.entrySet() ) {
			final Collection<PlanRecord> records = entry.getValue();

			int nPoint = 0;
			for ( final PlanRecord r : records ) {
				if ( r.getAgent().getPointedPlan() == r.getPlan()  ) nPoint++;
			}

			if ( nPoint > 0 && nPoint < minPoint ) {
				minPoint = nPoint;
				toMark = entry;
			}
		}

		if ( toMark == null ) {
			throw new RuntimeException( "could not find least pointed joint plan in "+recordsPerJointPlan );
		}

		for ( final PlanRecord r : toMark.getValue() ) r.setInfeasible();
		recordsPerJointPlan.remove( toMark.getKey() );
	}
}

