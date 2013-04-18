/* *********************************************************************** *
 * project: org.matsim.*
 * FixedStepsIntegerMovesGenerator.java
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
 * Generates moves with fixed steps for all integer values.
 * @author thibautd
 */
public class FixedStepsIntegerMovesGenerator implements MoveGenerator {
	private final List<Integer> steps;
	private final List<Integer> integerIndices;
	private final boolean switchSubsequentValues;

	public FixedStepsIntegerMovesGenerator(
			final Solution<?> initialSolution,
			final List<Integer> steps,
			final boolean switchSubsequentValues) {
		this.steps = steps;
		this.switchSubsequentValues = switchSubsequentValues;

		integerIndices = new ArrayList<Integer>();

		int i = 0;
		for (Value value : initialSolution.getGenotype()) {
			if (value.getValue() instanceof Integer) {
				integerIndices.add( i );
			}
			i++;
		}
	}

	@Override
	public Collection<Move> generateMoves() {
		List<Move> moves = new ArrayList<Move>();

		for (int index : integerIndices) {
			for (int step : steps) {
				moves.add( new IntegerValueChanger( index , step , switchSubsequentValues ) );
				moves.add( new IntegerValueChanger( index , -step , switchSubsequentValues ) );
			}
		}

		return moves;
	}
}

