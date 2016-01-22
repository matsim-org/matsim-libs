/*
 * Opdyts - Optimization of dynamic traffic simulations
 *
 * Copyright 2015 Gunnar Flötteröd
 * 
 *
 * This file is part of Opdyts.
 *
 * Opdyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opdyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Opdyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.opdyts.convergencecriteria;

import static java.lang.Math.abs;

import java.util.List;

import floetteroed.opdyts.trajectorysampling.TransitionSequence;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ObjectiveFunctionChangeConvergenceCriterion implements
		ConvergenceCriterion {

	// -------------------- CONSTANTS --------------------

	private final double absoluteChange;

	private final double relativeChange;

	private final int minAverageIterations;

	// -------------------- MEMBERS --------------------

	private Double finalObjectiveFunctionValue = null;

	public ObjectiveFunctionChangeConvergenceCriterion(
			final double absoluteChange, final double relativeChange,
			final int minAverageIterations) {
		this.absoluteChange = absoluteChange;
		this.relativeChange = relativeChange;
		this.minAverageIterations = minAverageIterations;
	}

	// --------------- IMPLEMENTATION OF ConvergenceCriterion ---------------

	@Override
	public void evaluate(final TransitionSequence<?> transitionSequence) {
		if (transitionSequence.size() < 2 * this.minAverageIterations) {
			this.finalObjectiveFunctionValue = null;
		} else {
			for (int averageIterations = this.minAverageIterations; averageIterations <= transitionSequence
					.size() / 2; averageIterations++) {
				final double mean1 = new Vector(transitionSequence
						.getObjectiveFunctionValues().subList(
								transitionSequence.size() - 2
										* averageIterations,
								transitionSequence.size() - averageIterations))
						.mean();
				final double mean2 = new Vector(transitionSequence
						.getObjectiveFunctionValues().subList(
								transitionSequence.size() - averageIterations,
								transitionSequence.size())).mean();
				if ((abs(mean1 - mean2) <= this.absoluteChange)
						|| (abs(mean1 - mean2) <= this.relativeChange
								* abs(mean1))) {
					this.finalObjectiveFunctionValue = mean2;
					return;
				}
			}
			this.finalObjectiveFunctionValue = null;
		}
	}

	@Override
	public boolean isConverged() {
		return (this.finalObjectiveFunctionValue != null);
	}

	@Override
	public Double getFinalObjectiveFunctionValue() {
		return this.finalObjectiveFunctionValue;
	}

	@Override
	public void reset() {
		this.finalObjectiveFunctionValue = null;
	}

	@Override
	public List<Double> getFinalWeights() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double getFinalEquilbriumGap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double getFinalUniformityGap() {
		throw new UnsupportedOperationException();
	}
	
}
