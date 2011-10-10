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

import java.lang.management.ManagementFactory;

import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.audit.IEvolutionMonitor;
import org.jgap.Configuration;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Monitors the evolution and stops them if stagnation is observed.
 *
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
	private final long maxCpuTime;
	private final boolean monitorTime;
	private long maxEndTime = Long.MAX_VALUE;

	// for log only
	private final int nMembers;

	private int iterationsToEval = 0;
	private double oldBest = Double.NEGATIVE_INFINITY;

	public JointPlanOptimizerJGAPEvolutionMonitor(
			final Configuration jgapConfig,
			final JointReplanningConfigGroup configGroup,
			final int nMembers) {
		this.period = configGroup.getMonitoringPeriod();
		this.minIterations = configGroup.getMinIterations();
		this.maxIterations = configGroup.getMaxIterations();
		this.minImprovement = nMembers * configGroup.getMinImprovement();
		this.jgapConfig = jgapConfig;
		this.maxCpuTime = (long) nMembers * configGroup.getMaxCpuTimePerMemberNanoSecs();
		this.monitorTime = (this.maxCpuTime >= 0); 
		this.nMembers = nMembers;
	}

	@Override
	public void start(final Configuration config) {
		if (this.jgapConfig != config) {
			throw new IllegalArgumentException("the monitor must be ran "+
					"with the config used to initialize it");
		}
		if (monitorTime) {
			this.maxEndTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() + this.maxCpuTime;
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

		return enoughImprovement(population) && !timeIsElapsed();
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

	private boolean timeIsElapsed() {
		if (monitorTime) {
			boolean timeElapsed = this.maxEndTime <= ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
			if (timeElapsed) log.debug("stopping genetic iterations based on computation time after "+this.jgapConfig.getGenerationNr()+" iterations for clique with "+nMembers+" members");
			return timeElapsed;
		}

		return false;
	}
}

