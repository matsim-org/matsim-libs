/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package opdytsintegration.utils;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Created by amit on 03.06.17.
 */

public class OpdytsConfigGroup extends ReflectiveConfigGroup {

	public OpdytsConfigGroup() {
		super(GROUP_NAME);
	}

	public static final String GROUP_NAME = "opdyts";

	private static final String VARIATION_SIZE_OF_RANDOMIZE_DECISION_VARIABLE = "variationSizeOfRandomizeDecisionVariable";
	private double variationSizeOfRandomizeDecisionVariable = 0.1;

	private static final String RANDOM_SEED_TO_RANDOMIZE_DECISION_VARIABLE = "randomSeedToRandomizeDecisionVariable";
	private int randomSeedToRandomizeDecisionVariable = 4711;

	private static final String OUTPUT_DIRECTORY = "outputDirectory";
	private String outputDirectory = "./output/";

	// search algorithm
	private static final String MAX_ITERATION = "maxIteration";
	private int maxIteration = 10;

	private static final String MAX_TRANSITION = "maxTransition";
	private int maxTransition = Integer.MAX_VALUE;

	private static final String POPULATION_SIZE = "populationSize";
	private int populationSize = 10;

	private static final String IS_INTERPOLATE = "interpolate";
	private boolean interpolate = true;

	private static final String INCLUDE_CURRENT_BEST = "includeCurrentBest";
	private boolean includeCurrentBest = false;

	// time discretization
	private static final String START_TIME = "startTime";
	private int startTime = 0;

	private static final String BIN_SIZE = "binSize";
	private int binSize = 3600;

	private static final String BIN_COUNT = "binCount";
	private int binCount = 24;

	// convergence criteria
	private static final String NUMBER_OF_ITERATION_TO_AVERAGE = "numberOfIterationsForAveraging";
	private int numberOfIterationsForAveraging = 20;

	private static final String NUMER_OF_ITERATION_FOR_CONVERGENCE = "numberOfIterationsForConvergence";
	private int numberOfIterationsForConvergence = 600;

	// self tuner
	private static final String INERTIA = "inertia";
	private double inertia = 0.95;

	private static final String NOISY_SYSTEM = "noisySystem";
	private boolean noisySystem = true;

	private static final String SELF_TUNING_WEIGHT = "selfTuningWeight";
	private double selfTuningWeight = 1.0;

	private static final String EQUILIBRIUM_GAP_WEIGHT = "equilibriumGapWeight";
	private double equilibriumGapWeight = 0.;

	private static final String UNIFORMITY_GAP_WEIGHT = "uniformityGapWeight";
	private double uniformityGapWeight = 0.;


	@StringGetter(VARIATION_SIZE_OF_RANDOMIZE_DECISION_VARIABLE)
	public double getVariationSizeOfRandomizeDecisionVariable() {
		return variationSizeOfRandomizeDecisionVariable;
	}

	@StringSetter(VARIATION_SIZE_OF_RANDOMIZE_DECISION_VARIABLE)
	public void setVariationSizeOfRandomizeDecisionVariable(double variationSizeOfRandomizeDecisionVariable) {
		this.variationSizeOfRandomizeDecisionVariable = variationSizeOfRandomizeDecisionVariable;
	}

	@StringGetter(RANDOM_SEED_TO_RANDOMIZE_DECISION_VARIABLE)
	public int getRandomSeedToRandomizeDecisionVariable() {
		return this.randomSeedToRandomizeDecisionVariable;
	}

	@StringSetter(RANDOM_SEED_TO_RANDOMIZE_DECISION_VARIABLE)
	public void setRandomSeedToRandomizeDecisionVariable(int randomSeedToRandomizeDecisionVariable) {
		this.randomSeedToRandomizeDecisionVariable = randomSeedToRandomizeDecisionVariable;
	}

	@StringGetter(OUTPUT_DIRECTORY)
	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	@StringSetter(OUTPUT_DIRECTORY)
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@StringGetter(MAX_ITERATION)
	public int getMaxIteration() {
		return this.maxIteration;
	}

	@StringSetter(MAX_ITERATION)
	public void setMaxIteration(int maxIteration) {
		this.maxIteration = maxIteration;
	}

	@StringGetter(MAX_TRANSITION)
	public int getMaxTransition() {
		return this.maxTransition;
	}

	@StringSetter(MAX_TRANSITION)
	public void setMaxTransition(int maxTransition) {
		this.maxTransition = maxTransition;
	}

