/* *********************************************************************** *
 * project: org.matsim.*
 * ParameterOptimizerFitness.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.gaparamoptimizer;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.jgap.InvalidConfigurationException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Fitness function for the meta-GA.
 * The fitness is calculated based on the best fitness and the computation time
 * of several runs of the replanning algorithm on different cliques.
 *
 * @author thibautd
 */
public class ParameterOptimizerFitness extends FitnessFunction {
	private static final Logger log =
		Logger.getLogger(ParameterOptimizerFitness.class);

	private static final long serialVersionUID = 1L;

	// to avoid negative fitness errors
	private static final double START_FITNESS = 1E7;
	// parameters of the fitness
	private static final double CHF_PER_MICROSEC = 1E-6;
	private static final double CHF_PER_NANOSEC = CHF_PER_MICROSEC * 1E-3;
	private static final int N_PLAN_EXEC = 1;

	// indices of the genes in the chromosome:
	private static final int CHROM_LENGTH = 13;
	//private static final int POP_SIZE_GENE = 0;
	private static final int POP_INTERCEPT_GENE = 0;
	private static final int MUT_PROB_GENE = 1;
	private static final int WHOLE_PROB_GENE = 2;
	private static final int SIMPLE_PROB_GENE = 3;
	private static final int SINGLE_PROB_GENE = 4;
	private static final int DISCRETE_SCALE_GENE = 5;
	//private static final int RTS_WINDOW_GENE = 6;
	private static final int RTS_WINDOW_INTERCEPT_GENE = 6;
	private static final int NON_UNIFORM_GENE = 7;
	private static final int IN_PLACE_GENE = 8;
	private static final int P_NON_UNIFORM_GENE = 9;
	private static final int POP_SLOPE_GENE = 10;
	private static final int RTS_WINDOW_SLOPE_GENE = 11;
	private static final int HAMMING_GENE = 12;

	// bounds
	//private static final int MAX_POP_SIZE = 100;
	private static final double MAX_POP_INTERCEPT = 20;
	private static final double MIN_POP_SLOPE = 1;
	private static final double MAX_POP_SLOPE = 10;
	private static final double MAX_DISCRETE_SCALE = 1E7;
	//private static final int MAX_WINDOW_SIZE = 20;
	private static final double MIN_WINDOW_SLOPE = 0;
	private static final int MAX_WINDOW_SLOPE = 2;
	private static final int MAX_WINDOW_INTERCEPT = 5;
	private static final double MAX_NON_UNIFORM = 50d;

	// kept for cloning
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final Controler controler;
	private final Network network;
	private final String iterationOutputPath;

	// instance fields
	private final List<JointPlan> plans;
	private final int nPlans;
	private JPOForOptimization jpoAlgo;
	private final Configuration jgapConfig;
	private final int[] cliqueSizes;

	public ParameterOptimizerFitness(
			final Configuration jgapConfig,
			final List<JointPlan> plans,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			// final PlansCalcRoute routingAlgorithm,
			final Controler controler,
			final Network network,
			final String iterationOutputPath
			) {
 		this.scoringFunctionFactory = scoringFunctionFactory;
 		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
 		this.controler = controler;
 		this.network = network;
 		this.iterationOutputPath = iterationOutputPath;

		this.jgapConfig = jgapConfig;
		this.plans = Collections.unmodifiableList(plans);
		this.nPlans = plans.size();
		this.jpoAlgo = new JPOForOptimization(
				scoringFunctionFactory, legTravelTimeEstimatorFactory, 
				(PlansCalcRoute) controler.createRoutingAlgorithm(),
				network, iterationOutputPath);

		this.cliqueSizes = new int[this.nPlans];

		for (int i = 0; i < this.nPlans; i++) {
			this.cliqueSizes[i] = this.plans.get(i).getClique().getMembers().size();
		}

		// log.debug("fitness function initialized");
		// //log.debug("max pop size: "+MAX_POP_SIZE);
		// log.debug("max pop slope: "+MAX_POP_SLOPE);
		// log.debug("max pop intercept: "+MAX_POP_INTERCEPT);
		// log.debug("max discrete scale: "+MAX_DISCRETE_SCALE);
		// //log.debug("max window size: "+MAX_WINDOW_SIZE);
		// log.debug("max window size slope: "+MAX_WINDOW_SLOPE);
		// log.debug("max window size intercept: "+MAX_WINDOW_INTERCEPT);
		// log.debug("max non uniformity: "+MAX_NON_UNIFORM);
		// log.debug("n plan execs: "+N_PLAN_EXEC);
		// log.debug("CHF per microsec: "+CHF_PER_MICROSEC);
	}

