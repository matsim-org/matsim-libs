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
package vind;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.log4j.Logger;

import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class WeightOptimizer3 {

	private double equilGapWeight;

	private double unifGapWeight;

	private int its = 0;

	// -------------------- CONSTRUCTION --------------------

	public WeightOptimizer3(final double initialEquilGapWeight,
			final double initialUnifGapWeight) {
		this.equilGapWeight = initialEquilGapWeight;
		this.unifGapWeight = initialUnifGapWeight;
	}

	// -------------------- IMPLEMENTATION --------------------

	public double[] updateWeights(
			final double equilGapWeight,
			final double unifGapWeight,
			final SamplingStage<?> lastSamplingStage,
			final double finalObjFctValue,
			final double finalEquilGap,
			final double finalUnifGap,
//			final SurrogateObjectiveFunction<?> finalSurrogateObjectiveFunction,
			final Vector finalAlphas) {
		return this.updateWeights(equilGapWeight, unifGapWeight,
				lastSamplingStage.getEquilibriumGap(),
				lastSamplingStage.getUniformityGap(), finalObjFctValue,
				lastSamplingStage.getSurrogateObjectiveFunctionValue(),
				finalEquilGap, finalUnifGap, 
//				finalSurrogateObjectiveFunction,
				finalAlphas);
	}

	private double[] updateWeights(
			final double equilGapWeight,
			final double unifGapWeight,
			final double equilGap,
			final double unifGap,
			final double finalObjFctValue,
			final double finalSurrogateObjectiveFunctionValue,
			final double finalEquilGap,
			final double finalUnifGap,
//			final SurrogateObjectiveFunction<?> finalSurrogateObjectiveFunction,
			final Vector finalAlphas) {

		/*
		 * Decision variables in the linear program are [r s v w].
		 * 
		 * Create objective function.
		 * 
		 * Q([r s v w]) = r + s = [1 0 0 0]' [r s v w];
		 */
		final LinearObjectiveFunction objFct = new LinearObjectiveFunction(
				new double[] { 1, 1, 0, 0 }, 0.0);

		final List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		/*
		 * r = |eg(v-v0)|.
		 * 
		 * r >= +eg(v-v0) <=> [1 0 -eg 0]' [r s v w] >= -eg v0
		 * 
		 * r >= -eg(v-v0) <=> [1 0 +eg 0]' [r s v w] >= +eg v0
		 */
//		constraints.add(new LinearConstraint(new double[] { 1, 0,
//				-finalEquilGap, 0 }, Relationship.GEQ, -finalEquilGap
//				* this.equilGapWeight));
//		constraints.add(new LinearConstraint(new double[] { 1, 0,
//				+finalEquilGap, 0 }, Relationship.GEQ, +finalEquilGap
//				* this.equilGapWeight));
		 constraints.add(new LinearConstraint(new double[] { 1, 0, -1, 0 },
		 Relationship.GEQ, -1 * this.equilGapWeight));
		 constraints.add(new LinearConstraint(new double[] { 1, 0, +1, 0 },
		 Relationship.GEQ, +1 * this.equilGapWeight));
		/*
		 * s = |ug(w-w0)|.
		 * 
		 * s >= +ug(w-w0) <=> [0 1 0 -ug]' [r s v w] >= -ug w0
		 * 
		 * s >= -ug(w-w0) <=> [0 1 0 +ug]' [r s v w] >= +ug w0
		 */
//		constraints.add(new LinearConstraint(new double[] { 0, 1, 0,
//				-finalUnifGap }, Relationship.GEQ, -finalUnifGap
//				* this.unifGapWeight));
//		constraints.add(new LinearConstraint(new double[] { 0, 1, 0,
//				+finalUnifGap }, Relationship.GEQ, +finalUnifGap
//				* this.unifGapWeight));
		 constraints.add(new LinearConstraint(new double[] { 0, 1, 0, -1 },
		 Relationship.GEQ, -1 * this.unifGapWeight));
		 constraints.add(new LinearConstraint(new double[] { 0, 1, 0, +1 },
		 Relationship.GEQ, +1 * this.unifGapWeight));
		/*
		 * v >= 0, w >= 0.
		 */
		constraints.add(new LinearConstraint(new double[] { 0, 0, 1, 0 },
				Relationship.GEQ, 0.0));
		constraints.add(new LinearConstraint(new double[] { 0, 0, 0, 1 },
				Relationship.GEQ, 0.0));
		/*
		 * Qsurr0 + eg(v-v0) + ug(w-w0) = Q*
		 * 
		 * <=> [0 0 eg ug]' [r s v w] = Q* - Qsurr0 + eg v0 + ug w0
		 */
		constraints.add(new LinearConstraint(new double[] { 0, 0,
				finalEquilGap, finalUnifGap }, Relationship.EQ,
				finalObjFctValue - finalSurrogateObjectiveFunctionValue
						+ finalEquilGap * this.equilGapWeight + finalUnifGap
						* this.unifGapWeight));

		/*
		 * Solve the linear program and take over the result.
		 */
		this.its++;
		try {
			final PointValuePair result = (new SimplexSolver()).optimize(
					objFct, new LinearConstraintSet(constraints));
			final double updateWeight = 1.0; // 1.0 / this.its;
			this.equilGapWeight = (1.0 - updateWeight) * this.equilGapWeight
					+ updateWeight * result.getPoint()[2];
			this.unifGapWeight = (1.0 - updateWeight) * this.unifGapWeight
					+ updateWeight * result.getPoint()[3];
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).warn(e.getMessage());
		}
		return new double[] { this.equilGapWeight, this.unifGapWeight };
	}
}
