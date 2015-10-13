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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.ObjectBasedObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.VectorBasedObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;
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

	private final VectorBasedObjectiveFunction vectorBasedObjectiveFunction;

	private final ObjectBasedObjectiveFunction objectBasedObjectiveFunction;

	private final ConvergenceCriterion convergenceCriterion;

	private final Random rnd;

	final double equilibriumWeight;

	final double uniformityWeight;

	// further program control parameters

	private int maxMemoryLength = Integer.MAX_VALUE;

	private final StatisticsMultiWriter<SamplingStage<U>> statisticsWriter = new StatisticsMultiWriter<>();

	// runtime variables

	private boolean initialized = false;

	private final Map<U, TransitionSequence<U>> decisionVariable2transitionSequence = new LinkedHashMap<>();

	private SimulatorState fromState = null;

	private U currentDecisionVariable = null;

	private int totalTransitionCnt = 0;

	private Map<U, Double> decisionVariable2finalObjectiveFunctionValue = new LinkedHashMap<>();

	private final List<SamplingStage<U>> samplingStages = new ArrayList<>();

	private Double initialGradientNorm = null;

	// -------------------- CONSTRUCTION --------------------

	public ParallelTrajectorySampler(final Set<? extends U> decisionVariables,
			final VectorBasedObjectiveFunction vectorBasedObjectiveFunction,
			final ConvergenceCriterion convergenceCriterion, final Random rnd,
			final double equilibriumWeight, final double uniformityWeight) {
		this.decisionVariablesToBeTriedOut = new LinkedHashSet<U>(
				decisionVariables);
		this.vectorBasedObjectiveFunction = vectorBasedObjectiveFunction;
		this.objectBasedObjectiveFunction = null;
		this.convergenceCriterion = convergenceCriterion;
		this.rnd = rnd;
		this.equilibriumWeight = equilibriumWeight;
		this.uniformityWeight = uniformityWeight;
	}

	public ParallelTrajectorySampler(final Set<? extends U> decisionVariables,
			final ObjectBasedObjectiveFunction objectBasedObjectiveFunction,
			final ConvergenceCriterion convergenceCriterion, final Random rnd,
			final double equilibriumWeight, final double uniformityWeight) {
		this.decisionVariablesToBeTriedOut = new LinkedHashSet<U>(
				decisionVariables);
		this.vectorBasedObjectiveFunction = null;
		this.objectBasedObjectiveFunction = objectBasedObjectiveFunction;
		this.convergenceCriterion = convergenceCriterion;
		this.rnd = rnd;
		this.equilibriumWeight = equilibriumWeight;
		this.uniformityWeight = uniformityWeight;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setMaxMemoryLength(final int maxMemoryLength) {
		this.maxMemoryLength = maxMemoryLength;
	}

	public int getMaxMemoryLength() {
		return this.maxMemoryLength;
	}

	public void addStatistic(final String logFileName,
			final Statistic<SamplingStage<U>> statistic) {
		this.statisticsWriter.addStatistic(logFileName, statistic);
	}

	@Override
	public U getCurrentDecisionVariable() {
		return this.currentDecisionVariable;
	}

	public int getTotalTransitionCnt() {
		return this.totalTransitionCnt;
	}

	public Set<U> getConvergedDecisionVariables() {
		return new LinkedHashSet<U>(
				this.decisionVariable2finalObjectiveFunctionValue.keySet());
	}

	public Double getFinalObjectiveFunctionValue(final U decisionVariable) {
		return this.decisionVariable2finalObjectiveFunctionValue
				.get(decisionVariable);
	}

	@Override
	public Map<U, Double> getDecisionVariable2finalObjectiveFunctionValue() {
		return Collections
				.unmodifiableMap(this.decisionVariable2finalObjectiveFunctionValue);
	}

	@Override
	public boolean foundSolution() {
		return (this.decisionVariable2finalObjectiveFunctionValue.size() > 0);
	}

	public List<SamplingStage<U>> getSamplingStages() {
		return this.samplingStages;
	}

	public Double getInitialGradientNorm() {
		return this.initialGradientNorm;
	}

	// -------------------- IMPLEMENTATION --------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * floetteroed.opdyts.trajectorysampling.TrajectorySamplerInterface#initialize
	 * ()
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see floetteroed.opdyts.trajectorysampling.TrajectorySamplerInterface#
	 * afterIteration(floetteroed.opdyts.SimulatorState)
	 */
	public void afterIteration(final SimulatorState newState) {

		this.totalTransitionCnt++;

		/*
		 * If the from-state is null then one has just observed the first
		 * simulator transition after initialization; not much can be learned
		 * from that.
		 * 
		 * If the from-state is not null, a full transition has been observed
		 * that can now be processed.
		 */
		if (this.fromState != null) {

			/*
			 * Memorize the most recently observed transition.
			 */
			TransitionSequence<U> currentTransitionSequence = this.decisionVariable2transitionSequence
					.get(this.currentDecisionVariable);
			if (currentTransitionSequence == null) {
				currentTransitionSequence = new TransitionSequence<>(
						this.fromState, this.currentDecisionVariable, newState,
						this.objectBasedObjectiveFunction,
						this.vectorBasedObjectiveFunction);
				this.decisionVariable2transitionSequence
						.put(this.currentDecisionVariable,
								currentTransitionSequence);
			} else {
				currentTransitionSequence.addTransition(this.fromState,
						this.currentDecisionVariable, newState,
						this.objectBasedObjectiveFunction,
						this.vectorBasedObjectiveFunction);
			}
			currentTransitionSequence
					.shrinkToMaximumLength(this.maxMemoryLength);

			/*
			 * Check for convergence.
			 */
			this.convergenceCriterion.evaluate(currentTransitionSequence);
			if (this.convergenceCriterion.isConverged()) {
				this.decisionVariable2finalObjectiveFunctionValue.put(
						this.currentDecisionVariable, this.convergenceCriterion
								.getFinalObjectiveFunctionValue());
			}
		}

		/*
		 * Prepare the next iteration.
		 */
		if (this.decisionVariablesToBeTriedOut.size() > 0) {

			/*
			 * There still are untried decision variables, pick one.
			 */
			this.currentDecisionVariable = MathHelpers.drawAndRemove(
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

		} else {

			/*
			 * Estimate the current average gradient norm once and as soon as
			 * all decision variables have been tried out once.
			 */
			if (this.initialGradientNorm == null) {
				if (this.vectorBasedObjectiveFunction != null) {
					this.initialGradientNorm = 0.0;
					for (TransitionSequence<U> transSeq : this.decisionVariable2transitionSequence
							.values()) {
						this.initialGradientNorm += this.vectorBasedObjectiveFunction
								.gradient(
										transSeq.getLastState()
												.getReferenceToVectorRepresentation())
								.euclNorm();
					}
					this.initialGradientNorm /= this.decisionVariable2transitionSequence
							.size();
				} else {
					this.initialGradientNorm = 1.0;
				}
			}

			/*
			 * Create the next sampling stage.
			 */

			// final SamplingStage samplingStage = this
			// .newFullInterpolationSamplingStrategy().nextSamplingStage(
			// this.decisionVariable2transitionSequence);
			final TransitionSequencesAnalyzer<U> samplingStageEvaluator = new TransitionSequencesAnalyzer<U>(
					decisionVariable2transitionSequence,
					this.equilibriumWeight, this.uniformityWeight,
					// this.objectBasedObjectiveFunction,
					this.vectorBasedObjectiveFunction, this.initialGradientNorm);
			final Vector alphas = samplingStageEvaluator.optimalAlphas();
			final SamplingStage<U> samplingStage = new SamplingStage<>(alphas,
					samplingStageEvaluator);

			this.statisticsWriter.writeToFile(samplingStage);

			this.samplingStages.add(samplingStage);
			this.currentDecisionVariable = samplingStage
					.drawDecisionVariable(this.rnd);
			this.fromState = this.decisionVariable2transitionSequence.get(
					this.currentDecisionVariable).getLastState();
			this.fromState.implementInSimulation();
			this.currentDecisionVariable.implementInSimulation();

		}
	}

	// -------------------- TODO NEW: FACTORIES --------------------
	//
	// public TransitionSequencesAnalyzer newTransitionSequenceAnalyzer() {
	// return new TransitionSequencesAnalyzer(
	// TransitionSequencesAnalyzer
	// .map2list(this.decisionVariable2transitionSequence),
	// this.equilibriumWeight, this.uniformityWeight,
	// this.objectiveFunction, this.initialGradientNorm);
	// }
	//
	// public SamplingStrategy newFullInterpolationSamplingStrategy() {
	// return new FullInterpolationSamplingStrategy(this.equilibriumWeight,
	// this.uniformityWeight, this.objectiveFunction,
	// this.initialGradientNorm);
	// }

}
