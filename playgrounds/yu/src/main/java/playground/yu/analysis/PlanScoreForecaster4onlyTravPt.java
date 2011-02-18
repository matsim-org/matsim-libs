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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;

import playground.yu.utils.DebugTools;

/**
 * approximately forecasts the score of a plan, that was newly created by e.g.
 * ReRoute or TimeAllocationMutator
 * 
 * @author yu
 * 
 */
public class PlanScoreForecaster4onlyTravPt {
	private PlanImpl plan, oldSelected;
	private NetworkImpl net;
	private TravelTime ttc;
	private PlanCalcScoreConfigGroup scoring;
	private double score = 0.0, betaTraveling, betaTravelingPt, betaPerforming,
	// betaDist,
			firstActEndTime, attrTraveling = 0.0, attrPerforming = 0.0
			// ,attrDistance = 0.0
			;

	public PlanScoreForecaster4onlyTravPt(Plan plan, NetworkImpl net,
			TravelTime tt, PlanCalcScoreConfigGroup scoring,
			double betaTravelingPt) {
		this.plan = (PlanImpl) plan;
		this.net = net;
		this.ttc = tt;
		this.scoring = scoring;
		this.betaTraveling = scoring.getTraveling_utils_hr();
		this.betaTravelingPt = betaTravelingPt;
		this.betaPerforming = scoring.getPerforming_utils_hr();
	}

	public PlanScoreForecaster4onlyTravPt(Plan selectedPlan, Plan oldSelected,
			NetworkImpl net, TravelTime tt,
			PlanCalcScoreConfigGroup scoring, double d) {
		this(selectedPlan, net, tt, scoring, d);
		this.oldSelected = (PlanImpl) oldSelected;
	}

	public PlanScoreForecaster4onlyTravPt(Plan selectedPlan, Plan oldSelected,
			NetworkImpl net, TravelTime tt,
			PlanCalcScoreConfigGroup scoring, double d0, double d1) {

	}

	public double getPlanScore() {
		// boolean fistActDone = false;
		// for (PlanElement pe : this.plan.getPlanElements()) {
		// if (pe instanceof ActivityImpl) {
		// ActivityImpl act = (ActivityImpl) pe;
		// this.handleAct(act);
		// } else if (pe instanceof LegImpl) {
		// LegImpl leg = (LegImpl) pe;
		// this.handleLeg(leg);
		// }
		// }
		// return this.score;
		return this.oldSelected.getScore();
	}

	public double getAttrPerforming() {
		return attrPerforming;
	}

	public double getAttrTraveling() {
		return attrTraveling;
	}

	// public double getAttrDistance() {
	// return attrDistance;
	// }

	/**
	 * believes only legDepartureTime of newly created Plans
	 * 
	 * @param leg
	 */
	private void handleLeg(LegImpl leg) {

		double travelTime_s = 0.0, departTime = leg.getDepartureTime()
		// , legDist = route.getDistance()
		;

		if (departTime < 0) {
			Activity preAct = this.plan.getPreviousActivity(leg);
			departTime = preAct.getEndTime();
		}
		if (departTime < 0 && oldSelected != null) {
			LegImpl oldLeg = (LegImpl) oldSelected.getPlanElements().get(
					plan.getActLegIndex(leg));
			departTime = oldLeg.getDepartureTime();
			if (departTime < 0)
				departTime = oldSelected.getPreviousActivity(oldLeg)
						.getEndTime();

			if (departTime < 0) {
				ActivityImpl oldPreAct = (ActivityImpl) oldSelected
						.getPreviousActivity(oldLeg);
				departTime = oldPreAct.getStartTime() + oldPreAct.getMaximumDuration();
			}
		}
		if (departTime < 0) {
			this.score = this.oldSelected.getScore();
			return;
		}
		Route route = leg.getRoute();
		if (route instanceof NetworkRoute) {
			NetworkRoute netRoute = (NetworkRoute) route;
			Map<Id, Link> links = this.net.getLinks();
			for (Id linkId : netRoute.getLinkIds()) {
				travelTime_s += ttc.getLinkTravelTime(links.get(linkId),
						departTime + travelTime_s);
			}
			travelTime_s += this.ttc.getLinkTravelTime(links.get(netRoute
					.getEndLinkId()), departTime + travelTime_s);
		} else if (route instanceof GenericRoute)
			travelTime_s += route.getTravelTime();

		double attrTravelTime = travelTime_s / 3600.0;
		this.attrTraveling += attrTravelTime;

		// if (Double.isNaN(legDist) || legDist < 0) {
		// legDist = 0;
		// for (Id linkId : route.getLinkIds()) {
		// legDist += links.get(linkId).getLength();
		// }
		// legDist += links.get(route.getEndLinkId()).getLength();
		// }
		// double attrDist = legDist / 1000.0;
		// this.attrDistance += attrDist;

		double betaTrav = 0.0;
		if (leg.getMode().equals(TransportMode.car))
			betaTrav = this.betaTraveling;
		else if (leg.getMode().equals(TransportMode.pt))
			betaTrav = this.betaTravelingPt;

		score += betaTrav * attrTravelTime/* [h] */;
		// System.out.println("SCORE:\tbetaTrav\t=\t" + betaTrav
		// + "\t*\tattrTravTime\t=\t" + attrTravelTime + " [h]\t=\t"
		// + betaTrav * attrTravelTime + ";\tscore\t=\t" + score);

		if (Double.isNaN(score))
			throw new RuntimeException(PlanScoreForecaster4onlyTravPt.class
					.getName()
					+ "\t"
					+ DebugTools.getLineNumber(new Exception())
					+ "\tutil/score is a NaN.");

		leg.setArrivalTime(departTime + travelTime_s);
	}

