/* *********************************************************************** *
 * project: org.matsim.*
 * TournamentSelectorWithRemoval.java
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;
import org.jgap.RandomGenerator;

/**
 * A class implementing the tournament selection scheme. It is based on the
 * JGAP TournamentSelector, with the difference that here, selected chromosomes
 * are removed from the pool of candidate chromosomes. Thus, no chromosome
 * can be selected multiple times.
 *
 * @author thibautd
 */
public class TournamentSelectorWithRemoval extends NaturalSelectorExt {
	private static final long serialVersionUID = 1L;

	private final int tournamentSize;
	private final double probability;
	private final List<IChromosome> chromosomes = new ArrayList<IChromosome>();

	/**
	 * Comparator that is only concerned about fitness values
	 */
	private final FitnessValueComparator fitnessComparator = new FitnessValueComparator();

	/**
	 * @param a_config the configuration to use
	 * @param a_tournament_size the size of each tournament to play
	 * @param a_probability probability for selecting the best individuals
	 *
	 * @throws InvalidConfigurationException
	 *
	 */
	public TournamentSelectorWithRemoval(
			final Configuration a_config,
			final int a_tournament_size,
			final double a_probability) throws InvalidConfigurationException {
		super(a_config);
		if (a_tournament_size < 1) {
			throw new IllegalArgumentException("Tournament size must be at least 1!");
		}
		if (a_probability <= 0.0d || a_probability > 1.0d) {
			throw new IllegalArgumentException("Probability must be greater 0.0 and"
					+ " less or equal than 1.0!");
		}
		tournamentSize = a_tournament_size;
		probability = a_probability;
	}

	/**
	 * Select a given number of Chromosomes from the pool that will move on
	 * to the next generation population. This selection will be guided by the
	 * fitness values. The chromosomes with the best fitness value win.
	 *
	 * @param a_howManyToSelect int
	 * @param a_to_pop the population the Chromosomes will be added to
	 *
	 */
	@Override
	public void selectChromosomes(
			final int a_howManyToSelect,
			final Population a_to_pop) {
		List<IChromosome> tournament = new ArrayList<IChromosome>( tournamentSize );
		RandomGenerator random = getConfiguration().getRandomGenerator();

		int size = chromosomes.size();
		if (size == 0) {
			return;
		}

		int k;
		IChromosome toAdd;
		for (int i = 0; i < a_howManyToSelect; i++) {
			// Choose [tournament size] individuals from the population at random.
			// -------------------------------------------------------------------
			tournament.clear();
			for (int j = 0; j < tournamentSize; j++) {
				k = random.nextInt( size );
				tournament.add( chromosomes.get( k ) );
			}
			Collections.sort(tournament, fitnessComparator);
			double prob = random.nextDouble();
			double probAccumulated = probability;
			int index = 0;
			// Play the tournament.
			// --------------------
			if (tournamentSize > 1) {
				do {
					if (prob <= probAccumulated) {
						break;
					}
					else {
						probAccumulated += probAccumulated * (1 - probability);
						index++;
					}
				} while (index < tournamentSize - 1);
			}

			toAdd = tournament.get(index);
			a_to_pop.addChromosome( toAdd );
			// do not allow re-selection of already selected chromosomes
			if (size > 2) {
				chromosomes.remove( toAdd );
			}
			size--;
		}
	}

	@Override
	public boolean returnsUniqueChromosomes() {
		return false;
	}

	@Override
	public void empty() {
		chromosomes.clear();
	}

	/**
	 *
	 * @param a_chromosomeToAdd the chromosome to add
	 *
	 */
	@Override
	protected void add(final IChromosome a_chromosomeToAdd) {
		chromosomes.add(a_chromosomeToAdd);
	}

	/**
	 * Comparator regarding only the fitness value. Best fitness value will
	 * be on first position of resulting sorted list.
	 *
	 */
	private class FitnessValueComparator implements Comparator<IChromosome> {
		@Override
		public int compare(
				final IChromosome chrom1,
				final IChromosome chrom2) {
			if (getConfiguration().getFitnessEvaluator().isFitter(chrom2.
					getFitnessValue(), chrom1.getFitnessValue())) {
				return 1;
			}
			else if (getConfiguration().getFitnessEvaluator().isFitter(
					chrom1.getFitnessValue(), chrom2.getFitnessValue())) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}
}
