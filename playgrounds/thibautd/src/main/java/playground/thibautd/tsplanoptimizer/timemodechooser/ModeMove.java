/* *********************************************************************** *
 * project: org.matsim.*
 * ModeMove.java
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

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;

/**
 * @author thibautd
 */
public class ModeMove implements Move {
	private final int index;
	private final String newMode;

	public ModeMove(
			final int index,
			final String newMode) {
		this.index = index;
		this.newMode = newMode;
	}

	@Override
	public <T> Solution<T> apply(final Solution<T> solution) {
		Solution<T> newSolution = solution.createClone();

		newSolution.getGenotype().get( index ).setValue( newMode );

		return newSolution;
	}

	@Override
	public Move getReverseMove() {
		throw new UnsupportedOperationException();
	}

	public int getIndex() {
		return index;
	}

	public String getMode() {
		return newMode;
	}

	@Override
	public String toString() {
		return "ModeMove{index="+index+", mode="+newMode+"}";
	}
}

