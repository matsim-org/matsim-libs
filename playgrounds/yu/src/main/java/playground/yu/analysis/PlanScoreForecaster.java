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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;

import playground.yu.utils.DebugTools;

/**
 * approximately forecasts the score of a plan, that was newly created by e.g.
 * ReRoute or TimeAllocationMutator
 *
 * @author yu
 *
 */
public class PlanScoreForecaster {
	private PlanImpl plan, oldSelected;
	private NetworkImpl net;
	private TravelTime ttc;
	private CharyparNagelScoringConfigGroup scoring;
	private double score = 0.0, betaTraveling, betaPerforming, betaDist,
			firstActEndTime, attrTraveling = 0.0, attrPerforming = 0.0,
			attrDistance = 0.0;

	public PlanScoreForecaster(Plan plan, NetworkImpl net, TravelTime ttc,
			CharyparNagelScoringConfigGroup scoring, double betaTraveling,
			double betaPerforming) {
		this.plan = (PlanImpl) plan;
		this.net = net;
		this.ttc = ttc;
		this.scoring = scoring;
		this.betaTraveling = betaTraveling;
		this.betaPerforming = betaPerforming;
	}

	public PlanScoreForecaster(Plan selectedPlan, Plan oldSelected,
			NetworkImpl net2, TravelTime tt,
			CharyparNagelScoringConfigGroup scoring2, double d, double e) {
		this(selectedPlan, net2, tt, scoring2, d, e);
		this.oldSelected = (PlanImpl) oldSelected;
	}

	public PlanScoreForecaster(Plan selectedPlan, Plan oldSelected,
			NetworkImpl net, TravelTime tt,
			CharyparNagelScoringConfigGroup scoring, double betaTraveling,
			double betaPerforming, double betaDist) {
		this(selectedPlan, oldSelected, net, tt, scoring, betaTraveling,
				betaPerforming);
		this.betaDist = betaDist;
	}

	public double getPlanScore() {
		// boolean fistActDone = false;
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				this.handleAct(act);
			} else if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				this.handleLeg(leg);
			}
		}
		return this.score;
	}

	public double getAttrPerforming() {
		return attrPerforming;
	}

	public double getAttrTraveling() {
		return attrTraveling;
	}

	public double getAttrDistance() {
		return attrDistance;
	}

	/**
	 * believes only legDepartureTime of newly created Plans
	 *
	 * @param leg
	 */
	private void handleLeg(Leg leg) {
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		double travelTime_s = 0.0, departTime = leg.getDepartureTime(), legDist = route
				.getDistance();
		if (departTime < 0) {
			Activity preAct = (this.plan).getPreviousActivity(leg);
			departTime = preAct.getEndTime();
			if (departTime < 0 && oldSelected != null) {
				Leg oldLeg = (Leg) oldSelected.getPlanElements().get(
						(plan).getActLegIndex(leg));
				departTime = oldLeg.getDepartureTime();
			}
		}

		Map<Id, LinkImpl> links = this.net.getLinks();
		for (Id linkId : route.getLinkIds()) {
			travelTime_s += ttc.getLinkTravelTime(links.get(linkId), departTime
					+ travelTime_s);
		}
		travelTime_s += this.ttc.getLinkTravelTime(links.get(route
				.getEndLinkId()), departTime + travelTime_s);
		double attrTravelTime = travelTime_s / 3600.0;
		this.attrTraveling += attrTravelTime;

		if (Double.isNaN(legDist) || legDist < 0) {
			legDist = 0;
			for (Id linkId : route.getLinkIds()) {
				legDist += links.get(linkId).getLength();
			}
			legDist += links.get(route.getEndLinkId()).getLength();
		}
		double attrDist = legDist / 1000.0;
		this.attrDistance += attrDist;

		score += this.betaTraveling * attrTravelTime/* [h] */+ this.betaDist
				* 0.12 * attrDist/* [km] */;
		if (Double.isNaN(score))
			throw new RuntimeException(PlanScoreForecaster.class.getName()
					+ "\t" + DebugTools.getLineNumber(new Exception())
					+ "\tutil/score is a NaN.");

		((LegImpl) leg).setArrivalTime(departTime + travelTime_s);
	}

	private void handleAct(Activity act) {
		ActivityParams actParams = this.scoring
				.getActivityParams(act.getType());

		LegImpl preLeg = null, nextLeg = null;
		if (!plan.getFirstActivity().equals(act))// not the first one
			preLeg = (LegImpl) plan.getPreviousLeg(act);
		if (!plan.getLastActivity().equals(act))// not the last one
			nextLeg = (LegImpl) plan.getNextLeg(act);

		double actStartTime = -1, actEndTime = -1;
		if (preLeg != null) {
			actStartTime = preLeg.getArrivalTime();

			act.setStartTime(actStartTime);
		}
		if (nextLeg != null) {
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
					throw new RuntimeException(PlanScoreForecaster.class
							.getName()
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
		if (Double.isNaN(score))
			throw new RuntimeException(PlanScoreForecaster.class.getName()
					+ "\t" + DebugTools.getLineNumber(new Exception())
					+ "\tutil/score is a NaN.");

	}

	public static void main(String[] args) {

		String configFilename = "../integration-parameterCalibration/test/matsim/configDummy.xml", //
		netFilename = "../integration-parameterCalibration/test/network.xml", //
		eventsFilename = "../integration-parameterCalibration/test/matsim/outputReplanning/ITERS/it.100/100.events.txt.gz", //
		popFilename = "../integration-parameterCalibration/test/matsim/outputReplanning/output_plans.xml.gz";

		Scenario sc = new ScenarioImpl();

		Config cf = sc.getConfig();
		new MatsimConfigReader(cf).readFile(configFilename);
		CharyparNagelScoringConfigGroup scoring = cf.charyparNagelScoring();

		NetworkLayer net = (NetworkLayer) sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);

		Population pop = sc.getPopulation();
		new MatsimPopulationReader(sc).readFile(popFilename);

		TravelTimeCalculator ttc = new TravelTimeCalculatorFactoryImpl()
				.createTravelTimeCalculator(net, cf.travelTimeCalculator());

		EventsManager events = new EventsManagerImpl();
		events.addHandler(ttc);
		new EventsReaderTXTv1(events).readFile(eventsFilename);

		for (Person ps : pop.getPersons().values()) {
			// for (Plan pl : ps.getPlans()) {
			Plan pl = ps.getSelectedPlan();
			double score = new PlanScoreForecaster(pl, net, ttc,
					scoring, -6.0, 6.0).getPlanScore();
			if (pl.getScore().intValue() != (int) score)
				System.out.println("person\t" + ps.getId() + "\tplan\t" + pl
						+ "\tutil\t" + score);
			// }
		}

	}
}
