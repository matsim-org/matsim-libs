/* *********************************************************************** *
 * project: org.matsim.*
 * ModeChainTabuChecker.java
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser.JointTimeModeChooserSolution.SubtourValue;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuChecker;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * @author thibautd
 */
public class ModeChainTabuList implements TabuChecker<JointPlan> {
	private final LinkedList<List<Value>> tabuChains = new LinkedList<List<Value>>();
	private final int size;

	public ModeChainTabuList(final int size) {
		this.size = size;
	}

	@Override
	public void notifyMove(
			final Solution<? extends JointPlan> currentSolution,
			final Move toApply,
			final double resultingFitness) {
		if (toApply instanceof SubtourAndParentsModeMove) {
			Solution<? extends JointPlan> newSolution = toApply.apply( currentSolution );
			tabuChains.add( getChain( newSolution ) );
		}
		else {
			tabuChains.add( null );
		}

		while (tabuChains.size() > size) {
			tabuChains.removeFirst();
		}
	}

	private final static List<Value> getChain(final Solution<? extends JointPlan> sol) {
		final List<Value> subtourValues = new ArrayList<Value>();

		for (Value val : sol.getGenotype()) {
			if (val instanceof SubtourValue) {
				subtourValues.add( val.createClone() );
			}
		}

		return subtourValues;
	}

	@Override
	public boolean isTabu(
			final Solution<? extends JointPlan> solution,
			final Move move) {
		if (move instanceof SubtourAndParentsModeMove) {
			// only forbid *mode* moves creating a tabu mode chain
			// (otherwise, good solutions not retained)
			return tabuChains.contains( getChain( move.apply( solution ) ) );
		}

		return false;
	}
}

