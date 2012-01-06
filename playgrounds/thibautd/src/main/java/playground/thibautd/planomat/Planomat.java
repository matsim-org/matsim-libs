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

import org.apache.log4j.Logger;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatFitnessFunction;
import playground.thibautd.planomat.api.PlanomatFitnessFunctionFactory;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;

/**
 * The core of the v2 of the planomat external strategy module:
 * optimises a plan using a genetic algorithm.
 *
 * The dimensions optimised, as well as the assumptions behind the
 * score estimation, are encapsulated in a {@link PlanomatFitnessFunctionFactory}
 * passed at the constructor.
 *
 * The genetic process is standard and non extensible, and is basically what provides
 * this new version of planomat.
 *
 * Assembling of the elements (ie initialisation of the
 * {@link PlanomatFitnessFunctionFactory}) is let to the {@link PlanStrategyModule}
 * using this algorithm.
 *
 * @author thibautd, based on meisterk
 *
 */
public class Planomat implements PlanAlgorithm {

	private final Planomat2ConfigGroup planomatConfigGroup;
	private final Random seedGenerator;
	private final PlanomatFitnessFunctionFactory fitnessFunctionFactory;
	private final ActivityWhiteList whiteList;

	private final static Logger logger = Logger.getLogger(Planomat.class);
	private final boolean doLogging;

	public Planomat(
			final PlanomatFitnessFunctionFactory fitnessFunctionFactory,
			final ActivityWhiteList whiteList,
			final Planomat2ConfigGroup configGroup) {
		this.planomatConfigGroup = configGroup;
		this.fitnessFunctionFactory = fitnessFunctionFactory;
		this.whiteList = whiteList;

		this.doLogging = this.planomatConfigGroup.isDoLogging();

		this.seedGenerator = MatsimRandom.getLocalInstance();
	}

	@Override
	public void run(final Plan plan) {

		if (this.doLogging) {
			logger.info("Running planomat on plan of person # " + plan.getPerson().getId().toString() + "...");
		}

		long seed = this.seedGenerator.nextLong();
		if (this.doLogging) {
			logger.info("agent id: " + plan.getPerson().getId() + "; JGAP seed: " + Long.toString(seed));
		}

		PlanomatJGAPConfiguration jgapConfiguration = new PlanomatJGAPConfiguration(
				plan,
				fitnessFunctionFactory,
				seed,
				whiteList,
				planomatConfigGroup);

		Genotype population = null;
		try {
			population = Genotype.randomInitialGenotype( jgapConfiguration );
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}
		if (this.doLogging) {
			logger.info("Initialization of JGAP configuration...done.");
			logger.info("Running evolution...");
		}

		IChromosome fittest = this.evolveAndReturnFittest(population);

		if (this.doLogging) {
			logger.info("Running evolution...done.");
			logger.info("Writing solution back to Plan object...");
		}

		((PlanomatFitnessFunction) jgapConfiguration.getFitnessFunction()).modifyBackPlan( fittest );

		if (this.doLogging) {
			logger.info("Writing solution back to Plan object...done.");
			logger.info("Running planomat on plan of person # " + plan.getPerson().getId().toString() + "...done.");
		}

		// invalidate score information
		plan.setScore( null );
	}

	private IChromosome evolveAndReturnFittest(final Genotype population) {
		// TODO: use fitness monitoring
		population.evolve(planomatConfigGroup.getJgapMaxGenerations());

		return population.getFittestChromosome();
	}
}
