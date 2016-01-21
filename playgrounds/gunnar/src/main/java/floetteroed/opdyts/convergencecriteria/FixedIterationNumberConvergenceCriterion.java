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

import java.util.ArrayList;
import java.util.List;

import floetteroed.opdyts.trajectorysampling.TransitionSequence;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FixedIterationNumberConvergenceCriterion implements
		ConvergenceCriterion {

	// -------------------- CONSTANTS --------------------

	private final int iterationsToConvergence;

	private final int averagingIterations;

	private List<Double> finalWeights = null;

	private Double finalEquilbiriumGap = null;

	private Double finalUniformityGap = null;

	// -------------------- MEMBERS --------------------

	private Double finalObjectiveFunctionValue = null;

	// -------------------- CONSTRUCTION --------------------

	public FixedIterationNumberConvergenceCriterion(
			final int iterationsToConvergence, final int averagingIterations) {
		this.iterationsToConvergence = iterationsToConvergence;
		this.averagingIterations = averagingIterations;
	}

	// --------------- IMPLEMENTATION OF ConvergenceCriterion ---------------

	@Override
	public void evaluate(final TransitionSequence<?> transitionSequence) {
		if ((transitionSequence.iterations() < this.iterationsToConvergence)) {
			this.finalObjectiveFunctionValue = null;
		} else {
			final List<Double> objectiveFunctionValues = transitionSequence
					.getObjectiveFunctionValues();
			this.finalObjectiveFunctionValue = 0.0;
			for (int i = transitionSequence.size() - this.averagingIterations; i < transitionSequence
					.size(); i++) {
				this.finalObjectiveFunctionValue += objectiveFunctionValues
						.get(i);
			}
			this.finalObjectiveFunctionValue /= this.averagingIterations;

			// TODO >>> NEW >>>
			this.finalWeights = new ArrayList<>(transitionSequence.size());
			for (int i = 0; i < transitionSequence.size()
					- this.averagingIterations; i++) {
				this.finalWeights.add(0.0);
			}
			for (int i = transitionSequence.size() - this.averagingIterations; i < transitionSequence
					.size(); i++) {
				this.finalWeights.add(1.0 / this.averagingIterations);
			}

			// TODO >>> EVEN NEWER >>>
			
			final Vector totalDelta = transitionSequence.getTransitions()
					.get(transitionSequence.size() - this.averagingIterations)
					.getDelta().copy();
			for (int i = transitionSequence.size() - this.averagingIterations
					+ 1; i < transitionSequence.size(); i++) {
				totalDelta.add(transitionSequence.getTransitions().get(i)
						.getDelta());
			}
			final double finalWeight = 1.0 / this.averagingIterations;
			totalDelta.mult(finalWeight);
			this.finalEquilbiriumGap = totalDelta.euclNorm();
			this.finalUniformityGap = finalWeight;
			
			// TODO <<< NEW <<<
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
		return this.finalWeights;
	}

	@Override
	public Double getFinalEquilbriumGap() {
		return this.finalEquilbiriumGap;
	}

	@Override
	public Double getFinalUniformityGap() {
		return this.finalUniformityGap;
	}
}
