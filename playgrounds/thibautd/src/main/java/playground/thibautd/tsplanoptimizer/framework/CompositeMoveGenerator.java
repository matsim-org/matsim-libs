/* *********************************************************************** *
 * project: org.matsim.*
 * CompositeMoveGenerator.java
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
package playground.thibautd.tsplanoptimizer.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link MoveGenerator} wich generates moves using several sub-routines.
 * @author thibautd
 */
public class CompositeMoveGenerator implements MoveGenerator {
	private final List<MoveGenerator> generators = new ArrayList<MoveGenerator>();

	public void add( final MoveGenerator generator ) {
		generators.add( generator );
	}

	@Override
	public Collection<Move> generateMoves() {
		List<Move> moves = new ArrayList<Move>();

		for (MoveGenerator generator : generators) {
			moves.addAll( generator.generateMoves() );
		}

		return moves;
	}
}

