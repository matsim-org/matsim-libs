/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatJGAPConfiguration.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.InvalidConfigurationException;
import org.jgap.event.EventManager;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatChromosome;
import playground.thibautd.planomat.api.PlanomatFitnessFunction;
import playground.thibautd.planomat.api.PlanomatFitnessFunctionFactory;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;

public class PlanomatJGAPConfiguration extends Configuration {

	private static final long serialVersionUID = 1L;

	public PlanomatJGAPConfiguration(
			final Plan plan,
			final PlanomatFitnessFunctionFactory fitnessFunctionFactory,
			final long seed,
			final ActivityWhiteList whiteList,
			final Planomat2ConfigGroup planomatConfigGroup) {

		// JGAP Configuration object is initialized without an id. This means there can be only one configuration object per thread, which is what we want.
		super(null);

		// Configuration for current thread is reset.
		Configuration.reset();

		try {
			// initialize random number generator
			setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) this.getRandomGenerator()).setSeed( seed );

			setEventManager(new EventManager());
			setBreeder(new PlanomatGABreeder());

			// initialize population properties
			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			PlanomatFitnessFunction fitness =
				fitnessFunctionFactory.createFitnessFunction(
						this,
						plan,
						whiteList);
			this.setFitnessFunction( fitness );

			PlanomatChromosome sampleChromosome = fitness.getSampleChomosome();
			this.setSampleChromosome( sampleChromosome );

			// GA parameters
			int populationSize = (int) Math.ceil(planomatConfigGroup.getPopIntercept() +
				planomatConfigGroup.getPopSlope() * sampleChromosome.size());
			this.setPopulationSize( Math.max( 2 , populationSize ) );

			// initialize genetic operators
			setChromosomePool(new ChromosomePool());
			addGeneticOperator(new CrossoverOperator(this, 0.6d));
			addGeneticOperator(new MutationOperator(this, this.getPopulationSize()));

			// initialize selection:
			BestChromosomesSelector bestChromsSelector =
				new BestChromosomesSelector(
					this, 0.90d);
			bestChromsSelector.setDoubletteChromosomesAllowed(false);
			addNaturalSelector(bestChromsSelector, false);
		} catch (Exception e) {
			throw new RuntimeException("problem at initialisation of "+getClass(), e);
		}
	}
}
