package org.matsim.contrib.opdyts.utils;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import org.matsim.contrib.opdyts.MATSimSimulator2;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MATSimConfiguredFactories<U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final OpdytsConfigGroup opdytsConfig;

	// -------------------- CONSTRUCTION --------------------

	public MATSimConfiguredFactories(final OpdytsConfigGroup opdytsConfig) {
		this.opdytsConfig = opdytsConfig;
	}

	public MATSimConfiguredFactories(final Config config) {
		this((OpdytsConfigGroup) config.getModules().get(OpdytsConfigGroup.GROUP_NAME));
	}

	// -------------------- IMPLEMENTATION --------------------

	public FixedIterationNumberConvergenceCriterion newFixedIterationNumberConvergenceCriterion() {
		return new FixedIterationNumberConvergenceCriterion(this.opdytsConfig.getNumberOfIterationsForConvergence(),
				this.opdytsConfig.getNumberOfIterationsForAveraging());
	}

	public TimeDiscretization newTimeDiscretization() {
		return new TimeDiscretization(this.opdytsConfig.getStartTime(), this.opdytsConfig.getBinSize(),
				this.opdytsConfig.getBinCount());
	}

	public SelfTuner newSelfTuner() {
		final SelfTuner result = new SelfTuner(this.opdytsConfig.getInertia());
		result.setNoisySystem(this.opdytsConfig.isNoisySystem());
		result.setWeightScale(this.opdytsConfig.getSelfTuningWeight());
		return result;
	}

	public RandomSearch<U> newRandomSearch(final MATSimSimulator2<U> matsim,
			final DecisionVariableRandomizer<U> randomizer, final U initialDecisionVariable,
			final ConvergenceCriterion convergenceCriterion, final ObjectiveFunction objectiveFunction) {
		final RandomSearch<U> result = new RandomSearch<U>(matsim, randomizer, initialDecisionVariable,
				convergenceCriterion, this.opdytsConfig.getMaxIteration(), // maxSearchIterations,
				this.opdytsConfig.getMaxTransition(), // Integer.MAX_VALUE,
				this.opdytsConfig.getPopulationSize(), // numberOfTrialDecisionVariables,
				MatsimRandom.getRandom(), this.opdytsConfig.isInterpolate(), objectiveFunction,
				this.opdytsConfig.isIncludeCurrentBest(), this.opdytsConfig.getWarmUpIterations());
		result.setLogPath(this.opdytsConfig.getOutputDirectory());
		result.setMaxTotalMemory(Integer.MAX_VALUE);
		return result;
	}

}
