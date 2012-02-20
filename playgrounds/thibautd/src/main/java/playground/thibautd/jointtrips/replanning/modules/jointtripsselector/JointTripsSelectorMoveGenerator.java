/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsSelectorMoveGenerator.java
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.MoveGenerator;

/**
 * @author thibautd
 */
public class JointTripsSelectorMoveGenerator implements MoveGenerator {
	private final List<Move> moves;

	public JointTripsSelectorMoveGenerator(final int size) {
		List<Move> moves = new ArrayList<Move>();

		for (int i=0; i < size; i++) {
			moves.add( new MutateOneBoolean( i ) );
		}

		this.moves = Collections.unmodifiableList( moves );
	}

	@Override
	public Collection<Move> generateMoves() {
		return moves;
	}
}

