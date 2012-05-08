/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatConfigurationFactory.java
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
package playground.thibautd.planomat.api;

import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.event.EventManager;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.planomat.PlanomatGABreeder;

/**
 * Creates jgap configuration objects for Planomat v2.
 *
 * <br>
 * It sets all the part the should not change with application, and delegates
 * the genetic process tuning (population size and operators) to subclasses.
 *
 * @author thibautd
 */
public abstract class PlanomatConfigurationFactory {

	/**
	 * Creates a new configuration.
	 *
	 * @param plan the plan to optimize
	 * @param whiteList the activity white list
	 * @param fitnessFunctionFactory the fitness function factory
	 * @param seed the random seed to use to initialise the random generator
	 *
	 * @return a ready-to-use configuration object
	 */
	public Configuration createConfiguration(
			final Plan plan,
			final ActivityWhiteList whiteList,
			final PlanomatFitnessFunctionFactory fitnessFunctionFactory,
			final long seed) {
		// JGAP Configuration object is initialized without an id. config means there can be only one configuration object per thread, which is what we want.
		Configuration config = new Configuration( null );

		// Configuration for current thread is reset.
		Configuration.reset();

		try {
			// initialize random number generator
			config.setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) config.getRandomGenerator()).setSeed( seed );

			config.setEventManager(new EventManager());
			config.setBreeder(new PlanomatGABreeder());
			config.setChromosomePool(new ChromosomePool());

			// initialize population properties
			config.setFitnessEvaluator(new DefaultFitnessEvaluator());
			PlanomatFitnessFunction fitness =
				fitnessFunctionFactory.createFitnessFunction(
						config,
						plan,
						whiteList);
			config.setFitnessFunction( fitness );

			PlanomatChromosome sampleChromosome = fitness.getSampleChomosome();
			config.setSampleChromosome( sampleChromosome );

			// customisable part
			setPopulationSize( config );
			setNaturalSelectors( config );
			setGeneticOperators( config );
		} catch (Exception e) {
			throw new RuntimeException("problem at initialisation of "+getClass(), e);
		}

		return config;
	}

	/**
	 * Adds genetic operators to the config.
	 *
	 * @param config the config to set
	 */
	protected abstract void setGeneticOperators(final Configuration config) throws Exception;

	/**
	 * Adds natural selectors to the config.
	 * @param config the config to set
	 */
	protected abstract void setNaturalSelectors(final Configuration config) throws Exception;

	/**
	 * Sets the population size in the config.
	 * @param config the config to set
	 */
	protected abstract void setPopulationSize(final Configuration config) throws Exception;
}

