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

import java.util.List;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.jgap.InvalidConfigurationException;

import org.matsim.api.core.v01.network.Network;
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
	private static final long serialVersionUID = 1L;

	// to avoid negative fitness errors
	private static final double START_FITNESS = 1E9;
	// parameters of the fitness
	private static final double CHF_PER_MICROSEC = 10;
	private static final double CHF_PER_NANOSEC = CHF_PER_MICROSEC * 1E-3;

	// indices of the genes in the chromosome:
	private static final int CHROM_LENGTH = 7;
	private static final int POP_SIZE_GENE = 0;
	private static final int MUT_PROB_GENE = 1;
	private static final int WHOLE_PROB_GENE = 2;
	private static final int SIMPLE_PROB_GENE = 3;
	private static final int SINGLE_PROB_GENE = 4;
	private static final int DISCRETE_SCALE_GENE = 5;
	private static final int RTS_WINDOW_GENE = 6;

	// bounds
	private static final int MAX_POP_SIZE = 200;
	private static final double MAX_DISCRETE_SCALE = 1E7;
	private static final int MAX_WINDOW_SIZE = 20;

	// instance fields
	private final List<JointPlan> plans;
	private final int nPlans;
	private JPOForOptimization jpoAlgo;
	private final Configuration jgapConfig;

	public ParameterOptimizerFitness(
			final Configuration jgapConfig,
			final List<JointPlan> plans,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final String iterationOutputPath
			) {
		this.jgapConfig = jgapConfig;
		this.plans = plans;
		this.nPlans = plans.size();
		this.jpoAlgo = new JPOForOptimization(
				scoringFunctionFactory, legTravelTimeEstimatorFactory, routingAlgorithm,
				network, iterationOutputPath);
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
		double[] scores = new double[nPlans];
		long[] cpuTimesNanoSecs = new long[nPlans];
		JointReplanningConfigGroup configGroup = fromChromosomeToConfig(chromosome);
		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
		long startTime;
		long endTime;

		for (int i=0; i < nPlans; i++) {
			startTime = thread.getCurrentThreadCpuTime();
			scores[i] = this.jpoAlgo.run(configGroup, this.plans.get(i));
			endTime = thread.getCurrentThreadCpuTime();
			cpuTimesNanoSecs[i] = endTime - startTime;
		}

		return getScore(scores, cpuTimesNanoSecs);
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

		genes[POP_SIZE_GENE] = new IntegerGene(this.jgapConfig, 2, MAX_POP_SIZE);
		genes[MUT_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[WHOLE_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[SIMPLE_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[SINGLE_PROB_GENE] = new DoubleGene(this.jgapConfig, 0d, 1d);
		genes[DISCRETE_SCALE_GENE] = new DoubleGene(this.jgapConfig, 0d, MAX_DISCRETE_SCALE);
		genes[RTS_WINDOW_GENE] = new IntegerGene(this.jgapConfig, 2, MAX_WINDOW_SIZE);

		return new Chromosome(this.jgapConfig, genes);
	}

	/**
	 * "Decodes" a chromosome.
	 */
	public final static JointReplanningConfigGroup fromChromosomeToConfig(
			final IChromosome chromosome) {
		JointReplanningConfigGroup configGroup = new JointReplanningConfigGroup();

		Gene[] genes = chromosome.getGenes();

		configGroup.setPopulationSize(getIntValue(genes[POP_SIZE_GENE]));
		configGroup.setMutationProbability(getDoubleValue(genes[MUT_PROB_GENE]));
		configGroup.setWholeCrossOverProbability(getDoubleValue(genes[WHOLE_PROB_GENE]));
		configGroup.setSimpleCrossOverProbability(getDoubleValue(genes[SIMPLE_PROB_GENE]));
		configGroup.setSingleCrossOverProbability(getDoubleValue(genes[SINGLE_PROB_GENE]));
		configGroup.setDiscreteDistanceScale(getDoubleValue(genes[DISCRETE_SCALE_GENE]));
		configGroup.setRtsWindowSize(getIntValue(genes[RTS_WINDOW_GENE]));

		return configGroup;
	}

	private static int getIntValue(final Gene gene) {
		return ((IntegerGene) gene).intValue();
	}

	private static double getDoubleValue(final Gene gene) {
		return ((DoubleGene) gene).doubleValue();
	}
}

