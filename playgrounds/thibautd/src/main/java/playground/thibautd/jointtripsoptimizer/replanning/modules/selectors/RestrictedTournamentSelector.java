/* *********************************************************************** *
 * project: org.matsim.*
 * RestrictedTournamentSelector.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;
import org.jgap.RandomGenerator;

/**
 * Selector using a "restricted tournament" (cf Harik 1995) to generate the new 
 * population.
 * The idea is the following:
 *  -for each "new born", <i>w</i> chromosomes from the population are selected
 *   randomly.
 *  -the "closest" chromosome of this set is selected for competition
 *  -if the new born is fittest than the chromosome competitor, it replaces it
 *   (and becomes thus part of the possible competitors for not yet added new
 *   born)
 *
 * @author thibautd
 */
public class RestrictedTournamentSelector extends NaturalSelectorExt {

	private static final long serialVersionUID = 1L;

	private final List<IChromosome> newBorned = new ArrayList<IChromosome>();
	// protected for use in the "tabued" version. If approach works, improve.
	protected final List<IChromosome> agedIndividuals = new ArrayList<IChromosome>();
	private final int windowSize;
	private final Configuration jgapConfig;
	private final ChromosomeDistanceComparator distanceComparator;

	public RestrictedTournamentSelector(
			final Configuration jgapConfig,
			final ChromosomeDistanceComparator distanceComparator
			) throws InvalidConfigurationException {
		super(jgapConfig);
		this.windowSize = 15;
		this.jgapConfig = jgapConfig;
		this.distanceComparator = distanceComparator;
	}

	@Override
	public void empty() {
		this.newBorned.clear();
		this.agedIndividuals.clear();
	}

	@Override
	public boolean returnsUniqueChromosomes() {
		return true;
	}

	@Override
	protected void add(
			final IChromosome chromosome) {
		if (chromosome.getAge() == 0) {
			this.newBorned.add(chromosome);
		}
		else {
			this.agedIndividuals.add(chromosome);
		}
	}

	/**
	 * Selects the chromosomes for the next generation.
	 */
	@Override
	protected void selectChromosomes(
			final int nToSelect,
			final Population nextGeneration) {
		List<IChromosome> window;
		IChromosome closestOldCompetitor;
		RandomGenerator generator = this.jgapConfig.getRandomGenerator();

		// examine all new borned and make them compete with old fellows by RTS
		for (IChromosome competitor : this.newBorned) {
			Collections.shuffle(this.agedIndividuals, (Random) generator);
			window = this.agedIndividuals.subList(0, this.windowSize);
			this.distanceComparator.setComparisonData(competitor, window);
			Collections.sort(window, this.distanceComparator);
			closestOldCompetitor = window.get(0);

			if (competitor.getFitnessValue() > closestOldCompetitor.getFitnessValue()) {
				this.agedIndividuals.add(competitor);
				this.agedIndividuals.remove(closestOldCompetitor);
			}
		}

		for (IChromosome chrom : this.agedIndividuals) {
			nextGeneration.addChromosome(chrom);
		}

		if (nextGeneration.size() != nToSelect) {
			throw new IllegalArgumentException("RTS must be used to generate the"
					+" full population: toSelect="+nToSelect+", generationSize="+
					nextGeneration.size());
		}
	}
}

