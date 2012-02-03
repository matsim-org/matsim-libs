/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerProcessBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer;

import java.util.Collection;

import org.jgap.audit.IEvolutionMonitor;
import org.jgap.GeneticOperator;
import org.jgap.NaturalSelector;

import playground.thibautd.jointtrips.population.JointPlan;

/**
 * Provides factory methods to create the elements responsible for the the genetic
 * process.
 * All methods are called after all semantics elements were set, in the order specified
 * in the javadocs of the methods.
 *
 * @author thibautd
 */
public interface JointPlanOptimizerProcessBuilder {
	/**
	 * Gives access to the number of chromosomes. It is the first called method.
	 * @param plan the plan to optimise
	 * @param configuration the configuration object
	 * @return the population size
	 */
	public int getPopulationSize(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) throws Exception;

	/**
	 * Gives access to the evolution monitor. It is the second method called
	 * @param plan the plan to optimise
	 * @param configuration the configuration object
	 * @return the evolution monitor
	 */
	public IEvolutionMonitor createEvolutionMonitor(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) throws Exception;

	/**
	 * Gives access to the natural selector to use after having executed the genetic operators.
	 * It is the third method called.
	 * @param plan the plan to optimise
	 * @param configuration the configuration object
	 * @return the natural selector
	 */
	public NaturalSelector createNaturalSelector(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) throws Exception;

	/**
	 * Gives access to the genetic operators. It is the forth (and last) method called.
	 * @param plan the plan to optimise
	 * @param configuration the configuration object
	 * @return the genetic operators
	 */
	public Collection<GeneticOperator> createGeneticOperators(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) throws Exception;
}

