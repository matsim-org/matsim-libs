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

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SamplingStage<U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final Vector alphas;

	private final double equilibriumGapWeight;

	private final double uniformityWeight;

	private final double equilibriumGap;

	private final double originalObjectiveFunctionValue;

	private final double surrogateObjectiveFunctionValue;

	private final Map<U, Double> decisionVariable2alphaSum;

	private final U lastDecisionVariable;

	private final double lastObjectiveFunctionValue;

	private final double lastEquilibriumGap;

	private final Double convergedObjectiveFunctionValue;

	// -------------------- CONSTRUCTION --------------------

	public SamplingStage(final Vector alphas,
			final TransitionSequencesAnalyzer<U> evaluator,
			final Transition<U> lastTransition,
			final Double convergedObjectiveFunctionValue) {

		this.alphas = alphas.copy();
		this.equilibriumGapWeight = evaluator.getEquilibriumGapWeight();
		this.uniformityWeight = evaluator.getUniformityWeight();

		this.equilibriumGap = evaluator.equilibriumGap(alphas);
		this.originalObjectiveFunctionValue = evaluator
				.originalObjectiveFunctionValue(alphas);
		this.surrogateObjectiveFunctionValue = evaluator
				.surrogateObjectiveFunctionValue(alphas);
		this.decisionVariable2alphaSum = evaluator
				.decisionVariable2alphaSum(alphas);

		this.lastDecisionVariable = lastTransition.getDecisionVariable();
		this.lastObjectiveFunctionValue = lastTransition
				.getToStateObjectiveFunctionValue();
		this.lastEquilibriumGap = lastTransition.getDelta().euclNorm();

		this.convergedObjectiveFunctionValue = convergedObjectiveFunctionValue;
	}

	// -------------------- CONTENT ACCESS --------------------

	U drawDecisionVariable(final Random rnd) {
		return MathHelpers.draw(this.decisionVariable2alphaSum, rnd);
	}

	public double getAlphaSquareNorm() {
		return this.alphas.innerProd(this.alphas);
	}

	public double getAlphaNorm() {
		return this.alphas.euclNorm();
	}

	public double getEquilibriumGapWeight() {
		return this.equilibriumGapWeight;
	}

	public double getUniformityGapWeight() {
		return this.uniformityWeight;
	}

	public double getEquilibriumGap() {
		return this.equilibriumGap;
	}

	public double getSurrogateObjectiveFunctionValue() {
		return this.surrogateObjectiveFunctionValue;
	}

	public double getOriginalObjectiveFunctionValue() {
		return this.originalObjectiveFunctionValue;
	}

	public double getAlphaSum(final DecisionVariable decisionVariable) {
		return this.decisionVariable2alphaSum.get(decisionVariable);
	}

	// TODO NEW; replace by view on the entire map
	public Set<U> getDecisionVariables() {
		return Collections.unmodifiableSet(this.decisionVariable2alphaSum
				.keySet());
	}

	// TODO NEW
	public U getLastDecisionVariable() {
		return this.lastDecisionVariable;
	}

	// TODO NEW
	public double getLastObjectiveFunctionValue() {
		return this.lastObjectiveFunctionValue;
	}

	// TODO NEW
	public double getLastEquilibriumGap() {
		return this.lastEquilibriumGap;
	}

	// TODO NEW
	public Double getConvergedObjectiveFunctionValue() {
		return this.convergedObjectiveFunctionValue;
	}
}
