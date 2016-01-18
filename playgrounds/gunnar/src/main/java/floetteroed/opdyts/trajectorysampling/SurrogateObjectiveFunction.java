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
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class SurrogateObjectiveFunction<U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final List<Transition<U>> transitions;

	private final Matrix deltaCovariances;

	private final double equilibriumGapWeight;

	private final double uniformityWeight;

	// -------------------- CONSTRUCTION --------------------

	SurrogateObjectiveFunction(final List<Transition<U>> transitions,
			final double equilibriumGapWeight, final double uniformityWeight) {
		this.transitions = transitions;
		this.equilibriumGapWeight = equilibriumGapWeight;
		this.uniformityWeight = uniformityWeight;

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

	double originalObjectiveFunctionValue(final Vector alphas) {
		double result = 0;
		for (int i = 0; i < alphas.size(); i++) {
			result += alphas.get(i)
					* this.transitions.get(i)
							.getToStateObjectiveFunctionValue();
		}
		return result;
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

	double getEquilibriumGapWeight() {
		return this.equilibriumGapWeight;
	}

	double getUniformityWeight() {
		return this.uniformityWeight;
	}

	// --------------------IMPLEMENTATION OF ObjectiveFunction ----------

	// @Override
	public double value(final Vector alphas) {
		return this.originalObjectiveFunctionValue(alphas)
				+ this.equilibriumGapWeight * this.equilibriumGap(alphas)
				+ this.uniformityWeight * alphas.innerProd(alphas);
	}

	// TODO NEW; recycle in gradient(Vector)
	Vector dQdAlpha(final Vector alphas) {
		final Vector result = new Vector(alphas.size());
		for (int i = 0; i < alphas.size(); i++) {
			result.set(i, this.transitions.get(i)
					.getToStateObjectiveFunctionValue());
		}
		return result;
	}

	// TODO NEW; recycle in gradient(Vector); note that this is not weighted
	Vector dEquilibriumGapdAlpha(final Vector alphas) {
		final Vector result = new Vector(alphas.size());
		final double equilibriumGap = this.equilibriumGap(alphas);
		for (int l = 0; l < alphas.size(); l++) {
			result.set(l, alphas.innerProd(this.deltaCovariances.getRow(l))
					/ (equilibriumGap + 1e-8));
		}
		return result;
	}

	// TODO NEW; recycle in gradient(Vector); note that this is not weighted
	Vector dUniformityGapdAlpha(final Vector alphas) {
		final Vector result = new Vector(alphas.size());
		for (int l = 0; l < alphas.size(); l++) {
			result.set(l, 2.0 * alphas.get(l));
		}
		return result;
	}

	// TODO NEW
	Matrix d2EquilibriumGapdAlpha2(final Vector alphas) {
		final Matrix result = this.deltaCovariances.copy();		
		final Vector gradient = this.dEquilibriumGapdAlpha(alphas);
		result.addOuterProduct(gradient, gradient, -1.0);
		result.mult(1.0 / this.equilibriumGap(alphas));
		return result;
	}
	
	// TODO NEW
	Matrix d2UniformityGapdAlpha2(final Vector alphas) {
		return Matrix.newDiagonal(alphas.size(), 2.0);
	}
	
	// @Override
	public Vector gradient(final Vector alphas) {
		final Vector result = new Vector(alphas.size());
		/*
		 * objective function value contribution
		 */
		for (int i = 0; i < alphas.size(); i++) {
			result.set(i, this.transitions.get(i)
					.getToStateObjectiveFunctionValue());
		}
		/*
		 * equilibrium gap contribution
		 */
		final double equilibriumGap = this.equilibriumGap(alphas);
		for (int l = 0; l < alphas.size(); l++) {
			result.add(
					l,
					this.equilibriumGapWeight
							* alphas.innerProd(this.deltaCovariances.getRow(l))
							/ (equilibriumGap + 1e-8));
		}
		/*
		 * uniformity contribution, assuming alpha vector to have norm 1
		 */
		for (int l = 0; l < alphas.size(); l++) {
			result.add(l, this.uniformityWeight * 2.0 * alphas.get(l));
		}
		return result;
	}
}
