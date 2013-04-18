/* *********************************************************************** *
 * project: org.matsim.*
 * PlanTabuList.java
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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuChecker;
import playground.thibautd.tsplanoptimizer.timemodechooser.IntegerValueChanger;
import playground.thibautd.tsplanoptimizer.timemodechooser.ModeMove;

/**
 * A tabu list that makes plans alternatively modified.
 * @author thibautd
 */
public class PlanTabuList implements TabuChecker<JointPlan> {
	private final int size;
	private final List<Integer> lastIndexPerPlan;
	private final LinkedList<Integer> tabuPlans = new LinkedList<Integer>();

	public PlanTabuList(final JointTimeModeChooserSolution initialSolution) {
		lastIndexPerPlan = initialSolution.getLastValuePerPlan();
		Collections.sort( lastIndexPerPlan );
		size = lastIndexPerPlan.size() - 1;
	}

	@Override
	public void notifyMove(
			final Solution<? extends JointPlan> currentSolution,
			final Move toApply,
			final double resultingFitness) {
		tabuPlans.add( getPlanId( toApply ) );

		while (tabuPlans.size() > size) {
			tabuPlans.removeFirst();
		}
	}

	@Override
	public boolean isTabu(
			final Solution<? extends JointPlan> solution,
			final Move move) {
		return tabuPlans.contains( getPlanId( move ) );
	}

	private int getPlanId(final Move move) {
		int modifiedIndex;

		if (move instanceof IntegerValueChanger) {
			modifiedIndex = ((IntegerValueChanger) move).getIndex();
		}
		else if (move instanceof ModeMove) {
			modifiedIndex = ((ModeMove) move).getIndex();
		}
		else {
			// cannot infer
			return -1;
		}

		int planIndex = 0;

		for (Integer i : lastIndexPerPlan) {
			// ordered
			if ( modifiedIndex < i ) {
				break;
			}
			planIndex++;
		}

		return planIndex;
	}
}

