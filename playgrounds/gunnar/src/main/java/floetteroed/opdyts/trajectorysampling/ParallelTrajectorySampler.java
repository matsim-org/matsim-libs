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

import static floetteroed.utilities.math.MathHelpers.drawAndRemove;
import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterionResult;
import floetteroed.opdyts.logging.ConvergedObjectiveFunctionValue;
import floetteroed.opdyts.logging.EquilibriumGap;
import floetteroed.opdyts.logging.EquilibriumGapWeight;
import floetteroed.opdyts.logging.FreeMemory;
import floetteroed.opdyts.logging.LastDecisionVariable;
import floetteroed.opdyts.logging.LastEquilibriumGap;
import floetteroed.opdyts.logging.LastObjectiveFunctionValue;
import floetteroed.opdyts.logging.MaxMemory;
import floetteroed.opdyts.logging.SurrogateObjectiveFunctionValue;
import floetteroed.opdyts.logging.TotalMemory;
import floetteroed.opdyts.logging.UniformityGap;
import floetteroed.opdyts.logging.UniformityGapWeight;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.statisticslogging.Statistic;
import floetteroed.utilities.statisticslogging.StatisticsMultiWriter;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ParallelTrajectorySampler<U extends DecisionVariable> implements
		TrajectorySampler<U> {

	// -------------------- MEMBERS --------------------

	// set during construction

	private final Set<U> decisionVariablesToBeTriedOut;

	private final ObjectiveFunction objectiveFunction;

	private final ConvergenceCriterion convergenceCriterion;

	private final Random rnd;

	private final double equilibriumWeight;

	private final double uniformityWeight;

	// further program control parameters

	private final StatisticsMultiWriter<SamplingStage<U>> statisticsWriter;

	// runtime variables

	private int totalTransitionCnt = 0;

	private boolean initialized = false;

	private TransitionSequenceSet<U> allTransitionSequences;

	private SimulatorState fromState = null;

	private U currentDecisionVariable = null;

	private Map<U, ConvergenceCriterionResult> decisionVariable2convergenceResult = new LinkedHashMap<>();

	private final List<SamplingStage<U>> samplingStages = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public ParallelTrajectorySampler(final Set<? extends U> decisionVariables,
			final ObjectiveFunction objectBasedObjectiveFunction,
			final ConvergenceCriterion convergenceCriterion, final Random rnd,
			final double equilibriumWeight, final double uniformityWeight,
			final boolean appendToLogFile, final int maxTotalMemory,
			final int maxMemoryPerTrajectory,
			final boolean maintainAllTrajectories) {
		this.decisionVariablesToBeTriedOut = new LinkedHashSet<U>(
				decisionVariables);
		this.objectiveFunction = objectBasedObjectiveFunction;
		this.convergenceCriterion = convergenceCriterion;
		this.rnd = rnd;
		this.equilibriumWeight = equilibriumWeight;
		this.uniformityWeight = uniformityWeight;
		this.statisticsWriter = new StatisticsMultiWriter<>(appendToLogFile);
		this.allTransitionSequences = new TransitionSequenceSet<U>(
				maxTotalMemory, maxMemoryPerTrajectory, maintainAllTrajectories);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	@Override
	public void addStatistic(final String logFileName,
			final Statistic<SamplingStage<U>> statistic) {
		this.statisticsWriter.addStatistic(logFileName, statistic);
	}

	@Override
	public void setStandardLogFileName(final String logFileName) {
		this.addStatistic(logFileName, new SurrogateObjectiveFunctionValue<U>());
		this.addStatistic(logFileName, new LastObjectiveFunctionValue<U>());
		this.addStatistic(logFileName, new ConvergedObjectiveFunctionValue<U>());
		this.addStatistic(logFileName, new EquilibriumGapWeight<U>());
		this.addStatistic(logFileName, new EquilibriumGap<U>());
		this.addStatistic(logFileName, new LastEquilibriumGap<U>());
		this.addStatistic(logFileName, new UniformityGapWeight<U>());
		this.addStatistic(logFileName, new UniformityGap<U>());
		this.addStatistic(logFileName, new TotalMemory<U>());
		this.addStatistic(logFileName, new FreeMemory<U>());
		this.addStatistic(logFileName, new MaxMemory<U>());
		this.addStatistic(logFileName, new LastDecisionVariable<U>());
	}

	@Override
	public U getCurrentDecisionVariable() {
		return this.currentDecisionVariable;
	}

	public int getTotalTransitionCnt() {
		return this.totalTransitionCnt;
		// return this.allTransitionSequences.size();
	}

	@Override
	public Map<U, ConvergenceCriterionResult> getDecisionVariable2convergenceResultView() {
		return unmodifiableMap(this.decisionVariable2convergenceResult);
	}

	@Override
	public boolean foundSolution() {
		return (this.decisionVariable2convergenceResult.size() > 0);
	}

	public List<SamplingStage<U>> getSamplingStages() {
		return this.samplingStages;
	}

	public List<Transition<U>> getTransitions() {
		return this.allTransitionSequences.getAllTransitionsInInsertionOrder();
	}

	@Override
	public ObjectiveFunction getObjectiveFunction() {
		return this.objectiveFunction;
	}

	public List<Transition<U>> getTransitions(final U decisionVariable) {
		return this.allTransitionSequences.getTransitions(decisionVariable);
	}

	public int additionCnt(final U decisionVariable) {
		return this.allTransitionSequences.additionCnt(decisionVariable);
	}

	public StatisticsMultiWriter<SamplingStage<U>> getStatisticsWriter() {
		return this.statisticsWriter;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void initialize() {
		if (this.initialized) {
			throw new RuntimeException("Cannot re-initialize an instance of "
					+ this.getClass().getSimpleName()
					+ ". Create a new instance instead.");
		} else {
			this.initialized = true;
		}
		this.currentDecisionVariable = MathHelpers.draw(
				this.decisionVariablesToBeTriedOut, this.rnd);
		this.currentDecisionVariable.implementInSimulation();
	}

	public void afterIteration(final SimulatorState newState) {

		this.totalTransitionCnt++;

		Logger.getLogger(this.getClass().getName()).info(
				"Trajectory sampling iteration " + this.samplingStages.size());

		/*
		 * If the from-state is null then one has just observed the first
		 * simulator transition after initialization; not much can be learned
		 * from that.
		 * 
		 * If the from-state is not null, a full transition has been observed
		 * that can now be processed.
		 */
		TransitionSequencesAnalyzer<U> samplingStageEvaluator = null;
		SamplingStage<U> samplingStage = null;

		if (this.fromState != null) {

			/*
			 * Memorize the most recently observed transition.
			 */
			this.allTransitionSequences.addTransition(this.fromState,
					this.currentDecisionVariable, newState,
					this.objectiveFunction.value(newState));

			/*
			 * Check for convergence.
			 */
			final ConvergenceCriterionResult convergenceResult = this.convergenceCriterion
					.evaluate(this.allTransitionSequences
							.getTransitions(this.currentDecisionVariable),
							this.allTransitionSequences
									.additionCnt(this.currentDecisionVariable));
			if (convergenceResult.converged) {
				samplingStageEvaluator = new TransitionSequencesAnalyzer<U>(
						this.allTransitionSequences
								.getAllTransitionsInInsertionOrder(),
						this.equilibriumWeight, this.uniformityWeight);
				samplingStage = samplingStageEvaluator.newOptimalSamplingStage(
						this.allTransitionSequences.getTransitions(
								this.currentDecisionVariable).getLast(),
						convergenceResult.finalObjectiveFunctionValue,
						this.samplingStages == null ? null
								: this.samplingStages.get(
										samplingStages.size() - 1)
										.transition2lastSolutionView());
				this.samplingStages.add(samplingStage);
				this.decisionVariable2convergenceResult.put(
						this.currentDecisionVariable, convergenceResult);
			}
		}

		/*
		 * Prepare the next iteration.
		 */
		if (this.decisionVariablesToBeTriedOut.size() > 0) {

			/*
			 * There still are untried decision variables, pick one.
			 */
			this.currentDecisionVariable = drawAndRemove(
					this.decisionVariablesToBeTriedOut, this.rnd);

			/*
			 * All untried decision variables are evaluated starting from the
			 * same state. This initial state is the first state ever registered
			 * here.
			 */
			if (this.fromState == null) {
				this.fromState = newState;
			} else {
				this.fromState.implementInSimulation();
			}
			this.currentDecisionVariable.implementInSimulation();
			this.statisticsWriter.writeToFile(null, EquilibriumGapWeight.LABEL,
					Double.toString(this.equilibriumWeight),
					UniformityGapWeight.LABEL,
					Double.toString(this.uniformityWeight));

		} else {

			/*
			 * Create the next sampling stage.
			 */

			if (samplingStageEvaluator == null) {
				samplingStageEvaluator = new TransitionSequencesAnalyzer<U>(
						this.allTransitionSequences
								.getAllTransitionsInInsertionOrder(),
						this.equilibriumWeight, this.uniformityWeight);
				samplingStage = samplingStageEvaluator.newOptimalSamplingStage(
						this.allTransitionSequences.getTransitions(
								this.currentDecisionVariable).getLast(),
						null,
						this.samplingStages.size() == 0 ? null
								: this.samplingStages.get(
										samplingStages.size() - 1)
										.transition2lastSolutionView());
				this.samplingStages.add(samplingStage);
			}

			this.statisticsWriter.writeToFile(samplingStage);

			/*
			 * Decide what decision variable to use in the next iteration; set
			 * the simulation to the last state visited by the corresponding
			 * sampling trajectory.
			 */
			this.currentDecisionVariable = samplingStage
					.drawDecisionVariable(this.rnd);
			this.fromState = this.allTransitionSequences
					.getLastState(this.currentDecisionVariable);
			this.fromState.implementInSimulation();
			this.currentDecisionVariable.implementInSimulation();

		}
	}
}