	@Override
	public ParameterOptimizerFitness clone() {
		return new ParameterOptimizerFitness(
			jgapConfig,
			plans,
			scoringFunctionFactory,
			legTravelTimeEstimatorFactory,
			controler,
			network,
			iterationOutputPath);
	}

	/**
	 * Scores a parameter set.
	 *
	 * The replanning algorithm is ran on the test instances, and best fitness
	 * and CPU time are measured.
	 *
	 * The fitness is the sum, over all test instance, of F_best - alpha*CPU_time.
	 * alpha can be interpreted as the "acceptable time" to gain one fitness unit.
	 * The fitness is understood as the fitness per clique member.
	 */
	@Override
	protected double evaluate(final IChromosome chromosome) {
		LogInfo info = new LogInfo();
		info.generationNumber = this.jgapConfig.getGenerationNr();
		info.chromValue = toString(chromosome);
		double[] scores = new double[nPlans];
		long[] cpuTimesNanoSecs = new long[nPlans];
		JointReplanningConfigGroup configGroup = fromChromosomeToConfig(chromosome);
		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
		long startTime;
		long endTime;
		double currentScore;
		long currentTime;

		if (!thread.isThreadCpuTimeEnabled()) {
			log.warn("CPU time monitoring had to be enabled by hand");
			thread.setThreadCpuTimeEnabled(true);
		}

		for (int i=0; i < nPlans; i++) {
			scores[i] = 0d;
			cpuTimesNanoSecs[i] = 0L;
			for (int j=0; j < N_PLAN_EXEC; j++) {
				startTime = thread.getCurrentThreadCpuTime();
				JointPlan plan = new JointPlan(this.plans.get(i));
				currentScore = this.jpoAlgo.run(configGroup, plan);
				endTime = thread.getCurrentThreadCpuTime();
				currentTime = (endTime - startTime) / this.cliqueSizes[i];

				scores[i] += currentScore;
				cpuTimesNanoSecs[i] += currentTime;
				info.scores.add(currentScore);
				info.times.add((double) currentTime);
			}
		}

		info.log();

		return getScore(scores, cpuTimesNanoSecs);
	}

	private static class LogInfo {
		public int generationNumber;
		public String chromValue;
		public List<Double> scores = new ArrayList<Double>();
		public List<Double> times  = new ArrayList<Double>();

		private synchronized void log() {
			log.debug("computing fitness: generation #"+generationNumber);
			log.debug("evaluating chromosome with value: "+chromValue);
			for (int i=0; i < scores.size(); i++) {
				log.debug("plan score per member (CHF): "+scores.get(i));
				log.debug("CPU time per member (s): "+(times.get(i)*1E-9));
			}
		}
	}

	private double getScore(
			final double[] fitnesses,
			final long[] cpuTimes) {
		return START_FITNESS + sum(fitnesses) - CHF_PER_NANOSEC * sum(cpuTimes);
	}

	private double sum(final double[] array) {
		double result = 0d;

		for (double d : array) {
			result += d;
		}

		return result;
	}

	private long sum(final long[] array) {
		long result = 0;

		for (long l : array) {
			result += l;
		}

		return result;
	}

	private double average(final double[] scores) {
		double sum = 0;
		int n = scores.length;

		for (int i=0; i < n; i++) {
			sum += scores[i];
		}

		return sum / n;
	}

