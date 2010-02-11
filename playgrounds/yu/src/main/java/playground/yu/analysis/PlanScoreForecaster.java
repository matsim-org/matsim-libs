/* *********************************************************************** *
 * project: org.matsim.*
 * PlanScoreForecaster.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.analysis;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * approximately forecasts the score of a plan, that was newly created by e.g.
 * ReRoute or TimeAllocationMutator
 * 
 * @author yu
 * 
 */
public class PlanScoreForecaster {
	private PlanImpl plan;
	private NetworkLayer net;
	private TravelTimeCalculator ttc;
	private CharyparNagelScoringConfigGroup scoring;
	private double score = 0.0, betaTraveling, betaPerforming;

	public PlanScoreForecaster(PlanImpl plan, NetworkLayer net,
			TravelTimeCalculator ttc, CharyparNagelScoringConfigGroup scoring,
			double betaTraveling, double betaPerforming) {
		this.plan = plan;
		this.net = net;
		this.ttc = ttc;
		this.scoring = scoring;
		this.betaTraveling = betaTraveling;
		this.betaPerforming = betaPerforming;
	}

	public double getPlanScore(PlanImpl plan) {
		boolean fistActDone = false;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				this.handleAct(act);
			} else if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				this.handleLeg(leg);
			}
		}
		return 0.0;
	}

	/**
	 * believes only legDepartureTime of newly created Plans
	 * 
	 * @param leg
	 */
	private void handleLeg(LegImpl leg) {
		double travelTime_s = 0.0, departTime = leg.getDepartureTime();
		NetworkRouteWRefs route = (NetworkRouteWRefs) leg.getRoute();
		Map<Id, LinkImpl> links = this.net.getLinks();
		for (Id linkId : route.getLinkIds()) {
			travelTime_s += ttc.getLinkTravelTime(links.get(linkId), departTime
					+ travelTime_s);
		}
		travelTime_s += this.ttc.getLinkTravelTime(links.get(route
				.getEndLinkId()), departTime + travelTime_s);

		score += this.betaTraveling * travelTime_s / 3600.0/* [h] */;

		leg.setArrivalTime(departTime + travelTime_s);
	}

	private void handleAct(ActivityImpl act) {
		ActivityParams actParams = this.scoring
				.getActivityParams(act.getType());

		double actStartTime = plan.getPreviousLeg(act).getArrivalTime(), actEndTime = plan
				.getNextLeg(act).getDepartureTime();

		if (plan.getFirstActivity().getType().equals(
				plan.getLastActivity().getType())) {
			if (plan.getFirstActivity().equals(act))
				actStartTime = 0.0;
			if (plan.getLastActivity().equals(act))
				actEndTime = 24.0 * 3600.0 - 1.0;
		}

		double openTime = actParams.getOpeningTime(), closeTime = actParams
				.getClosingTime(); //
		if (act.getType().startsWith("h")) {
			openTime = 0.0;
			closeTime = 24.0 * 3600.0 - 1.0;
		}

		double typicalDuration_h = actParams.getTypicalDuration() / 3600.0, zeroUtilityDuration_h// [h]
		= typicalDuration_h
				* Math.exp(-10.0 / typicalDuration_h / actParams.getPriority()), //

		actStart = actStartTime, actEnd = actEndTime;

		if (openTime >= 0 && actStartTime < openTime)
			actStart = openTime;
		if (closeTime >= 0 && closeTime < actEndTime)
			actEnd = closeTime;
		if (openTime >= 0 && closeTime >= 0
				&& (openTime > actEndTime || closeTime < actStartTime)) {
			// agent could not perform action
			actStart = actEndTime;
			actEnd = actEndTime;
		}
		double performingTime_h = (actEnd - actStart) / 3600.0;
		double durAttr = typicalDuration_h
				* Math.log(performingTime_h / zeroUtilityDuration_h);
		score += this.betaPerforming * Math.max(durAttr, 0);
	}
	public static void main(String args[]){
		
	}
}
