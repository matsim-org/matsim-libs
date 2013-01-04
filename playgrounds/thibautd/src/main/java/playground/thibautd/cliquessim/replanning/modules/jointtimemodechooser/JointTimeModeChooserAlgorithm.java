/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeModeChooserAlgorithm.java
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
package playground.thibautd.cliquessim.replanning.modules.jointtimemodechooser;

import java.util.Random;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.cliquessim.config.JointTimeModeChooserConfigGroup;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchConfiguration;
import static playground.thibautd.tsplanoptimizer.framework.TabuSearchRunner.runTabuSearch;
import playground.thibautd.tsplanoptimizer.timemodechooser.traveltimeestimation.EstimatorTripRouterFactory;

/**
 * @author thibautd
 */
public class JointTimeModeChooserAlgorithm implements PlanAlgorithm {
	private final StatisticsCollector statsCollector;
	private final Random random;

	private final DepartureDelayAverageCalculator delay;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Scenario scenario;

	private final TravelTime travelTimeCalculator;
	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	private final TripRouterFactory exactRouterFactory;

	public JointTimeModeChooserAlgorithm(
			final Random random,
			final StatisticsCollector statsCollector,
			final DepartureDelayAverageCalculator delay,
			final Controler controler) {
		this( random,
				statsCollector,
				delay,
				controler.getScenario(),
				controler.getScoringFunctionFactory(),
				controler.getTravelTimeCalculator(),
				controler.getLeastCostPathCalculatorFactory(),
				controler.getTripRouterFactory());
	}

	public JointTimeModeChooserAlgorithm(
			final Random random,
			final StatisticsCollector statsCollector,
			final DepartureDelayAverageCalculator delay,
			final Scenario scenario,
			final ScoringFunctionFactory scoringFunctionFactory,
			final TravelTime travelTimeCalculator,
			final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
			final TripRouterFactory tripRouterFactory) {
		this.random = random;
		this.statsCollector = statsCollector;
		this.delay = delay;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scenario = scenario;
		this.travelTimeCalculator = travelTimeCalculator;
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
		this.exactRouterFactory = tripRouterFactory;
	}

	@Override
	public void run(final Plan plan) {
		JointPlan jointPlan = (JointPlan) plan;

		TripRouterFactory estimatorRouterFactory =
			getAndTuneTripRouterFactory(
					plan,
					delay,
					scenario,
					travelTimeCalculator,
					leastCostPathCalculatorFactory,
					exactRouterFactory );

		if (statsCollector != null) {
			statsCollector.notifyTripRouterFactory( estimatorRouterFactory );
		}

		JointTimeModeChooserConfigGroup config = (JointTimeModeChooserConfigGroup)
			scenario.getConfig().getModule( JointTimeModeChooserConfigGroup.GROUP_NAME );
		JointTimeModeChooserConfigBuilder builder =
			new JointTimeModeChooserConfigBuilder(
					random,
					jointPlan,
					config,
					scoringFunctionFactory,
					null); // XXX: no debug plots anymore.
					//config.isDebugMode() ? controler.getControlerIO().getIterationPath( controler.getIterationNumber() ) : null);

		// first run without synchro
		TabuSearchConfiguration configuration = new TabuSearchConfiguration();
		Solution initialSolution = new JointTimeModeChooserSolution(
						(JointPlan) plan,
						estimatorRouterFactory.createTripRouter());
		builder.buildConfiguration(
				false,
				initialSolution,
				configuration );
		Solution bestSolution = runTabuSearch( configuration , initialSolution );

		if (jointPlan.getIndividualPlans().size() > 1) {
			// then start at the found solution and synchronize
			configuration = new TabuSearchConfiguration();
			builder.buildConfiguration(
					true,
					bestSolution,
					configuration );
			bestSolution = runTabuSearch( configuration , bestSolution );
		}

		// two goals here:
		// 1- the side effect: getRepresentedPlan sets the plan to the represented state
		// 2- the obvious check
		if (bestSolution.getRepresentedPlan() != plan) {
			throw new RuntimeException( "the returned plan is not the input plan" );
		}
	}

	private static TripRouterFactory getAndTuneTripRouterFactory(
			final Plan plan,
			final DepartureDelayAverageCalculator delay,
			final Scenario scenario,
			final TravelTime travelTimeCalculator,
			final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
			final TripRouterFactory tripRouterFactory) {
		return new EstimatorTripRouterFactory(
				plan,
				scenario.getPopulation().getFactory(),
				scenario.getNetwork(),
				travelTimeCalculator,
				leastCostPathCalculatorFactory,
				((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory(),
				scenario.getConfig().scenario().isUseTransit() ?
					scenario.getTransitSchedule() :
					null,
				scenario.getConfig().plansCalcRoute(),
				delay,
				tripRouterFactory);
	}
}
