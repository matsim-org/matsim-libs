/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerLegTravelTimeEstimatorFactory.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

/**
 * @author thibautd
 */
public class JointPlanOptimizerLegTravelTimeEstimatorFactory {
	private static final Log log =
		LogFactory.getLog(JointPlanOptimizerLegTravelTimeEstimatorFactory.class);
	private static boolean logWrappingWarning = true;

	private final TravelTime travelTime;
	private final DepartureDelayAverageCalculator depDelay;

	public JointPlanOptimizerLegTravelTimeEstimatorFactory(
			final TravelTime travelTime,
			final  DepartureDelayAverageCalculator depDelay) {
		TravelTime effectiveTravelTime;

		try {
			effectiveTravelTime = new TravelTimeEstimatorWrapper((TravelTimeCalculator) travelTime);
		} catch (ClassCastException e) {
			if (logWrappingWarning) {
				logWrappingWarning = false;
				log.warn("Not wrapping travel time estimator for Joint Plan Optimisation. On big networks, this can be quite unefficient!");
			}
			effectiveTravelTime = travelTime;
		}

		this.travelTime = effectiveTravelTime;
		this.depDelay = depDelay;
	}

	//TODO: pass everything except the plan by the constructor?
	public LegTravelTimeEstimator getLegTravelTimeEstimator(
			final Plan plan,
			final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation,
			final PlanomatConfigGroup.RoutingCapability routingCapability,
			final PlansCalcRoute routingAlgorithm,
			final Network network) {
		return new ODBasedFixedRouteLegTravelTimeEstimator(
				plan,
				this.travelTime,
				this.depDelay,
				routingAlgorithm,
				simLegInterpretation,
				network);
	}
}

