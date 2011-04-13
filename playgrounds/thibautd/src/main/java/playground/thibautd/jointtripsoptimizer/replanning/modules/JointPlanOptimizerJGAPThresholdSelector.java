/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPThresholdSelector.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;
import org.jgap.RandomGenerator;

/**
 * Jgap "natural selector", similar to jgap's ThresholdSelector.
 * The difference is that this selector allows to choose whether to draw
 * chromosomes with replacement (jgap's implementation draws with replacement
 * only).
 * Preventing "doublettes" helps to maintain more diversity.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPThresholdSelector extends NaturalSelectorExt {
	private final static long serialVersionUID = 1L;
	
	private List m_chromosomes;
	private boolean m_needsSorting;
	private FitnessValueComparator m_fitnessValueComparator;
	private final double selectionThreshold;
	
	/**
	 * Constructor, taken from jgap's ThresholdSelector.
	 *
	 * @param a_config the configuration to use
	 * @param a_bestChromosomes_Percentage indicates the number of best
	 * chromosomes from the population to be selected for granted. All other
	 * chromosomes will be selected in a random fashion. The value must be in
	 * the range from 0.0 to 1.0.
	 *
	 * @throws InvalidConfigurationException
	 */
	public JointPlanOptimizerJGAPThresholdSelector(
			final Configuration a_config,
			final double a_bestChromosomes_Percentage)
			throws InvalidConfigurationException {
		super(a_config);
		if (a_bestChromosomes_Percentage < 0.0000000d
			  || a_bestChromosomes_Percentage > 1.0000000d) {
			throw new IllegalArgumentException("Percentage must be between 0.0"
			    + " and 1.0 !");
		}
		selectionThreshold = a_bestChromosomes_Percentage;
		m_chromosomes = new Vector();
		m_needsSorting = false;
		m_fitnessValueComparator = new FitnessValueComparator();
	}
	
	/**
	 * Select a given number of Chromosomes from the pool that will move on
	 * to the next generation population. This selection will be guided by the
	 * fitness values. The chromosomes with the best fitness value win.
	 *
	 * @param a_howManyToSelect the number of Chromosomes to select.
	 * @param a_to_pop the population the Chromosomes will be added to
	 */
	public void selectChromosomes(
			final int a_howManyToSelect,
			final Population a_to_pop) {
		boolean withReplacement = this.getDoubletteChromosomesAllowed();
		if ( (!withReplacement) &&
				(a_howManyToSelect > m_chromosomes.size()) ) {
			throw new IllegalArgumentException("cannot draw "+a_howManyToSelect+
					" elements from a population of "+m_chromosomes.size()+" individuals");
		}
		int canBeSelected;
		if (a_howManyToSelect > m_chromosomes.size()) {
			canBeSelected = m_chromosomes.size();
		}
		else {
			canBeSelected = a_howManyToSelect;
		}

		// Sort the collection of chromosomes previously added for evaluation.
		// Only do this if necessary.
		// -------------------------------------------------------------------
		if (m_needsSorting) {
			Collections.sort(m_chromosomes, m_fitnessValueComparator);
			m_needsSorting = false;
		}
		// Select the best chromosomes for granted
		int bestToBeSelected = (int) 
			Math.round(canBeSelected * this.selectionThreshold);

		if (withReplacement) {
			for (int i = 0; i < bestToBeSelected; i++) {
				a_to_pop.addChromosome( (IChromosome) m_chromosomes.get(i));
			}
		}
		else {
			for (int i = 0; i < bestToBeSelected; i++) {
				a_to_pop.addChromosome( (IChromosome) m_chromosomes.remove(0));
			}
		}

		// Fill up the rest by randomly selecting chromosomes.
		// ---------------------------------------------------
		int missing = a_howManyToSelect - bestToBeSelected;
		RandomGenerator rn = getConfiguration().getRandomGenerator();
		int index;
		int size = m_chromosomes.size();
		
		if (withReplacement) {
			for (int i=0; i < missing; i++) {
				index = rn.nextInt(size);
				IChromosome chrom = (IChromosome) m_chromosomes.get(index);
				a_to_pop.addChromosome(chrom);
			}
		}
		else {
			for (int i=0; i < missing; i++) {
				index = rn.nextInt(size - i);
				IChromosome chrom = (IChromosome) m_chromosomes.remove(index);
				a_to_pop.addChromosome(chrom);
			}
		}
	}
	
	/**
	 * @return true if the chromosomes are drawn without replacement.
	 */
	public boolean returnsUniqueChromosomes() {
		return !this.getDoubletteChromosomesAllowed();
	}
	
	public void empty() {
		m_chromosomes.clear();
		m_needsSorting = false;
	}
	
	/**
	 * @param a_chromosomeToAdd Chromosome
	 */
	protected void add(final IChromosome a_chromosomeToAdd) {
		m_chromosomes.add(a_chromosomeToAdd);
		m_needsSorting = true;
	}
	
	/**
	 * Comparator regarding only the fitness value. Best fitness value will
	 * be on first position of resulting sorted list.
	 * taken from jgap's code.
	 */
	private class FitnessValueComparator implements Comparator {
		public FitnessValueComparator() {}

		public int compare(final Object a_first, final Object a_second) {
			IChromosome chrom1 = (IChromosome) a_first;
			IChromosome chrom2 = (IChromosome) a_second;
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
