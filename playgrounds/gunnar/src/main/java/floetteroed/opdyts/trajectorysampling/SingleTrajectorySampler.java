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

import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SingleTrajectorySampler<U extends DecisionVariable> implements
		TrajectorySampler<U> {

	// -------------------- MEMBERS --------------------

	private final U decisionVariable;

	private final ObjectiveFunction objectiveFunction;

	private final ConvergenceCriterion convergenceCriterion;

	private SimulatorState fromState = null;

	private TransitionSequence<U> transitionSequence = null;

	// -------------------- CONSTRUCTION --------------------

	public SingleTrajectorySampler(final U decisionVariable,
			final ObjectiveFunction objectiveFunction,
			final ConvergenceCriterion convergenceCriterion) {
		this.decisionVariable = decisionVariable;
		this.objectiveFunction = objectiveFunction;
		this.convergenceCriterion = convergenceCriterion;
	}

	// --------------- IMPLEMENTATION OF TrajectorySampler ---------------

	@Override
	public ObjectiveFunction getObjectiveFunction() {
		return this.objectiveFunction;
	}

	@Override
	public boolean foundSolution() {
		return this.convergenceCriterion.isConverged();
	}

	@Override
	public Map<U, Double> getDecisionVariable2finalObjectiveFunctionValue() {
		final Map<U, Double> result = new LinkedHashMap<>();
		if (this.convergenceCriterion.isConverged()) {
			result.put(this.decisionVariable,
					this.convergenceCriterion.getFinalObjectiveFunctionValue());
		}
		return result;
	}

	@Override
	public U getCurrentDecisionVariable() {
		return this.decisionVariable;
	}

	@Override
	public void initialize() {
		this.decisionVariable.implementInSimulation();
	}

	@Override
	public void afterIteration(SimulatorState newState) {
		if (this.fromState != null) {
			if (this.transitionSequence == null) {
				this.transitionSequence = new TransitionSequence<U>(
						this.fromState, this.decisionVariable, newState,
						this.objectiveFunction.value(newState));
			} else {
				this.transitionSequence.addTransition(this.fromState,
						this.decisionVariable, newState,
						this.objectiveFunction.value(newState));
			}
			this.convergenceCriterion.evaluate(this.transitionSequence);
		}
		this.fromState = newState;
	}

	@Override
	public int getTotalTransitionCnt() {
		if (this.transitionSequence != null) {
			return this.transitionSequence.size();
		} else {
			return 0;
		}
	}

	@Override
	public void addStatistic(final String logFileName,
			final Statistic<SamplingStage<U>> statistic) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setStandardLogFileName(final String logFileName) {
		throw new UnsupportedOperationException();
	}

}