	private void handleAct(ActivityImpl act) {
		ActivityParams actParams = this.scoring
				.getActivityParams(act.getType());

		LegImpl preLeg = null, nextLeg = null;
		if (!plan.getFirstActivity().equals(act))// not the first one
			preLeg = (LegImpl) plan.getPreviousLeg(act);
		if (!plan.getLastActivity().equals(act))// not the last one
			nextLeg = (LegImpl) plan.getNextLeg(act);

		double actStartTime = act.getStartTime(), actEndTime = act.getEndTime();
		if (actStartTime == Double.NEGATIVE_INFINITY)
			if (preLeg != null) {
				actStartTime = preLeg.getArrivalTime();

				act.setStartTime(actStartTime);
			}
		if (actEndTime == Double.NEGATIVE_INFINITY)
			actEndTime = act.getStartTime() + act.getMaximumDuration();
		else if (nextLeg != null) {
			actEndTime = nextLeg.getDepartureTime();
			if (actEndTime < 0 && oldSelected != null) {
				LegImpl oldNextLeg = (LegImpl) oldSelected.getPlanElements()
						.get(plan.getActLegIndex(nextLeg));
				actEndTime = oldNextLeg.getDepartureTime();
			}
			act.setEndTime(actEndTime);
		}

		if (plan.getFirstActivity().getType().equals(
				plan.getLastActivity().getType())/* home */) {
			if (plan.getFirstActivity().equals(act))/* first home */{
				this.firstActEndTime = actEndTime + 3600.0 * 24.0;
				return;
			}
		}

		double typicalDuration_h = actParams.getTypicalDuration() / 3600.0, //
		zeroUtilityDuration_h = typicalDuration_h
				* Math.exp(-10.0 / typicalDuration_h / actParams.getPriority());

		double actStart = actStartTime, actEnd;

		if (!plan.getFirstActivity().equals(act)
				&& !plan.getLastActivity().equals(act)) {// not home
			double openTime = actParams.getOpeningTime(), //
			closeTime = actParams.getClosingTime();
			actEnd = actEndTime;
			if (openTime >= 0 && actStartTime < openTime)
				actStart = openTime;
			if (closeTime >= 0 && closeTime < actEndTime)
				actEnd = closeTime;
			if (openTime >= 0 && closeTime >= 0
					&& (openTime > actEndTime || closeTime < actStartTime)) {
				// agent could not perform action
				actStart = actEndTime;
				actEnd = actEndTime;
				if (Double.isNaN(actStart) || Double.isNaN(actEnd))
					throw new RuntimeException(
							PlanScoreForecaster4onlyTravPt.class.getName()
									+ "\t"
									+ DebugTools.getLineNumber(new Exception())
									+ "\tutil/score is a NaN.");
			}
		} else /* maybe home */{
			actEnd = this.firstActEndTime;
		}

		double performingTime_h = (actEnd - actStart) / 3600.0;
		performingTime_h = Math.max(performingTime_h, 0.0);
		double durAttr = typicalDuration_h
				* Math.log(performingTime_h / zeroUtilityDuration_h);

		durAttr = Math.max(durAttr, 0);
		this.attrPerforming += durAttr;

		score += this.betaPerforming * durAttr;

		// System.out.println("SCORE:\tbetaPerforming\t=\t" + betaPerforming
		// + "\t*\tdurAttr\t=\t" + durAttr + " [h]\t=\t" + betaPerforming
		// * durAttr + ";\tscore\t=\t" + score);
		if (Double.isNaN(score))
			throw new RuntimeException(PlanScoreForecaster4onlyTravPt.class
					.getName()
					+ "\t"
					+ DebugTools.getLineNumber(new Exception())
					+ "\tutil/score is a NaN.");

	}
}
