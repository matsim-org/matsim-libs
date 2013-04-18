/* *********************************************************************** *
 * project: org.matsim.*
 * AllPossibleModesMovesGenerator.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.MoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * @author thibautd
 */
public class AllPossibleModesMovesGenerator implements MoveGenerator {
	private final List<Move> moves = new ArrayList<Move>();

	public AllPossibleModesMovesGenerator(
			final Solution<?> initialSolution,
			final Collection<String> possibleModes) {
		int i = 0;
		for (Value value : initialSolution.getGenotype()) {
			if (value.getValue() instanceof String) {
				for (String mode : possibleModes) {
					moves.add( new ModeMove( i , mode ) );
				}
			}
			i++;
		}
	}

	@Override
	public Collection<Move> generateMoves() {
		return moves;
	}
}

