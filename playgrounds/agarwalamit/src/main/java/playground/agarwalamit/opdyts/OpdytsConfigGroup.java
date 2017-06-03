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

package playground.agarwalamit.opdyts;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Created by amit on 03.06.17.
 */

public class OpdytsConfigGroup extends ReflectiveConfigGroup {

   public OpdytsConfigGroup(){
        super(GROUP_NAME);
   }

   public static final String GROUP_NAME = "opdyts";

   private static final String SCALING_PARAMETER_TO_RANDOMIZE_DECISION_VARIABLE = "scalingParameterToRamdomizeDecisionVariable";
   private double scalingParameterToRamdomizeDecisionVariable = 0.1;

   private static final String RANDOM_SEED_TO_RANDOMIZE_DECISION_VARIABLE = "randomSeedToRandomizeDecisionVariable";
   private long randomSeedToRandomizeDecisionVariable = 4711;

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

    public double getScalingParameterToRamdomizeDecisionVariable() {
        return scalingParameterToRamdomizeDecisionVariable;
    }

    public void setScalingParameterToRamdomizeDecisionVariable(double scalingParameterToRamdomizeDecisionVariable) {
        this.scalingParameterToRamdomizeDecisionVariable = scalingParameterToRamdomizeDecisionVariable;
    }

    public long getRandomSeedToRandomizeDecisionVariable() {
        return this.randomSeedToRandomizeDecisionVariable;
    }

    public void setRandomSeedToRandomizeDecisionVariable(long randomSeedToRandomizeDecisionVariable) {
        this.randomSeedToRandomizeDecisionVariable = randomSeedToRandomizeDecisionVariable;
    }

    public String getOutputDirectory() {
        return this.outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public int getMaxIteration() {
        return this.maxIteration;
    }

    public void setMaxIteration(int maxIteration) {
        this.maxIteration = maxIteration;
    }

    public int getMaxTransition() {
        return this.maxTransition;
    }

    public void setMaxTransition(int maxTransition) {
        this.maxTransition = maxTransition;
    }

    public int getPopulationSize() {
        return this.populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    public boolean isIncludeCurrentBest() {
        return includeCurrentBest;
    }

    public void setIncludeCurrentBest(boolean includeCurrentBest) {
        this.includeCurrentBest = includeCurrentBest;
    }

    public int getStartTime() {
        return this.startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getBinSize() {
        return this.binSize;
    }

    public void setBinSize(int binSize) {
        this.binSize = binSize;
    }

    public int getBinCount() {
        return this.binCount;
    }

    public void setBinCount(int binCount) {
        this.binCount = binCount;
    }

    public int getNumberOfIterationsForAveraging() {
        return numberOfIterationsForAveraging;
    }

    public void setNumberOfIterationsForAveraging(int numberOfIterationsForAveraging) {
        this.numberOfIterationsForAveraging = numberOfIterationsForAveraging;
    }

    public int getNumberOfIterationsForConvergence() {
        return numberOfIterationsForConvergence;
    }

    public void setNumberOfIterationsForConvergence(int numberOfIterationsForConvergence) {
        this.numberOfIterationsForConvergence = numberOfIterationsForConvergence;
    }

    public double getInertia() {
        return inertia;
    }

    public void setInertia(double inertia) {
        this.inertia = inertia;
    }

    public boolean isNoisySystem() {
        return noisySystem;
    }

    public void setNoisySystem(boolean noisySystem) {
        this.noisySystem = noisySystem;
    }

    public double getSelfTuningWeight() {
        return this.selfTuningWeight;
    }

    public void setSelfTuningWeight(double selfTuningWeight) {
        this.selfTuningWeight = selfTuningWeight;
    }
}