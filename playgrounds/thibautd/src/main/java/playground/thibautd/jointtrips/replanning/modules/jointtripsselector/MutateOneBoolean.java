/* *********************************************************************** *
 * project: org.matsim.*
 * MutateOneBoolean.java
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

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * Changes the boolean value at a given index.
 * @author thibautd
 */
public class MutateOneBoolean implements Move {
	private final int index;

	/**
	 * @param index the index of the value to mute. It must be a boolean value!
	 */
	public MutateOneBoolean(final int index) {
		this.index = index;
	}

	@Override
	public Solution apply(final Solution solution) {
		Solution newSolution = solution.createClone();

		Value<Boolean> value = (Value<Boolean>)
			newSolution.getRepresentation().get( index );
		boolean newValue = !value.getValue();
		value.setValue( newValue );

		return newSolution;
	}

	@Override
	public Move getReverseMove() {
		return this;
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof MutateOneBoolean && ((MutateOneBoolean) other).index == index;
	}

	@Override
	public int hashCode() {
		return index;
	}
}

