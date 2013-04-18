/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourAndParentsModeMoveGenerator.java
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser.JointTimeModeChooserSolution.SubtourValue;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.MoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * @author thibautd
 */
public class SubtourAndParentsModeMoveGenerator implements MoveGenerator {
	private final List<Move> moves;
	private final int nMoves;
	private final Random random;

	public SubtourAndParentsModeMoveGenerator(
			final Random random,
			final Solution<?> initialSolution,
			final Collection<String> modes,
			final double fraction) {
		this.random = random;
		List<Move> movesList = new ArrayList<Move>();

		int i=0;
		for (Value value : initialSolution.getGenotype()) {
			if (value instanceof SubtourValue) {
				for (String mode : modes) {
					movesList.add( new SubtourAndParentsModeMove( i , mode ) );
				}
			}
			i++;
		}

		this.moves = Collections.unmodifiableList( movesList );

		if (fraction < 0) {
			nMoves = 0;
		}
		else if (fraction > 1) {
			nMoves = 1;
		}
		else {
			nMoves = (int) Math.ceil( fraction * movesList.size() );
		}
	}

	@Override
	public Collection<Move> generateMoves() {
		if (nMoves == moves.size()) {
			return moves;
		}
		else {
			List<Move> possibleMoves = new ArrayList<Move>( moves );
			List<Move> out = new ArrayList<Move>();

			while (out.size() != nMoves) {
				out.add( possibleMoves.remove( random.nextInt( possibleMoves.size() ) ) );
			}

			return out;
		}
	}
}

