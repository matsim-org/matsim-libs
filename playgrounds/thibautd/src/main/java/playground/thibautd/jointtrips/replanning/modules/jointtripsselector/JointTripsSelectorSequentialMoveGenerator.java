/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsSelectorSequentialMoveGenerator.java
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import playground.thibautd.tsplanoptimizer.framework.AppliedMoveListener;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.MoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.Solution;

/**
 * Generates one move per iteration.
 * @author thibautd
 */
public class JointTripsSelectorSequentialMoveGenerator implements MoveGenerator, AppliedMoveListener {
	private static final List<Move> EMPTY = Collections.emptyList();
	private final Iterator<Move> moves;
	private Move currentMove;

	public JointTripsSelectorSequentialMoveGenerator(
			final Random random,
			final int size) {
		List<Move> moves = new ArrayList<Move>();

		for (int i=0; i < size; i++) {
			moves.add( new MutateOneBoolean( i ) );
		}

		Collections.shuffle( moves , random );
		this.moves = moves.iterator();
		currentMove = this.moves.hasNext() ? this.moves.next() : null;
	}

	@Override
	public void notifyMove(
			final Solution currentSolution,
			final Move toApply,
			final double resultingFitness) {
		if (toApply == currentMove) {
			currentMove = moves.hasNext() ? moves.next() : null;
		}
		else {
			throw new RuntimeException( "expected "+currentMove+", got "+toApply );
		}
	}

	@Override
	public Collection<Move> generateMoves() {
		return currentMove != null ? Arrays.asList( currentMove ) : EMPTY;
	}
}

