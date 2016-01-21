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
package floetteroed.opdyts.searchalgorithms;

import static org.apache.commons.math3.optim.linear.Relationship.GEQ;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.SimplexSolver;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class UpperBoundTuner {

	// -------------------- CONSTANTS --------------------

	// -------------------- MEMBERS --------------------

	// -------------------- CONSTRUCTION --------------------

	public UpperBoundTuner() {

	}

	// -------------------- GETTERS --------------------

	double equilGapWeight = 0.0;

	double unifGapWeight = 0.0;

	// -------------------- OPTIMIZATION --------------------

	public <U extends DecisionVariable> void registerSamplingStageSequence(
			final List<SamplingStage<U>> samplingStages,
			final double finalObjectiveFunctionValue) {

		/*
		 * Objective function.
		 */
		double avgEquilGap = 0;
		double avgUnifGap = 0;
		for (SamplingStage<U> stage : samplingStages) {
			avgEquilGap += stage.getEquilibriumGap();
			avgUnifGap += stage.getUniformityGap();
		}
		// {
		// SamplingStage<U> stage = samplingStages.get(0);
		// avgEquilGap += stage.getEquilibriumGap();
		// avgUnifGap += stage.getAlphaSquareNorm();
		// }
		avgEquilGap /= samplingStages.size();
		avgUnifGap /= samplingStages.size();
		final LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(
				new double[] { avgEquilGap, avgUnifGap }, 0.0);

		/*
		 * Constraints.
		 */
		final List<LinearConstraint> constraints = new ArrayList<>(
				2 + samplingStages.size());
		constraints.add(new LinearConstraint(new double[] { 1.0, 0.0 }, GEQ,
				0.0));
		constraints.add(new LinearConstraint(new double[] { 0.0, 1.0 }, GEQ,
				0.0));
		for (SamplingStage<U> stage : samplingStages) {
			constraints.add(new LinearConstraint(new double[] {
					stage.getEquilibriumGap(), stage.getUniformityGap() },
					GEQ, finalObjectiveFunctionValue
							- stage.getOriginalObjectiveFunctionValue()));
		}
		// {
		// // final SamplingStage<U> stage = samplingStages.get(0);
		// final SamplingStage<U> stage = samplingStages.get(samplingStages
		// .size() - 1);
		// constraints.add(new LinearConstraint(new double[] {
		// stage.getEquilibriumGap(), stage.getAlphaSquareNorm() },
		// GEQ, finalObjectiveFunctionValue
		// - stage.getOriginalObjectiveFunctionValue()));
		// }
		final LinearConstraintSet allConstraints = new LinearConstraintSet(
				constraints);

		/*
		 * Run optimization.
		 */
		final SimplexSolver solver = new SimplexSolver();
		final PointValuePair result = solver.optimize(objectiveFunction,
				allConstraints);

		this.equilGapWeight = result.getPoint()[0];
		this.unifGapWeight = result.getPoint()[1];

		// System.out.println("ONE-SHOT-OPTIMIZATION: v = " +
		// result.getPoint()[0]
		// + " w = " + result.getPoint()[1]);
	}
}
