/* *********************************************************************** *
 * project: org.matsim.*
 * ModeOptimizingMoveConfigBuilder.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import java.util.List;

import playground.thibautd.tsplanoptimizer.framework.CompositeMoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.CompositeTabuChecker;
import playground.thibautd.tsplanoptimizer.framework.ConfigurationBuilder;
import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.ImprovementDelayMonitor;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchConfiguration;

/**
 * @author thibautd
 */
public class ModeOptimizingMoveConfigBuilder implements ConfigurationBuilder {
	private Solution solution = null;
	private final FitnessFunction fitness;
	private final List<String> possibleModes;

	public ModeOptimizingMoveConfigBuilder(
			final FitnessFunction fitness,
			final List<String> possibleModes) {
		this.fitness = fitness;
		this.possibleModes = possibleModes;
	}

	@Override
	public void buildConfiguration(final TabuSearchConfiguration configuration) {
		configuration.setInitialSolution( solution );
		configuration.setFitnessFunction( fitness );

		configuration.setEvolutionMonitor(
				new ImprovementDelayMonitor(
					100,
					Integer.MAX_VALUE ));

		configuration.setMoveGenerator( 
				new AllPossibleModesMovesGenerator(
					solution,
					possibleModes) );

		configuration.setTabuChecker( new ModeMovesTabuList( 10 ) );
	}

	public void setSolution(final Solution solution) {
		this.solution = solution;
	}
}

