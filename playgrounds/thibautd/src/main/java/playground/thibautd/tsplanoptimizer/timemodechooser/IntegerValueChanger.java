/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeDoubleValue.java
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
import playground.thibautd.tsplanoptimizer.framework.TabuSearchRunner;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * A {@link Move} which changes a given int value by a given amount.
 * @author thibautd
 */
public class IntegerValueChanger implements Move {
	private static final boolean DURATION_ENCODING = true;
	private final int index;
	private final int amount;
	private final ModeOptimizingMoveConfigBuilder configBuilder;

	/**
	 * @param index the index of the representation value to change
	 * @param amount the amount to add
	 */
	public IntegerValueChanger(
			final int index,
			final int amount,
			final ModeOptimizingMoveConfigBuilder configBuilder) {
		this.index = index;
		this.amount = amount;
		this.configBuilder = configBuilder;
	}

	/**
	 * @throws ClassCastException if the value at the specified index is not a
	 * integer {@link Value}
	 */
	@Override
	public Solution apply(final Solution solution) {
		Solution newSolution = solution.createClone();

		Value<Integer> val = (Value<Integer>) newSolution.getRepresentation().get( index );
		val.setValue( val.getValue().intValue() + amount );

		if (DURATION_ENCODING) {
			// emulate a duration-based encoding by modifying all subsequent end times.
			for (int i=index+1; i < newSolution.getRepresentation().size(); i++) {
				val = (Value<Integer>) newSolution.getRepresentation().get( index );
				val.setValue( val.getValue().intValue() + amount );
			}
		}

		if (configBuilder != null) {
			configBuilder.setSolution( newSolution );
			newSolution = TabuSearchRunner.runTabuSearch( configBuilder );
		}

		return newSolution;
	}

	@Override
	public Move getReverseMove() {
		if (configBuilder == null) {
			return new IntegerValueChanger( index , -amount , configBuilder );
		}
		// if optimizing move, not reversible
		throw new UnsupportedOperationException();
	}

	public int getIndex() {
		return index;
	}

	public int getAmount() {
		return amount;
	}
}

