/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleLegTravelTimeEstimatorFactory.java
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
package playground.thibautd.agentsmating.logitbasedmating.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

/**
 * Creates travel tim estimators.
 * This is mainly a wrapper around the default factory, with all non-plan
 * dependant parameters passed in the constructor rather than the create method.
 * @author thibautd
 */
public class SimpleLegTravelTimeEstimatorFactory {
	private final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;
	private final PlanomatConfigGroup.RoutingCapability routingCapability;
	private final PlansCalcRoute routingAlgorithm;
	private final Network network;

	private final LegTravelTimeEstimatorFactory factory;

	public SimpleLegTravelTimeEstimatorFactory (
			final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation,
			final PlanomatConfigGroup.RoutingCapability routingCapability,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final TravelTime travelTime,
			final DepartureDelayAverageCalculator delay) {
		this.factory = new LegTravelTimeEstimatorFactory( travelTime, delay );
		this.simLegInterpretation = simLegInterpretation;
		this.routingCapability = routingCapability;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
	}

	public LegTravelTimeEstimator createLegTravelTimeEstimator(
			final Plan plan ) {
		return factory.getLegTravelTimeEstimator(
				plan,
				simLegInterpretation,
				routingCapability,
				routingAlgorithm,
				network);
	}
}