	@StringGetter(POPULATION_SIZE)
	public int getPopulationSize() {
		return this.populationSize;
	}

	@StringSetter(POPULATION_SIZE)
	public void setPopulationSize(int populationSize) {
		this.populationSize = populationSize;
	}

	@StringGetter(IS_INTERPOLATE)
	public boolean isInterpolate() {
		return interpolate;
	}

	@StringSetter(IS_INTERPOLATE)
	public void setInterpolate(boolean interpolate) {
		this.interpolate = interpolate;
	}

	@StringGetter(INCLUDE_CURRENT_BEST)
	public boolean isIncludeCurrentBest() {
		return includeCurrentBest;
	}

	@StringSetter(INCLUDE_CURRENT_BEST)
	public void setIncludeCurrentBest(boolean includeCurrentBest) {
		this.includeCurrentBest = includeCurrentBest;
	}

	@StringGetter(START_TIME)
	public int getStartTime() {
		return this.startTime;
	}

	@StringSetter(START_TIME)
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	@StringGetter(BIN_SIZE)
	public int getBinSize() {
		return this.binSize;
	}

	@StringSetter(BIN_SIZE)
	public void setBinSize(int binSize) {
		this.binSize = binSize;
	}

	@StringGetter(BIN_COUNT)
	public int getBinCount() {
		return this.binCount;
	}

	@StringSetter(BIN_COUNT)
	public void setBinCount(int binCount) {
		this.binCount = binCount;
	}

	@StringGetter(NUMBER_OF_ITERATION_TO_AVERAGE)
	public int getNumberOfIterationsForAveraging() {
		return numberOfIterationsForAveraging;
	}

	@StringSetter(NUMBER_OF_ITERATION_TO_AVERAGE)
	public void setNumberOfIterationsForAveraging(int numberOfIterationsForAveraging) {
		this.numberOfIterationsForAveraging = numberOfIterationsForAveraging;
	}

	@StringGetter(NUMER_OF_ITERATION_FOR_CONVERGENCE)
	public int getNumberOfIterationsForConvergence() {
		return numberOfIterationsForConvergence;
	}

	@StringSetter(NUMER_OF_ITERATION_FOR_CONVERGENCE)
	public void setNumberOfIterationsForConvergence(int numberOfIterationsForConvergence) {
		this.numberOfIterationsForConvergence = numberOfIterationsForConvergence;
	}

	@StringGetter(INERTIA)
	public double getInertia() {
		return inertia;
	}

	@StringSetter(INERTIA)
	public void setInertia(double inertia) {
		this.inertia = inertia;
	}

	@StringGetter(NOISY_SYSTEM)
	public boolean isNoisySystem() {
		return noisySystem;
	}

	@StringSetter(NOISY_SYSTEM)
	public void setNoisySystem(boolean noisySystem) {
		this.noisySystem = noisySystem;
	}

	@StringGetter(SELF_TUNING_WEIGHT)
	public double getSelfTuningWeight() {
		return this.selfTuningWeight;
	}

	@StringSetter(SELF_TUNING_WEIGHT)
	public void setSelfTuningWeight(double selfTuningWeight) {
		this.selfTuningWeight = selfTuningWeight;
	}

	@StringGetter(EQUILIBRIUM_GAP_WEIGHT)
	public double getEquilibriumGapWeight() {
		return equilibriumGapWeight;
	}

	@StringSetter(EQUILIBRIUM_GAP_WEIGHT)
	public void setEquilibriumGapWeight(double equilibriumGapWeight) {
		this.equilibriumGapWeight = equilibriumGapWeight;
	}

	@StringGetter(UNIFORMITY_GAP_WEIGHT)
	public double getUniformityGapWeight() {
		return uniformityGapWeight;
	}

	@StringSetter(UNIFORMITY_GAP_WEIGHT)
	public void setUniformityGapWeight(double uniformityGapWeight) {
		this.uniformityGapWeight = uniformityGapWeight;
	}

	// MAIN-FUNCTION, ONLY FOR TESTING

	public static void main(String[] args) {

		// pretend that we load the config group from file

		Config config = ConfigUtils.createConfig();
		config.addModule(new OpdytsConfigGroup());

		// use the config

		OpdytsConfigGroup optConf = (OpdytsConfigGroup) config.getModules().get(OpdytsConfigGroup.GROUP_NAME);

		// alternative to get configGroup from configUtils, amit June'17
//		OpdytsConfigGroup optConf = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.GROUP_NAME,OpdytsConfigGroup.class);
		
	}
}