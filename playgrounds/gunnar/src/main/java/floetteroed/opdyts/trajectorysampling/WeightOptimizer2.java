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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class WeightOptimizer2 {

	// -------------------- MEMBERS --------------------

	private double equilibriumGapWeight;

	private double uniformityGapWeight;

	// -------------------- CONSTRUCTION --------------------

	public WeightOptimizer2(final double initialEquilibriumGapWeight,
			final double initialUniformityGapWeight) {
		this.equilibriumGapWeight = initialEquilibriumGapWeight;
		this.uniformityGapWeight = initialUniformityGapWeight;
	}

	// -------------------- IMPLEMENTATION --------------------

	public <U extends DecisionVariable> double[] updateWeights(
			final List<Transition<U>> transitions, final double qStar) {

		final TransitionSequencesAnalyzer<U> analyzer = new TransitionSequencesAnalyzer<>(
				transitions, this.equilibriumGapWeight,
				this.uniformityGapWeight);
		final Vector alphas = analyzer.optimalAlphas();
		final double qBar = analyzer.originalObjectiveFunctionValue(alphas);
	
		final double equilibriumGap = analyzer.equilibriumGap(alphas);
		final double uniformityGap = alphas.innerProd(alphas);
//		System.out.println(equilibriumGap + "\t" + uniformityGap + "\t" + qBar
//				+ "\t" + analyzer.surrogateObjectiveFunctionValue(alphas));
		System.out.println(analyzer.surrogateObjectiveFunctionValue(alphas) + "\t"
				+ transitions.get(transitions.size() - 1).getToStateObjectiveFunctionValue());

		/*
		 * Q([dv dw v w])
		 * 
		 * =
		 * 
		 * [eg ug 0 0]' [dv dw v w]
		 * 
		 * +
		 * 
		 * [0 0 M*eg M*ug]' [dv dw v w]
		 */
		// final LinearObjectiveFunction objFct = new LinearObjectiveFunction(
		// new double[] { equilibriumGap, uniformityGap, equilibriumGap,
		// uniformityGap }, 0);
		final LinearObjectiveFunction objFct = new LinearObjectiveFunction(
				new double[] { 1, 1, 0, 0 }, 0);

		final List<LinearConstraint> constraints = new ArrayList<>(7);

		/*
		 * v, w >= 0
		 */
		constraints.add(new LinearConstraint(new double[] { 0, 0, 1, 0 },
				Relationship.GEQ, 0));
		constraints.add(new LinearConstraint(new double[] { 0, 0, 0, 1 },
				Relationship.GEQ, 0));

		/*
		 * dv = |v - v0|
		 * 
		 * dv >= +(v - v0) <=> dv - v >= -v0
		 * 
		 * dv >= -(v - v0) <=> dv + v >= +v0
		 */
		constraints.add(new LinearConstraint(new double[] { 1, 0, -1, 0 },
				Relationship.GEQ, -this.equilibriumGapWeight));
		constraints.add(new LinearConstraint(new double[] { 1, 0, +1, 0 },
				Relationship.GEQ, +this.equilibriumGapWeight));

		/*
		 * dw = |w - w0|
		 * 
		 * dw >= +(w - w0) <=> dw - w >= -w0
		 * 
		 * dw >= -(w - w0) <=> dw + w >= +w0
		 */
		constraints.add(new LinearConstraint(new double[] { 0, 1, 0, -1 },
				Relationship.GEQ, -this.uniformityGapWeight));
		constraints.add(new LinearConstraint(new double[] { 0, 1, 0, +1 },
				Relationship.GEQ, +this.uniformityGapWeight));

		/*
		 * Q_bar + v eg + w ug = Q_star
		 * 
		 * v eg + w ug = Q_star - Q_bar
		 */
		constraints
				.add(new LinearConstraint(new double[] { 0, 0, equilibriumGap,
						uniformityGap }, Relationship.GEQ, qStar - qBar));

		/*
		 * And, finally ...
		 */

		try {
			final PointValuePair result = (new SimplexSolver()).optimize(
					objFct, new LinearConstraintSet(constraints));
			this.equilibriumGapWeight = result.getPoint()[0];
			this.uniformityGapWeight = result.getPoint()[1];
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).info(e.getMessage());
			try {
				System.in.read();
			} catch (IOException e2) {
				e2.printStackTrace();
			}

		}

		return new double[] { this.equilibriumGapWeight,
				this.uniformityGapWeight };
	}
}
