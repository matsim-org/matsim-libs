/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.scoring.routeDesignScoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * Instantiate and apply RouteDesignScoringFunctions.
 * 
 * @author gleich
 *
 */
public class RouteDesignScoringManager {

	public enum RouteDesignScoreFunctionName {
		stop2StopVsBeelinePenalty, areaBtwStopsVsBeelinePenalty, areaBtwLinksVsBeelinePenalty,
		stopServedMultipleTimesPenalty
	}

	private final List<RouteDesignScoringFunction> scoringFunctions = new ArrayList<>();
	private final static Logger log = LogManager.getLogger(RouteDesignScoringManager.class);

	public final double scoreRouteDesign(PPlan pPlan) {
		double routeDesignScore = 0.0;

		if (pPlan.getLine().getRoutes().size() != 1) {
			log.error("Found a PPlan which has either no TransitRoute or multiple"
					+ "TransitRoutes instead of exactly one TransitRoute. "
					+ "This does not seem to make sense. Aborting!");
			/*
			 * A PPlan without a TransitRoute does not seem to make sense. A PPlan with
			 * multiple TransitRoutes does not seem to make sense either as a PPlan
			 * specifies stopsToBeServed, operation time start and end and the number of
			 * vehicles, so it is not clear how one TransitRoute could differ from other
			 * TransitRoutes of the same plan.
			 */
			new RuntimeException();
		}

		TransitRoute route = pPlan.getLine().getRoutes().values().iterator().next();

		for (RouteDesignScoringFunction scoringFunction : scoringFunctions) {
			routeDesignScore += scoringFunction.getScore(pPlan, route);
		}
		return routeDesignScore;
	}

	public final void init(final PConfigGroup pConfig, Network network) {
		Map<RouteDesignScoreFunctionName, RouteDesignScoreParams> paramMap = pConfig.getRouteDesignScoreParams();

		paramMap.forEach((name, params) -> {
			switch (name) {
			case stop2StopVsBeelinePenalty:
				scoringFunctions.add(new Stop2StopVsTerminiBeelinePenalty(params));
				break;
			case areaBtwStopsVsBeelinePenalty:
				scoringFunctions.add(new AreaBtwStopsVsTerminiBeelinePenalty(params));
				break;
			case areaBtwLinksVsBeelinePenalty:
				scoringFunctions.add(new AreaBtwLinksVsTerminiBeelinePenalty(params, network));
				break;
			case stopServedMultipleTimesPenalty:
				scoringFunctions.add(new StopServedMultipleTimesPenalty(params));
				break;
			default:
				log.error("Unknown RouteDesignScoreFunctionName");
				new RuntimeException("Unknown RouteDesignScoreFunctionName");
			}
		});
	}

	public final boolean isActive() {
		return scoringFunctions.size() > 0;
	}

}
