/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatFitnessFunctionFactoryImpl.java
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
package playground.thibautd.planomat.basic;

import java.util.Set;

import org.jgap.Configuration;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatFitnessFunction;
import playground.thibautd.planomat.api.PlanomatFitnessFunctionFactory;
import playground.thibautd.planomat.basic.algorithms.LegTravelTimeEstimatorWrapper;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;

/**
 * Implementation of a {@link PlanomatFitnessFunctionFactory} which returns
 * instances of {@link PlanomatFitnessFunctionImpl} using a {@link LegTravelTimeEstimatorWrapper}
 * as postdecoding algorithm.
 *
 * @author thibautd
 */
public class PlanomatFitnessFunctionFactoryImpl implements PlanomatFitnessFunctionFactory {
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final double scenarioDuration;
	private final Set<String> allowedModes;
	private final PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourLevel;
	private final LegTravelTimeEstimatorFactory travelTimeEstimatorFactory;
	private final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;
	private final PlanomatConfigGroup.RoutingCapability routingCapability;
	private final PlansCalcRoute router;
	private final Network network;

	public PlanomatFitnessFunctionFactoryImpl(
			final ScoringFunctionFactory scoringFunctionFactory,
			final Planomat2ConfigGroup config,
			final PlansCalcRoute router,
			final Network network,
			final LegTravelTimeEstimatorFactory travelTimeEstimatorFactory) {
		this( scoringFunctionFactory,
				config.getScenarioDuration(),
				config.getPossibleModes(),
				config.getTripStructureAnalysisLayer(),
				config.getSimLegInterpretation(),
				config.getRoutingCapability(),
				router,
				network,
				travelTimeEstimatorFactory);
	}

	/**
	 * @param scoringFunctionFactory
	 * @param scenarioDuration
	 * @param allowedModes
	 * @param subtourLevel
	 * @param simLegInterpretation
	 * @param routingCapability
	 * @param router
	 * @param network
	 * @param travelTimeEstimatorFactory
	 */
	public PlanomatFitnessFunctionFactoryImpl(
			final ScoringFunctionFactory scoringFunctionFactory,
			final double scenarioDuration,
			final Set<String> allowedModes,
			final PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourLevel,
			final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation,
			final PlanomatConfigGroup.RoutingCapability routingCapability,
			final PlansCalcRoute router,
			final Network network,
			final LegTravelTimeEstimatorFactory travelTimeEstimatorFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scenarioDuration = scenarioDuration;
		this.allowedModes = allowedModes;
		this.subtourLevel = subtourLevel;
		this.travelTimeEstimatorFactory = travelTimeEstimatorFactory;;
		this.simLegInterpretation = simLegInterpretation;
		this.routingCapability = routingCapability;
		this.router = router;
		this.network = network;
	}

	@Override
	public PlanomatFitnessFunction createFitnessFunction(
			final Configuration jgapConfig,
			final Plan plan,
			final ActivityWhiteList whiteList) {
		LegTravelTimeEstimator estimator =
			travelTimeEstimatorFactory.getLegTravelTimeEstimator(
				plan,
				simLegInterpretation,
				routingCapability,
				router,
				network);

		PlanomatFitnessFunctionImpl fitnessFunction =
			new PlanomatFitnessFunctionImpl(
					scoringFunctionFactory,
					jgapConfig,
					scenarioDuration,
					whiteList,
					allowedModes,
					subtourLevel,
					plan);

		fitnessFunction.addPostDecodingPlanAlgorithm(
				new LegTravelTimeEstimatorWrapper( plan , estimator ) );

		return fitnessFunction;
	}
}

