/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPEvolutionMonitor.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.audit.IEvolutionMonitor;
import org.jgap.Configuration;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
public class JointPlanOptimizerJGAPEvolutionMonitor implements IEvolutionMonitor {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPEvolutionMonitor.class);


	private final int period;
	private final int minIterations;
	private final int maxIterations;
	private final Configuration jgapConfig;
	private final double minImprovement;

	private int iterationsToEval = 0;
	private double oldBest = Double.NEGATIVE_INFINITY;

	public JointPlanOptimizerJGAPEvolutionMonitor(
			final Configuration jgapConfig,
			final JointReplanningConfigGroup configGroup) {
		this.period = configGroup.getMonitoringPeriod();
		this.minIterations = configGroup.getMinIterations();
		this.maxIterations = configGroup.getMaxIterations();
		this.minImprovement = configGroup.getMinImprovement();
		this.jgapConfig = jgapConfig;
	}

	@Override
	public void start(final Configuration config) {
		if (this.jgapConfig != config) {
			throw new IllegalArgumentException("the monitor must be ran "+
					"with the config used to initialize it");
		}
	}

	@Override
	public boolean nextCycle(
			final Population population,
			final List<String> messages) {
		int iteration = this.jgapConfig.getGenerationNr();

		if (iteration < this.minIterations) {
			return true;
		}
		if (iteration == this.maxIterations) {
			return false;
		}
		return enoughImprovement(population);
	}

	private boolean enoughImprovement(final Population population) {

		if (iterationsToEval == 0) {
			iterationsToEval = this.period;
			double currentBest = population.determineFittestChromosome().getFitnessValue();
			boolean output = (currentBest - oldBest > this.minImprovement);
			oldBest = currentBest;
			return output;
		}

		this.iterationsToEval--;
		return true;
	}
}

