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
public class UpperBoundTuner2 {

	// -------------------- CONSTANTS --------------------

	// -------------------- MEMBERS --------------------

	private double equilGapSum = 0.0;

	private double unifGapSum = 0.0;

	private final List<LinearConstraint> constraints = new ArrayList<>();

	double equilGapWeight = 0.0;

	double unifGapWeight = 0.0;

	// -------------------- CONSTRUCTION --------------------

	public UpperBoundTuner2() {
		this.constraints.add(new LinearConstraint(new double[] { 1.0, 0.0 },
				GEQ, 0.0));
		this.constraints.add(new LinearConstraint(new double[] { 0.0, 1.0 },
				GEQ, 0.0));
	}

	// -------------------- OPTIMIZATION --------------------

	public <U extends DecisionVariable> void registerSamplingStageSequence(
			final List<SamplingStage<U>> samplingStages,
			final double finalObjectiveFunctionValue) {

		final SamplingStage<U> lastStage = samplingStages.get(samplingStages
				.size() - 1);

		/*
		 * Objective function update.
		 */
		 this.equilGapSum += lastStage.getEquilibriumGap();
		 this.unifGapSum += lastStage.getUniformityGap();
		 
		/*
		 * Constraint.
		 */
		this.constraints.add(new LinearConstraint(
				new double[] { lastStage.getEquilibriumGap(),
						lastStage.getUniformityGap() }, GEQ,
				finalObjectiveFunctionValue
						- lastStage.getOriginalObjectiveFunctionValue()));

		/*
		 * Run optimization.
		 */
		final LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(
				new double[] { this.equilGapSum, this.unifGapSum }, 0.0);
		final LinearConstraintSet allConstraints = new LinearConstraintSet(
				this.constraints);
		final PointValuePair result = new SimplexSolver().optimize(
				objectiveFunction, allConstraints);
		this.equilGapWeight = result.getPoint()[0];
		this.unifGapWeight = result.getPoint()[1];
	}
}
