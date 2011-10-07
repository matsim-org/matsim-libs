/* *********************************************************************** *
 * project: org.matsim.*
 * DurationSimplexAlgo.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.optimization.direct.DirectSearchOptimizer;
import org.apache.commons.math.optimization.direct.MultiDirectional;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealConvergenceChecker;
import org.apache.commons.math.optimization.RealPointValuePair;

import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPChromosome;

/**
 * "on the fly" duration scorer, which performs an local optimisation algorithm
 * on durations and modifies back the chromosome.
 *
 * Using this scorer transforms the replanning algorithm in a "Memetic Algorithm".
 *
 * @author thibautd
 */
public class DurationSimplexAlgo implements FinalScorer {
	private static final Log log =
		LogFactory.getLog(DurationSimplexAlgo.class);

	private static final int N_MAX_ITERS = 20;
	private static final double MIN_IMPROVEMENT = 1E-5;
	// defines the size of the initial vertex (to be sure of the "locality"
	// of the search!)
	private static final double STEP = 5;
	//private static final double UNFEASIBLE_SCORE = Double.NEGATIVE_INFINITY;
	private static final double PENALTY = -100;

	//private final DirectSearchOptimizer optimizer = new NelderMead();
	private final DirectSearchOptimizer optimizer = new MultiDirectional();
	private final DurationOnTheFlyScorer scorer;
	private final DurationFitnessFunction fitness = new DurationFitnessFunction();

	private JointPlanOptimizerJGAPChromosome chromosome = null;
	private JointPlan plan = null;
	private DoubleGene[] dimensions = null;

	private boolean optimizerConfigured = false;

	public DurationSimplexAlgo(
			final DurationOnTheFlyScorer scorer) {
		this.scorer = scorer;
	}

	@Override
	public double score(
			final IChromosome chromosome,
			final JointPlan inputPlan) {
		this.chromosome = (JointPlanOptimizerJGAPChromosome) chromosome;
		this.plan = inputPlan;

		dimensions = getContinuousDimensions( chromosome.getGenes() );

		if (!optimizerConfigured) {
			configureOptimizer(inputPlan.getClique().getMembers().size());
		}

		RealPointValuePair optimum = null;
		try {
			 optimum = optimizer.optimize(
					fitness, GoalType.MAXIMIZE, getCurrentPoint());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		updateChromosomeValue(optimum.getPoint());

		cleanUp();
		return optimum.getValue();
	}

	private void cleanUp() {
		chromosome = null;
		plan = null;
		dimensions = null;
	}

	private void configureOptimizer(final int nMembers) {
		// set the convergence monitor
		optimizer.setConvergenceChecker(
				new SimplexConvergenceChecker(nMembers));

		// define the size of the initial vertex
		double[] steps = new double[dimensions.length];

		for (int i=0; i < dimensions.length; i++) {
			steps[i] = STEP;
		}

		optimizer.setStartConfiguration(steps);
		optimizerConfigured = true;
	}

	private double[] getCurrentPoint() {
		double[] point = new double[dimensions.length];

		for (int i = 0; i < dimensions.length; i++) {
			point[i] = dimensions[i].doubleValue();
		}

		return point;
	}

	private double updateChromosomeValue(final double[] point) {
		if (point.length != dimensions.length) {
			throw new IllegalArgumentException("dimensions incompatible");
		}

		double boundBreak = 0;

		for (int i = 0; i < point.length; i++) {
			double value = point[i];
			if (value < dimensions[i].getLowerBound()) {
				value = dimensions[i].getLowerBound();
			}
			else if (value > dimensions[i].getUpperBound()) {
				value = dimensions[i].getLowerBound();
			}
			boundBreak += Math.abs(point[i] - value);
			dimensions[i].setAllele( value );
		}

		return boundBreak;
	}

	private DoubleGene[] getContinuousDimensions(final Gene[] genes) {
		List<DoubleGene> dimensionsList = new ArrayList<DoubleGene>(genes.length);

		for (Gene gene : genes) {
			if (gene instanceof DoubleGene) {
				dimensionsList.add((DoubleGene) gene);
			}
		}
		
		return dimensionsList.toArray(new DoubleGene[0]);
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private class DurationFitnessFunction implements MultivariateRealFunction {
		@Override
		public double value(final double[] point)
				throws FunctionEvaluationException, IllegalArgumentException {
			double boundBreak = updateChromosomeValue(point);

			return scorer.score(chromosome, plan) +
				chromosome.getDegreeOfConstraintsBreak() * PENALTY +
				boundBreak * PENALTY;
		}
	}

	private static class SimplexConvergenceChecker implements RealConvergenceChecker {
		private final double threshold;

		public SimplexConvergenceChecker(final int nMembers) {
			threshold = nMembers * MIN_IMPROVEMENT;
		}

		@Override
		public boolean converged(
				final int iteration,
				final RealPointValuePair previousWorstPoint,
				final RealPointValuePair currentWorstPoint) {
			double previousValue = previousWorstPoint.getValue();
			double currentValue = currentWorstPoint.getValue();
			double improvement = Math.abs(currentValue - previousValue);
			boolean converged = (iteration > N_MAX_ITERS) || (improvement < threshold);

			//if (converged) {
			//	log.debug("convergence: "+iteration+" iters");
			//}

			return converged;
		}
	}
}
