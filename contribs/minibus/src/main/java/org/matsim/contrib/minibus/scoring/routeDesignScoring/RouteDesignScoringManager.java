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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * Instantiate and apply RouteDesignScoringFunctions.
 * 
 * @author gregor
 *
 */
public class RouteDesignScoringManager {

	public static enum RouteDesignScoreFunctionName {
		stop2StopVsBeelinePenalty, areaVsBeelinePenalty
	}

	List<RouteDesignScoringFunction> scoringFunctions = new ArrayList<>();
	Map<RouteDesignScoreFunctionName, RouteDesignScoreParams> paramMap = new HashMap<>();
	final static Logger log = Logger.getLogger(RouteDesignScoringManager.class);
	boolean isActive = false;

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

	public final void init(PConfigGroup pConfig) {
		paramMap = pConfig.getRouteDesignScoreParams();

		paramMap.forEach((name, params) -> {
			switch (name) {
			case stop2StopVsBeelinePenalty:
				scoringFunctions.add(new Stop2StopVsTerminiBeelinePenalty(params));
				break;
			case areaVsBeelinePenalty:
				scoringFunctions.add(new AreaVsTerminiBeelinePenalty(params));
				break;
			default:
				log.error("Unknown RouteDesignScoreFunctionName");
				new RuntimeException("Unknown RouteDesignScoreFunctionName");
			}
		});

		if (scoringFunctions.size() > 0) {
			isActive = true;
		}
	}

	public final boolean isActive() {
		return isActive;
	}

}
