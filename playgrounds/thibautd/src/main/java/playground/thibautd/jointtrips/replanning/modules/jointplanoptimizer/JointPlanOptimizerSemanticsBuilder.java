/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerSemanticsBuilder.java
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

import org.jgap.InvalidConfigurationException;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.AbstractJointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.ConstraintsManager;

/**
 * Provides factory methods to create all semantics-related configuration elements.
 * One builder must be initialised for each JPO instance , and provided to the
 * {@link JointPlanOptimizerJGAPConfiguration} constructor. The idea is to group
 * all semantics/encoding dependent initialisation in on object, to prevent
 * including inconsistencies when trying out new encodings/optimised dimensions.
 * When changing the encoding / constraints, changing only this class should be sufficient.
 * <br>
 * The builder IS NOT RESPONSIBLE FOR SETTING THE ELEMENTS
 *
 * @author thibautd
 */
public interface JointPlanOptimizerSemanticsBuilder {
	/**
	 * Creates a sample chromosome. First method to be called: other objects
	 * constructed by the builder are not yet available from the config.
	 *
	 * @param plan the plan to optimise
	 * @param configuration the configuration object being initialised
	 * @return a fully initialised chromosome
	 * @throws InvalidConfigurationException if it is thrown at initialisation of the component
	 */
	public JointPlanOptimizerJGAPChromosome createSampleChromosome(
			JointPlan plan,
			JointPlanOptimizerJGAPConfiguration configuration ) throws InvalidConfigurationException;

	/**
	 * Creates a constraint manager. Second method to be called: the sample chromosome
	 * is available, but not the fitness.
	 *
	 * @param plan the plan to optimise
	 * @param configuration the configuration object being initialised
	 * @return a fully initialised constraints manager
	 * @throws InvalidConfigurationException if it is thrown at initialisation of the component
	 */
	public ConstraintsManager createConstraintsManager(
			JointPlan plan,
			JointPlanOptimizerJGAPConfiguration configuration ) throws InvalidConfigurationException;

	/**
	 * Creates a fitness function. last method called: chromosome and constraint
	 * manager are available.
	 *
	 * @param plan the plan to optimise
	 * @param configuration the configuration object being initialised
	 * @return a fully initialised fitness function
	 * @throws InvalidConfigurationException if it is thrown at initialisation of the component
	 */
	public AbstractJointPlanOptimizerFitnessFunction createFitnessFunction(
			JointPlan plan,
			JointPlanOptimizerJGAPConfiguration configuration ) throws InvalidConfigurationException;
  
}

