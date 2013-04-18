/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourAndParentsModeMove.java
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

import java.util.List;

import playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser.JointTimeModeChooserSolution.SubtourValue;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * @author thibautd
 */
public class SubtourAndParentsModeMove implements Move {
	private final int index;
	private final String newMode;

	public SubtourAndParentsModeMove(
			final int index,
			final String newMode) {
		this.index = index;
		this.newMode = newMode;
	}

	@Override
	public <T> Solution<T> apply(final Solution<T> solution) {
		Solution<T> newSolution = solution.createClone();

		List<? extends Value> values = newSolution.getGenotype();
		SubtourValue value = (SubtourValue) values.get( index );

		while (value != null) {
			value.setValue( newMode );
			int par = value.getParentSubtourValueIndex();
			value = par < 0 ? null : (SubtourValue) values.get( par );
		}

		return newSolution;
	}

	@Override
	public Move getReverseMove() {
		throw new UnsupportedOperationException( "no reverse move makes sense here" );
	}
}

