/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsSelectorConfigBuilder.java
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsselector;

import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.ConfigurationBuilder;
import playground.thibautd.tsplanoptimizer.framework.IterationNumberMonitor;
import playground.thibautd.tsplanoptimizer.framework.MoveTabuList;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchConfiguration;

/**
 * @author thibautd
 */
public class JointTripsSelectorConfigBuilder implements ConfigurationBuilder {
	private final static int N_ITER = 5;
	private final static int N_TABU = 3;

	private final JointPlan plan;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final PlanAlgorithm optimisationRoutine;

	public JointTripsSelectorConfigBuilder(
			final JointPlan plan,
			final ScoringFunctionFactory scoringFunctionFactory,
			final PlanAlgorithm optimisationRoutine) {
		this.plan = plan;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.optimisationRoutine = optimisationRoutine;
	}

	@Override
	public void buildConfiguration(final TabuSearchConfiguration configuration) {
		Solution initialSolution =
			new JointTripsSelectorSolution(
					plan,
					optimisationRoutine);
		configuration.setInitialSolution( initialSolution );
		configuration.setFitnessFunction(
				new BasicJointPlanFitnessFunction( scoringFunctionFactory ));
		configuration.setEvolutionMonitor( new IterationNumberMonitor( N_ITER ));
		configuration.setTabuChecker( new MoveTabuList( N_TABU ) );
		configuration.setMoveGenerator( new JointTripsSelectorMoveGenerator( initialSolution.getRepresentation().size() ) );
	}
}

