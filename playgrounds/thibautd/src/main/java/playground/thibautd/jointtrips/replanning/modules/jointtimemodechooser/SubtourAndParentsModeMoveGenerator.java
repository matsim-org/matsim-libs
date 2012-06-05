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
package playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser.JointTimeModeChooserSolution.SubtourValue;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.MoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * @author thibautd
 */
public class SubtourAndParentsModeMoveGenerator implements MoveGenerator {
	private final List<Move> moves;

	public SubtourAndParentsModeMoveGenerator(
			final Solution initialSolution,
			final Collection<String> modes) {
		List<Move> moves = new ArrayList<Move>();

		int i=0;
		for (Value value : initialSolution.getRepresentation()) {
			if (value instanceof SubtourValue) {
				for (String mode : modes) {
					moves.add( new SubtourAndParentsModeMove( i , mode ) );
				}
			}
			i++;
		}

		this.moves = Collections.unmodifiableList( moves );
	}

	@Override
	public Collection<Move> generateMoves() {
		return moves;
	}
}

