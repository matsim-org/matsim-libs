/* *********************************************************************** *
 * project: org.matsim.*
 * TabuBestFitnessSelector.java
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

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
@Deprecated
public class TabuBestFitnessSelector extends NaturalSelectorExt {

	private final static long serialVersionUID = 1L;

	private final List<IChromosome> candidateChromosomes;
	private final TabuComparator tabuMonitor;

	public TabuBestFitnessSelector(
			final Configuration jgapConfig,
			final JointReplanningConfigGroup configGroup
			) throws InvalidConfigurationException {
		super();
		candidateChromosomes = new ArrayList<IChromosome>();
		tabuMonitor = new TabuComparator(jgapConfig, configGroup);
	}

	@Override
	public void empty() {
		this.candidateChromosomes.clear();
	}

	/**
	 * @return false as we do not check, but no "duplicated" chromosome
	 */
	@Override
	public boolean returnsUniqueChromosomes() {
		return false;
	}

	@Override
	protected void add(final IChromosome chromosome) {
		this.candidateChromosomes.add(chromosome);
	}

	/**
	 * Selects the best chromosomes, according to the tabu-penalised fitness.
	 */
	@Override
	protected void selectChromosomes(
			final int howManyToSelect,
			final Population population) {
		this.tabuMonitor.monitor(this.candidateChromosomes);
		Collections.sort(this.candidateChromosomes, this.tabuMonitor);
		Collections.reverse(this.candidateChromosomes);

		for (IChromosome chromosome : 
				this.candidateChromosomes.subList(0, howManyToSelect)) {
			population.addChromosome(chromosome);
		}
	}
}

