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

import java.util.TreeSet;

import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.event.EventManager;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.GABreeder;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

public class PlanomatJGAPConfiguration extends Configuration {

	private static final long serialVersionUID = 1L;

	public PlanomatJGAPConfiguration(
			Plan plan,
			PlanAnalyzeSubtours planAnalyzeSubtours,
			long seed,
			int numTimeIntervals,
			TreeSet<String> possibleModes,
			PlanomatConfigGroup planomatConfigGroup) {

		// JGAP Configuration object is initialized without an id. This means there can be only one configuration object per thread, which is what we want.
		super(null);

		// Configuration for current thread is reset.
		Configuration.reset();

		this.setBreeder(new GABreeder());
		try {
			// initialize random number generator
			this.setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) this.getRandomGenerator()).setSeed( seed );

			setEventManager(new EventManager());

			this.setBreeder(new PlanomatGABreeder());

			// initialize selection:
			BestChromosomesSelector bestChromsSelector = new BestChromosomesSelector(
					this, 0.90d);
			bestChromsSelector.setDoubletteChromosomesAllowed(false);
			this.addNaturalSelector(bestChromsSelector, false);

			// - elitism (de Jong, 1975)
			this.setPreservFittestIndividual(true);

			// initialize population properties
			// - population size: equal to the string length, if not specified otherwise (de Jong, 1975)
			int numActs = 0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					numActs++;
				}
			}
			// first and last activity are the same
			// TODO what if this is not the case?
			numActs -= 1;

			int numSubtours = 0;
			if (planAnalyzeSubtours != null) {
				numSubtours = planAnalyzeSubtours.getNumSubtours();
			}

			int populationSize = planomatConfigGroup.getPopSize();
			if (populationSize == Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.POPSIZE.getDefaultValue())) {

				populationSize = 1;
				populationSize += planomatConfigGroup.getLevelOfTimeResolution() * numActs;
				populationSize += planomatConfigGroup.getPossibleModes().size() * numSubtours;

			}
			this.setPopulationSize(populationSize);

			// initialize sample chromosome
			Gene[] sampleGenes = new Gene[1 + numActs + numSubtours];

			// first integer gene for the start time of the plan
			sampleGenes[0] = new IntegerGene(this, 0, numTimeIntervals - 1);
			// one integer gene for each activity duration
			for (int ii=0; ii < numActs; ii++) {
				sampleGenes[1 + ii] = new IntegerGene(this, 0, numTimeIntervals - 1);
			}
			// one integer gene for the mode of each subtour
			for (int ii=0; ii < numSubtours; ii++) {
				sampleGenes[1 + numActs + ii] = new IntegerGene(this, 0, possibleModes.size() - 1);
			}

			IChromosome sampleChromosome = null;
			try {
				sampleChromosome = new PlanomatJGAPChromosome( this, sampleGenes );
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
			this.setSampleChromosome(sampleChromosome);

			// initialize fitness function
			// - maximum selection
			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			// - MATSim scoring function

			// initialize genetic operators
			this.setChromosomePool(new ChromosomePool());
			this.addGeneticOperator(new CrossoverOperator(this, 0.6d));
			this.addGeneticOperator(new MutationOperator(this, this.getPopulationSize()));

		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
