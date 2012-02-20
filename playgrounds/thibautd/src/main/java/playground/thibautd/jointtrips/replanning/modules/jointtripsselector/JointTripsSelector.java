/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsSelector.java
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

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.ConfigurationBuilder;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchRunner;

/**
 * @author thibautd
 */
public class JointTripsSelector implements PlanAlgorithm {
	private static final Logger log =
		Logger.getLogger(JointTripsSelector.class);

	private static final boolean DEBUG = false;
	private final PlanAlgorithm optimisationSubroutine;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final TabuSearchRunner runner = new TabuSearchRunner();

	public JointTripsSelector(
			final PlanAlgorithm optimisationRoutine,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.optimisationSubroutine = optimisationRoutine;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	@Override
	public void run(final Plan plan) {
		ConfigurationBuilder builder =
			new JointTripsSelectorConfigBuilder(
					(JointPlan) plan,
					scoringFunctionFactory,
					optimisationSubroutine);

		Solution bestSolution = runner.runTabuSearch( builder );

		// problem: it re-optimises the plan (and it needs to)!
		((JointPlan) plan).resetFromPlan( (JointPlan) bestSolution.getRepresentedPlan() );
		((JointPlan) plan).resetScores();

		if (DEBUG) {
			log.debug( "resulting plan: "+plan );
		}
	}
}