	/**
	 * @return a chromosome corresponding to the expected format
	 */
	public IChromosome getSampleChromosome() throws InvalidConfigurationException {
		Gene[] genes = new Gene[CHROM_LENGTH];

		//genes[POP_SIZE_GENE] = new IntegerGene(this.jgapConfig, 2, MAX_POP_SIZE);
		genes[POP_SLOPE_GENE] = new DoubleGene(this.jgapConfig, MIN_POP_SLOPE, MAX_POP_SLOPE);
		genes[POP_INTERCEPT_GENE] = new DoubleGene(this.jgapConfig, 2d, MAX_POP_INTERCEPT);
		genes[MUT_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[WHOLE_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[SIMPLE_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[SINGLE_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[DISCRETE_SCALE_GENE] = new DoubleGene(this.jgapConfig, 0d, MAX_DISCRETE_SCALE);
		//genes[RTS_WINDOW_GENE] = new IntegerGene(this.jgapConfig, 2, MAX_WINDOW_SIZE);
		genes[RTS_WINDOW_SLOPE_GENE] = new DoubleGene(this.jgapConfig, MIN_WINDOW_SLOPE, MAX_WINDOW_SLOPE);
		genes[RTS_WINDOW_INTERCEPT_GENE] = new DoubleGene(this.jgapConfig, 2d, MAX_WINDOW_INTERCEPT);
		genes[NON_UNIFORM_GENE] = new DoubleGene(this.jgapConfig, 0d, MAX_NON_UNIFORM);
		genes[IN_PLACE_GENE] = new BooleanGene(this.jgapConfig);
		genes[P_NON_UNIFORM_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[HAMMING_GENE] = new BooleanGene(this.jgapConfig);

		return new Chromosome(this.jgapConfig, genes);
	}

	private String toString(final IChromosome chromosome) {
		Gene[] genes = chromosome.getGenes();
		return
				"[popSizeSlope = "+genes[POP_SLOPE_GENE]
				+"; popSizeIntercept = "+genes[POP_INTERCEPT_GENE]
				+"; mutationProb = "+genes[MUT_PROB_GENE]
				+"; wholeCOprob = "+genes[WHOLE_PROB_GENE]
				+"; simpleCOProb = "+genes[SIMPLE_PROB_GENE]
				+"; singleCOProb = "+genes[SINGLE_PROB_GENE]
				+"; discScale = "+genes[DISCRETE_SCALE_GENE]
				+"; windowSizeSlope = "+genes[RTS_WINDOW_SLOPE_GENE]
				+"; windowSizeIntercept = "+genes[RTS_WINDOW_INTERCEPT_GENE]
				+"; mutationNonUniformity = "+genes[NON_UNIFORM_GENE]
				+"; isMutationInPlace = "+genes[IN_PLACE_GENE]
				+"; nonUniformMutationProb = "+genes[P_NON_UNIFORM_GENE]
				+"; hammingOnly = "+genes[HAMMING_GENE];
	}

	/**
	 * "Decodes" a chromosome.
	 */
	public final static JointReplanningConfigGroup fromChromosomeToConfig(
			final IChromosome chromosome) {
		JointReplanningConfigGroup configGroup = new JointReplanningConfigGroup();

		Gene[] genes = chromosome.getGenes();

		//configGroup.setPopulationSize(getIntValue(genes[POP_SIZE_GENE]));
		configGroup.setPopulationCoef(getDoubleValue(genes[POP_SLOPE_GENE]));
		configGroup.setPopulationIntercept(getDoubleValue(genes[POP_INTERCEPT_GENE]));
		configGroup.setMutationProbability(getDoubleValue(genes[MUT_PROB_GENE]));
		configGroup.setWholeCrossOverProbability(getDoubleValue(genes[WHOLE_PROB_GENE]));
		configGroup.setSimpleCrossOverProbability(getDoubleValue(genes[SIMPLE_PROB_GENE]));
		configGroup.setSingleCrossOverProbability(getDoubleValue(genes[SINGLE_PROB_GENE]));
		configGroup.setDiscreteDistanceScale(getDoubleValue(genes[DISCRETE_SCALE_GENE]));
		//configGroup.setRtsWindowSize(getIntValue(genes[RTS_WINDOW_GENE]));
		configGroup.setWindowSizeCoef(getDoubleValue(genes[RTS_WINDOW_SLOPE_GENE]));
		configGroup.setWindowSizeIntercept(getDoubleValue(genes[RTS_WINDOW_INTERCEPT_GENE]));
		configGroup.setMutationNonUniformity(getDoubleValue(genes[NON_UNIFORM_GENE]));
		configGroup.setInPlaceMutation(getBooleanValue(genes[IN_PLACE_GENE]));
		configGroup.setNonUniformMutationProbability(getDoubleValue(genes[P_NON_UNIFORM_GENE]));
		configGroup.setUseOnlyHammingDistanceInRTS(getBooleanValue(genes[HAMMING_GENE]));

		return configGroup;
	}

	private static int getIntValue(final Gene gene) {
		return ((IntegerGene) gene).intValue();
	}

	private static double getDoubleValue(final Gene gene) {
		return ((DoubleGene) gene).doubleValue();
	}

	private static boolean getBooleanValue(final Gene gene) {
		return ((BooleanGene) gene).booleanValue();
	}
}

