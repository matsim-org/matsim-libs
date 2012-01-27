/* *********************************************************************** *
 * project: org.matsim.*
 * PlanOptimizeTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.thibautd.planomat;

import java.util.Random;

import org.jgap.Configuration;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatConfigurationFactory;
import playground.thibautd.planomat.api.PlanomatFitnessFunction;
import playground.thibautd.planomat.api.PlanomatFitnessFunctionFactory;

/**
 * The core of the v2 of the planomat external strategy module:
 * optimises a plan using a genetic algorithm.
 * <br>
 * The dimensions optimised, as well as the assumptions behind the
 * score estimation, are encapsulated in a {@link PlanomatFitnessFunctionFactory}
 * passed at the constructor.
 * <br>
 * The genetic process is defined by the configuration objects returned by
 * a {@link PlanomatConfigurationFactory} instance.
 * <br>
 * Assembling of the elements (ie initialisation of the
 * {@link PlanomatFitnessFunctionFactory} and {@link PlanomatConfigurationFactory})
 * is let to the {@link PlanStrategyModule} using this algorithm.
 * <br>
 * The idea is that this class is totally interpretation and parameter blind.
 *
 * @author thibautd, based on meisterk
 *
 */
public class Planomat implements PlanAlgorithm {

	private final Random seedGenerator;
	private final PlanomatFitnessFunctionFactory fitnessFunctionFactory;
	private final PlanomatConfigurationFactory configurationFactory;
	private final ActivityWhiteList whiteList;

	private final static int N_GEN = 100;

	/**
	 * Initialises an instance of planomat.
	 *
	 * @param fitnessFunctionFactory the {@link PlanomatFitnessFunctionFactory}
	 * providing the interpretation of the process.
	 * @param configurationFactory the {@link PlanomatConfigurationFactory}
	 * defining the genetic process.
	 * @param whiteList the activity white list
	 * @param randomSeedSource a random generator to use to generate random
	 * seeds for the genetic process.
	 */
	public Planomat(
			final PlanomatFitnessFunctionFactory fitnessFunctionFactory,
			final PlanomatConfigurationFactory configurationFactory,
			final ActivityWhiteList whiteList,
			final Random randomSeedSource) {
		this.fitnessFunctionFactory = fitnessFunctionFactory;
		this.configurationFactory = configurationFactory;
		this.whiteList = whiteList;

		this.seedGenerator = randomSeedSource;
	}

	@Override
	public void run(final Plan plan) {
		long seed = this.seedGenerator.nextLong();

		Configuration jgapConfiguration = configurationFactory.createConfiguration(
				plan,
				whiteList,
				fitnessFunctionFactory,
				seed);

		Genotype population = null;
		try {
			population = Genotype.randomInitialGenotype( jgapConfiguration );
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}

		// TODO: use evolution monitor (use more recent version of JGAP, for which
		// evolution monitor is in the config?)
		population.evolve( N_GEN );
		IChromosome fittest = population.getFittestChromosome();

		((PlanomatFitnessFunction) jgapConfiguration.getFitnessFunction()).modifyBackPlan( fittest );

		// invalidate score information
		plan.setScore( null );
	}
}
