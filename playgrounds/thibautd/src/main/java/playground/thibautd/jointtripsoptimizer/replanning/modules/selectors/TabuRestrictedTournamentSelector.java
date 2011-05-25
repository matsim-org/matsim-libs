/* *********************************************************************** *
 * project: org.matsim.*
 * TabuRestrictedTournamentSelector.java
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

import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Quick implementation of a RTS with tabu capabilities on toggle genes.
 * @author thibautd
 */
public class TabuRestrictedTournamentSelector extends RestrictedTournamentSelector {
	//cannot use delegation:  add protected
	private static final long serialVersionUID = 1L;

	private final TabuMonitor monitor;

	public TabuRestrictedTournamentSelector(
			final JointPlanOptimizerJGAPConfiguration jgapConfig,
			final JointReplanningConfigGroup configGroup,
			final ChromosomeDistanceComparator distanceComparator,
			final TabuMonitor monitor
			) throws InvalidConfigurationException {
		super(jgapConfig, configGroup, distanceComparator);
		this.monitor = monitor;
	}

	@Override
	protected void selectChromosomes(
			final int nToSelect,
			final Population nextGeneration) {
		super.selectChromosomes(nToSelect, nextGeneration);
		this.monitor.updateTabu(nextGeneration);
	}

	@Override
	protected void add(final IChromosome chromosome) {
		this.monitor.correctTabu(chromosome);
		super.add(chromosome);
	}
}
