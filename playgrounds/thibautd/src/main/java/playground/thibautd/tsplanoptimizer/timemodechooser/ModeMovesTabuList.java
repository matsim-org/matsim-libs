/* *********************************************************************** *
 * project: org.matsim.*
 * ModeMovesTabuList.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import java.util.LinkedList;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuChecker;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * {@link TabuChecker} verifying that previously changed modes are not re-examined
 * during a given number of iterations. Moves resulting in no change are also considered
 * tabu.
 *
 * @author thibautd
 */
public class ModeMovesTabuList implements TabuChecker<Plan> {
	private final LinkedList<Tuple<Integer, String>> tabuList = new LinkedList<Tuple<Integer, String>>();
	private final int length;

	public ModeMovesTabuList(
			final int length) {
		this.length = length;
	}

	@Override
	public void notifyMove(
			final Solution<? extends Plan> currentSolution,
			final Move toApply,
			final double resultingFitness) {
		if (toApply != null && toApply instanceof ModeMove) {
			ModeMove modeMove = (ModeMove) toApply;
			Value toChange = currentSolution.getGenotype().get( modeMove.getIndex() );
			tabuList.addLast(
					new Tuple<Integer, String>(
						modeMove.getIndex(),
						(String) toChange.getValue()));
		}
		else {
			tabuList.addLast( null );
		}

		while (tabuList.size() > length) {
			tabuList.removeFirst();
		}
	}

	@Override
	public boolean isTabu(
			final Solution<? extends Plan> solution,
			final Move move) {
		if (move instanceof ModeMove) {
			ModeMove modeMove = (ModeMove) move;
			Value toChange = solution.getGenotype().get( modeMove.getIndex() );

			if (toChange.getValue().equals( modeMove.getMode() )) {
				// prevent "identity" moves
				return true;
			}

			return tabuList.contains(
					new Tuple<Integer, String>(
						modeMove.getIndex(),
						modeMove.getMode()));
		}

		return false;
	}
}

