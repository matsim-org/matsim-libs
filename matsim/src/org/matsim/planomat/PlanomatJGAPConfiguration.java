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

package org.matsim.planomat;

import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.InvalidConfigurationException;
import org.jgap.event.EventManager;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.GABreeder;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;
import org.jgap.impl.WeightedRouletteSelector;
import org.matsim.config.groups.PlanomatConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

public class PlanomatJGAPConfiguration extends Configuration {

	/**
	 * default serial version ID
	 */
	private static final long serialVersionUID = 1L;

	public PlanomatJGAPConfiguration(Plan plan, PlanAnalyzeSubtours planAnalyzeSubtours) {
		this("", "", plan, planAnalyzeSubtours);
	}

	private PlanomatJGAPConfiguration(String a_id, String a_name, Plan plan, PlanAnalyzeSubtours planAnalyzeSubtours) {
		super(a_id, a_name);

		Configuration.reset();
		
		setBreeder(new GABreeder());
		try {
			// initialize random number generator
			setRandomGenerator(new StockRandomGenerator());
			setEventManager(new EventManager());

			// initialize selection:
			// - weighted roulette wheel selection (standard)
			WeightedRouletteSelector weightedRouletteSelector = new WeightedRouletteSelector(this);
			addNaturalSelector(weightedRouletteSelector, true);
			// - elitism (de Jong, 1975)
			this.setPreservFittestIndividual(true);

			// initialize population properties
			// - population size: equal to the string length, if not specified otherwise (de Jong, 1975)
			if (Gbl.getConfig().planomat().getPopSize() == Integer.parseInt(PlanomatConfigGroup.PlanomatConfigParameter.POPSIZE.getDefaultValue())) {
				
				int numActs = plan.getActsLegs().size() / 2;
				int populationSize = Gbl.getConfig().planomat().getLevelOfTimeResolution() * numActs;
				if (Gbl.getConfig().planomat().getPossibleModes().length > 0) {
					populationSize += Gbl.getConfig().planomat().getPossibleModes().length * planAnalyzeSubtours.getNumSubtours();
				}
				this.setPopulationSize( populationSize );
				
			} else {
				
				this.setPopulationSize( Gbl.getConfig().planomat().getPopSize() );
				
			}

			// initialize fitness function
			// - maximum selection
			setFitnessEvaluator(new DefaultFitnessEvaluator());

			// initialize genetic operators
			setChromosomePool(new ChromosomePool());
			addGeneticOperator(new CrossoverOperator(this, 0.6d));
			addGeneticOperator(new MutationOperator(this, this.getPopulationSize()));

		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(
					"Fatal error: DefaultConfiguration class could not use its "
					+ "own stock configuration values. This should never happen. "
					+ "Please report this as a bug to the JGAP team.");
		}
	}

	@Override
	public Object clone() {
		return super.clone();
	}

}
