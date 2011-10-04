/* *********************************************************************** *
 * project: org.matsim.*
 * NsgaIISelector.java
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPChromosome;

/**
 * @author thibautd
 */
public class NsgaIISelector extends NaturalSelectorExt {
	private static final long serialVersionUID = 1L;

	private final List<NsgaEntry> nsgaPopulation = new ArrayList<NsgaEntry>();
	private final FrontMap fronts = new FrontMap();

	// /////////////////////////////////////////////////////////////////////////
	// ctor
	// /////////////////////////////////////////////////////////////////////////
	public NsgaIISelector(final Configuration configuration) throws InvalidConfigurationException {
		super(configuration);
	}

	// /////////////////////////////////////////////////////////////////////////
	// impl. of abstract methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void empty() {
		nsgaPopulation.clear();
		fronts.clear();
	}

	@Override
	public boolean returnsUniqueChromosomes() {
		return false;
	}

	@Override
	protected void add(final IChromosome chromosome) {
		nsgaPopulation.add( new NsgaEntry(chromosome) );
	}

	@Override
	protected void selectChromosomes(
			final int nToSelect,
			final Population nextGeneration) {
		computeRanking();
		int popSize = 0;

		int currentFront = 0;
		List<NsgaEntry> front = fronts.get(currentFront);
		while (popSize + front.size() <= nToSelect) {
			for (NsgaEntry entry : front) {
				nextGeneration.addChromosome(entry.chromosome);
			}
			popSize += front.size();
			currentFront++;
			front = fronts.get(currentFront);
		}

		if (popSize != nToSelect) {
			computeCrowdingMetric(front);
			Collections.sort(front, new CrowdingComparator());

			for (int i = 0; i < nToSelect - popSize; i++) {
				nextGeneration.addChromosome(front.get(i).chromosome);
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private void computeRanking() {
		int currentRank = 0;
		List<NsgaEntry> currentRankElements = new ArrayList<NsgaEntry>();

		for (NsgaEntry entry : nsgaPopulation) {
			for (NsgaEntry other : nsgaPopulation) {
				if (Ranker.dominates(entry, other)) {
					entry.dominatedSolutions.add(other);
				}
				else if (Ranker.dominates(other, entry)) {
					entry.nDominators++;
				}
			}

			if (entry.nDominators == 0) {
				//the current entry is in the first rank (undominated)
				entry.rank = 0;
				currentRankElements.add(entry);
				fronts.put(0, entry);
			}
		}

		while (currentRankElements.size() > 0) {
			List<NsgaEntry> nextRankElements = new ArrayList<NsgaEntry>();

			for (NsgaEntry entry : currentRankElements) {
				for (NsgaEntry dominee : entry.dominatedSolutions) {
					// uncount the members of the currently considered front
					dominee.nDominators--;

					if (dominee.nDominators == 0) {
						// this solution was only dominated by members of the current front
						nextRankElements.add(dominee);
						dominee.rank = currentRank + 1;
						fronts.put(dominee.rank, dominee);
					}
				}
			}

			// now pass to next front
			currentRank++;
			currentRankElements = nextRankElements;
		}
	}

	/**
	 * The crowding metric, in NSGA, is defined over the fitness. The aim is to
	 * have populations well spread over the fitness space (not the parameter space).
	 * Ranking superiority is considered superior to crowding superiority: this metric
	 * is only computed for the last (partly) included front.
	 */
	private void computeCrowdingMetric(final List<NsgaEntry> front) {
		int nMembers = front.get(0).chromosome.getIndividualScores().length;
		int lastElement = front.size() - 1;
		double normalisationFactor;

		for (int i = 0; i < nMembers; i++) {
			Collections.sort(nsgaPopulation, new IndividualComparator(i));

			front.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
			front.get(lastElement).crowdingDistance = Double.POSITIVE_INFINITY;

			normalisationFactor = 1d /
				(front.get(lastElement).chromosome.getIndividualScores()[i] -
				front.get(0).chromosome.getIndividualScores()[i]);

			NsgaEntry currentEntry = front.get(1);
			NsgaEntry lastEntry = front.get(0);
			if (lastElement > 2) {
				for ( NsgaEntry nextEntry : front.subList(2,lastElement) ) {
					currentEntry.crowdingDistance += normalisationFactor *
						(nextEntry.chromosome.getIndividualScores()[i] -
						 lastEntry.chromosome.getIndividualScores()[i]);
				}
			}
		}
	}
}

class NsgaEntry {
	public int nDominators = 0;
	public List<NsgaEntry> dominatedSolutions = new ArrayList<NsgaEntry>();
	public int rank = Integer.MAX_VALUE;
	public double crowdingDistance = 0;
	public JointPlanOptimizerJGAPChromosome chromosome;

	public NsgaEntry(final IChromosome chromosome) {
		try {
			this.chromosome = (JointPlanOptimizerJGAPChromosome) chromosome;
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException("NSGA-II selection can only work with JointPlanOptimizerJGAPChromosome chromosomes", e);
		}
	}
}

class CrowdingComparator implements Comparator<NsgaEntry> {

	@Override
	public int compare(final NsgaEntry entry1, final NsgaEntry entry2) {
		return Double.compare(entry1.crowdingDistance, entry2.crowdingDistance);
	}
}

class Ranker {
	public static boolean dominates(
			final NsgaEntry dominator,
			final NsgaEntry dominee) {
		double[] dominatorScores = dominator.chromosome.getIndividualScores();
		double[] domineeScores = dominee.chromosome.getIndividualScores();

		for (int i = 0; i < dominatorScores.length; i++) {
			if (dominatorScores[i] < domineeScores[i]) return false;
		}

		return true;
	}
}

class IndividualComparator implements Comparator<NsgaEntry> {
	private final int index;

	public IndividualComparator(final int index) {
		this.index = index;
	}

	@Override
	public int compare(final NsgaEntry entry1, final NsgaEntry entry2) {
		return Double.compare(
				entry1.chromosome.getIndividualScores()[index],
				entry2.chromosome.getIndividualScores()[index]);
	}
}

class FrontMap {
	private Map<Integer, List<NsgaEntry>> fronts = new HashMap<Integer, List<NsgaEntry>>();

	public void put(final int rank, final NsgaEntry entry) {
		List<NsgaEntry> front = fronts.get(rank);
		if (front == null) {
			front = new ArrayList<NsgaEntry>();
			fronts.put(rank, front);
		}
		front.add(entry);
	}

	public List<NsgaEntry> get(final int rank) {
		return fronts.get(rank);
	}

	public void clear() {
		fronts.clear();
	}
}
