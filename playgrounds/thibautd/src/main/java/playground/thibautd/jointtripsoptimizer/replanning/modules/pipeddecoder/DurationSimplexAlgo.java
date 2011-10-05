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
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.optimization.direct.MultiDirectional;
import org.apache.commons.math.optimization.direct.NelderMead;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.MultivariateRealOptimizer;
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

	private static final int N_MAX_ITERS = 30;
	private static final double MIN_IMPROVEMENT = 1E-7;
	private static final double UNFEASIBLE_SCORE = Double.MIN_VALUE;

	//private final MultivariateRealOptimizer optimizer = new NelderMead();
	private final MultivariateRealOptimizer optimizer = new MultiDirectional();
	private final DurationOnTheFlyScorer scorer;
	private final DurationFitnessFunction fitness = new DurationFitnessFunction();

	private JointPlanOptimizerJGAPChromosome chromosome = null;
	private JointPlan plan = null;
	private DoubleGene[] dimensions = null;

	private boolean convergenceCheckerSet = false;

	public DurationSimplexAlgo(
			final DurationOnTheFlyScorer scorer) {
		this.scorer = scorer;
	}

	public double score(
			final IChromosome chromosome,
			final JointPlan inputPlan) {
		this.chromosome = (JointPlanOptimizerJGAPChromosome) chromosome;
		this.plan = inputPlan;

		if (!convergenceCheckerSet) {
			optimizer.setConvergenceChecker(
					new SimplexConvergenceChecker(inputPlan.getClique().getMembers().size()));
			convergenceCheckerSet = true;
		}

		Gene[] genes = chromosome.getGenes();
		dimensions = getContinuousDimensions(genes);

		RealPointValuePair optimum = null;
		try {
			 optimum = optimizer.optimize(
					fitness, GoalType.MAXIMIZE, getCurrentPoint());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		updateChromosomeValue(optimum.getPoint());

		return optimum.getValue();
	}

	private double[] getCurrentPoint() {
		double[] point = new double[dimensions.length];

		for (int i = 0; i < dimensions.length; i++) {
			point[i] = dimensions[i].doubleValue();
		}

		return point;
	}

	private void updateChromosomeValue(final double[] point) {
		for (int i = 0; i < point.length; i++) {
			dimensions[i].setAllele(point[i]);
		}
	}

	private DoubleGene[] getContinuousDimensions(final Gene[] genes) {
		List<DoubleGene> dimensions = new ArrayList<DoubleGene>(genes.length);

		for (Gene gene : genes) {
			if (gene instanceof DoubleGene) {
				dimensions.add((DoubleGene) gene);
			}
		}
		
		return dimensions.toArray(new DoubleGene[0]);
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private class DurationFitnessFunction implements MultivariateRealFunction {
		@Override
		public double value(final double[] point)
				throws FunctionEvaluationException, IllegalArgumentException {
			updateChromosomeValue(point);

			if (chromosome.respectsConstraints()) {
				return scorer.score(chromosome, plan);
			}

			return UNFEASIBLE_SCORE;
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
			//	log.debug("convergence: "+iteration+" iters, "
			//			+improvement+" value diff, "
			//			+threshold+" theshold.");
			//}

			return converged;
		}
	}
}

