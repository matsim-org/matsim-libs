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
package floetteroed.opdyts.trajectorysampling;

import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.VectorBasedObjectiveFunction;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class SurrogateObjectiveFunction<U extends DecisionVariable> implements VectorBasedObjectiveFunction {

	// -------------------- MEMBERS --------------------

	// private final ObjectBasedObjectiveFunction
	// originalObjectBasedObjectiveFunction;

	private final VectorBasedObjectiveFunction originalVectorBasedObjectiveFunction;

	private final List<Transition<U>> transitions;

	private final Matrix deltaCovariances;

	private final double equilibriumGapWeight;

	private final double uniformityWeight;

	private final double initialGradientNorm;

	// -------------------- CONSTRUCTION --------------------

	SurrogateObjectiveFunction(
			// final ObjectBasedObjectiveFunction
			// originalObjectBasedObjectiveFunction,
			final VectorBasedObjectiveFunction originalVectorBasedObjectiveFunction,
			final List<Transition<U>> transitions,
			final double equilibriumGapWeight, final double uniformityWeight,
			final double initialGradientNorm) {

		// this.originalObjectBasedObjectiveFunction =
		// originalObjectBasedObjectiveFunction;
		this.originalVectorBasedObjectiveFunction = originalVectorBasedObjectiveFunction;
		this.transitions = transitions;
		this.equilibriumGapWeight = equilibriumGapWeight;
		this.uniformityWeight = uniformityWeight;
		this.initialGradientNorm = initialGradientNorm;

		this.deltaCovariances = new Matrix(transitions.size(),
				transitions.size());
		for (int i = 0; i < transitions.size(); i++) {
			for (int j = 0; j <= i; j++) {
				final double val = transitions.get(i).getDelta()
						.innerProd(transitions.get(j).getDelta());
				deltaCovariances.getRow(i).set(j, val);
				deltaCovariances.getRow(j).set(i, val);
			}
		}
	}

	// -------------------- INTERNALS --------------------

	private Vector interpolatedSimulatorState(final Vector alphas) {
		Vector result = this.transitions.get(0).getToState().copy();
		result.mult(alphas.get(0));
		for (int i = 1; i < alphas.size(); i++) {
			result.add(this.transitions.get(i).getToState(), alphas.get(i));
		}
		return result;
	}

	double originalObjectiveFunctionValue(final Vector alphas) {
		if (this.originalVectorBasedObjectiveFunction != null) {
			return this.originalVectorBasedObjectiveFunction.value(this
					.interpolatedSimulatorState(alphas));
		} else {
			double result = 0;
			for (int i = 0; i < alphas.size(); i++) {
				result += alphas.get(i)
						* this.transitions.get(i)
								.getToStateObjectiveFunctionValue();
			}
			return result;
		}
	}

	double equilibriumGap(final Vector alphas) {
		double result = 0;
		for (int i = 0; i < alphas.size(); i++) {
			result += alphas.get(i) * alphas.get(i)
					* this.deltaCovariances.get(i, i);
			for (int j = 0; j < i; j++) {
				result += 2.0 * alphas.get(i) * alphas.get(j)
						* this.deltaCovariances.get(i, j);
			}
		}
		return Math.sqrt(Math.max(result, 0.0));
	}

	// ObjectBasedObjectiveFunction originalObjectBasedObjectiveFunction() {
	// return this.originalObjectBasedObjectiveFunction;
	// }

	// VectorBasedObjectiveFunction originalVectorBasedObjectiveFunction() {
	// return this.originalVectorBasedObjectiveFunction;
	// }

	double getEquilibriumGapWeight() {
		return this.equilibriumGapWeight;
	}

	double getUniformityWeight() {
		return this.uniformityWeight;
	}

	// double getInitialGradientNorm() {
	// return this.initialGradientNorm;
	// }

	// --------------------IMPLEMENTATION OF ObjectiveFunction ----------

	@Override
	public double value(final Vector alphas) {
		return this.originalObjectiveFunctionValue(alphas)
				+ this.equilibriumGapWeight * this.initialGradientNorm
				* this.equilibriumGap(alphas) + this.uniformityWeight
				* this.initialGradientNorm * alphas.innerProd(alphas);
	}

	@Override
	public Vector gradient(final Vector alphas) {
		final Vector result = new Vector(alphas.size());
		/*
		 * objective function value contribution
		 */
		if (this.originalVectorBasedObjectiveFunction != null) {
			final Vector interpolatedState = this
					.interpolatedSimulatorState(alphas);
			final Vector originalGradient = this.originalVectorBasedObjectiveFunction
					.gradient(interpolatedState);
			for (int i = 0; i < alphas.size(); i++) {
				result.set(i, originalGradient.innerProd(this.transitions
						.get(i).getToState()));
			}
		} else {
			for (int i = 0; i < alphas.size(); i++) {
				// result.set(i, this.originalObjectiveFunction
				// .value(this.transitions.get(i).getToState()));
				result.set(i, this.transitions.get(i)
						.getToStateObjectiveFunctionValue());
			}
		}
		/*
		 * equilibrium gap contribution
		 */
		final double equilibriumGap = this.equilibriumGap(alphas);
		for (int l = 0; l < alphas.size(); l++) {
			result.add(l, this.equilibriumGapWeight * this.initialGradientNorm
					* alphas.innerProd(this.deltaCovariances.getRow(l))
					/ (equilibriumGap + 1e-8));
		}
		/*
		 * uniformity contribution, assuming alpha vector to have norm 1
		 */
		for (int l = 0; l < alphas.size(); l++) {
			result.add(l, this.uniformityWeight * this.initialGradientNorm
					*  2.0 * alphas.get(l));
		}
		return result;
	}
}
