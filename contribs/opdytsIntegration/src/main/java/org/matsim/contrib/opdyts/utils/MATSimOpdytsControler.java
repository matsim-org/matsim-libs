package org.matsim.contrib.opdyts.utils;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.MATSimSimulator2;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MATSimOpdytsControler<U extends DecisionVariable> {

	// -------------------- MEMBERS --------------------

	private final OpdytsConfigGroup opdytsConfig;
	private TimeDiscretization timeDiscretization;
	private ConvergenceCriterion convergenceCriterion;
	private RandomSearch<U> randomSearch;

	// -------------------- CONSTRUCTION --------------------

	public MATSimOpdytsControler(final Scenario scenario) {
		this.opdytsConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), OpdytsConfigGroup.class);
		this.timeDiscretization = newTimeDiscretization();
		this.convergenceCriterion = newFixedIterationNumberConvergenceCriterion();
	}

	// -------------------- IMPLEMENTATION --------------------

	FixedIterationNumberConvergenceCriterion newFixedIterationNumberConvergenceCriterion() {
		return new FixedIterationNumberConvergenceCriterion(this.opdytsConfig.getNumberOfIterationsForConvergence(),
				this.opdytsConfig.getNumberOfIterationsForAveraging());
	}

	TimeDiscretization newTimeDiscretization() {
		return new TimeDiscretization(this.opdytsConfig.getStartTime(), this.opdytsConfig.getBinSize(),
				this.opdytsConfig.getBinCount());
	}

	// alternatively, one can set all arguments via setters/builders. Amit July'17
	public void run(final MATSimSimulator2<U> matsim,
					final DecisionVariableRandomizer<U> randomizer,
					final U initialDecisionVariable,
					final ObjectiveFunction objectiveFunction) {

		final RandomSearch<U> result = new RandomSearch<U>(matsim, randomizer, initialDecisionVariable,
				convergenceCriterion, this.opdytsConfig.getMaxIteration(), this.opdytsConfig.getMaxTransition(),
				this.opdytsConfig.getPopulationSize(), objectiveFunction);

		result.setLogPath(this.opdytsConfig.getOutputDirectory());
		result.setMaxTotalMemory(Integer.MAX_VALUE);
		result.setIncludeCurrentBest(this.opdytsConfig.isIncludeCurrentBest());
		result.setRandom(MatsimRandom.getRandom());
		result.setInterpolate(this.opdytsConfig.isInterpolate());
		result.setWarmupIterations(this.opdytsConfig.getWarmUpIterations());
		result.setUseAllWarmupIterations(this.opdytsConfig.getUseAllWarmUpIterations());
		result.setInitialEquilibriumGapWeight(this.opdytsConfig.getEquilibriumGapWeight());
		result.setInitialUniformityGapWeight(this.opdytsConfig.getUniformityGapWeight());

		final SelfTuner selfTuner = new SelfTuner(this.opdytsConfig.getInertia());
		selfTuner.setNoisySystem(this.opdytsConfig.isNoisySystem());
		selfTuner.setWeightScale(this.opdytsConfig.getSelfTuningWeight());
		result.setSelfTuner(selfTuner);

		this.randomSearch = result;

		result.run();
	}

	public RandomSearch<U> getRandomSearch(){
		return this.randomSearch ;
	}

	//optional
	public void setTimeDiscretization(TimeDiscretization timeDiscretization){
		this.timeDiscretization = timeDiscretization;
	}

	public TimeDiscretization getTimeDiscretization() {
		return this.timeDiscretization;
	}

	//optional
	public void setFixedIterationNumberConvergenceCriterion (ConvergenceCriterion convergenceCriterion) {
		this.convergenceCriterion = convergenceCriterion;
	}

	public ConvergenceCriterion getFixedIterationNumberConvergenceCriterion (){
		return this.convergenceCriterion;
	}
}
