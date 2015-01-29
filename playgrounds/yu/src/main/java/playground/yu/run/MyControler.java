/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler.java
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

/**
 *
 */
package playground.yu.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.*;

/**
 * temporarily solves the route.getDistance() NaN problem of
 * {@code LinkNetworkRouteImpl}
 *
 * @author yu
 *
 */
public final class MyControler extends Controler {
	private static class MyCharyparNagelScoringFunctionFactory extends
			CharyparNagelScoringFunctionFactory {

		private PlanCalcScoreConfigGroup config;

		public MyCharyparNagelScoringFunctionFactory(
				PlanCalcScoreConfigGroup config, final Network network) {
			super(config, network);
			this.config = config;
		}

		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
			scoringFunctionAccumulator
					.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config)));
			scoringFunctionAccumulator
					.addScoringFunction(new MyLegScoringFunction(person.getSelectedPlan(),
							new CharyparNagelScoringParameters(config), network));
			scoringFunctionAccumulator
					.addScoringFunction(new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config)));
			scoringFunctionAccumulator
					.addScoringFunction(new CharyparNagelAgentStuckScoring(
							new CharyparNagelScoringParameters(config)));
			return scoringFunctionAccumulator;
		}

	}

	private static class MyLegScoringFunction extends CharyparNagelLegScoring {
		private final static Logger log = Logger
				.getLogger(MyLegScoringFunction.class);
		private static int distanceWrnCnt = 0;

		public MyLegScoringFunction(Plan plan,
				CharyparNagelScoringParameters params, final Network network) {
			super(params, network);
		}

		@Override
		protected double calcLegScore(double departureTime, double arrivalTime,
				Leg leg) {
			double tmpScore = 0.0;
			double travelTime = arrivalTime - departureTime; // traveltime in
																// seconds

			/*
			 * we only as for the route when we have to calculate a distance
			 * cost, because route.getDist() may calculate the distance if not
			 * yet available, which is quite an expensive operation
			 */
			double dist = 0.0; // distance in meters

			if (TransportMode.car.equals(leg.getMode())) {
				if (params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
					Route route = leg.getRoute();
					dist = route.getDistance();
					if (Double.isNaN(dist)) {
						dist = RouteUtils.calcDistance((NetworkRoute) route,
								super.network);
					}
					if (distanceWrnCnt < 1) {
						/*
						 * TODO the route-distance does not contain the length
						 * of the first or last link of the route, because the
						 * route doesn't know those. Should be fixed somehow,
						 * but how? MR, jan07
						 */
						/*
						 * TODO in the case of within-day replanning, we cannot
						 * be sure that the distance in the leg is the actual
						 * distance driven by the agent.
						 */
						log.warn("leg distance for scoring computed from plan, not from execution (=events)."
								+ "This is not how it is meant to be, and it will fail for within-day replanning.");
						log.warn("Also means that first and last link are not included.");
						log.warn(Gbl.ONLYONCE);
						distanceWrnCnt++;
					}
				}
				tmpScore += travelTime * params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s
						+ params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * dist;
				tmpScore += params.modeParams.get(TransportMode.car).constant;
			} else if (TransportMode.pt.equals(leg.getMode())) {
				if (params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m != 0.0) {
					dist = leg.getRoute().getDistance();
				}
				tmpScore += travelTime * params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s
						+ params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * dist;
				tmpScore += params.modeParams.get(TransportMode.pt).constant;
			} else if (TransportMode.walk.equals(leg.getMode())
					|| TransportMode.transit_walk.equals(leg.getMode())) {
				if (params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m != 0.0) {
					dist = leg.getRoute().getDistance();
				}
				tmpScore += travelTime
						* params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s
						+ params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * dist;
				tmpScore += params.modeParams.get(TransportMode.walk).constant;
			} else if (TransportMode.bike.equals(leg.getMode())) {
				tmpScore += travelTime
						* params.modeParams.get(TransportMode.bike).marginalUtilityOfTraveling_s;
				tmpScore += params.modeParams.get(TransportMode.bike).constant;
			} else {
				if (params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m != 0.0) {
					dist = leg.getRoute().getDistance();
				}
				// use the same values as for "car"
				tmpScore += travelTime * params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s
						+ params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * dist;
				tmpScore += params.modeParams.get(TransportMode.car).constant;
			}

			return tmpScore;
		}
	}

	/**
	 * @param args
	 */
	public MyControler(String[] args) {
		super(args);
	}

	/**
	 * @param configFileName
	 */
	public MyControler(String configFileName) {
		super(configFileName);
	}

	/**
	 * @param config
	 */
	public MyControler(Config config) {
		super(config);
	}

	/**
	 * @param scenario
	 */
	public MyControler(Scenario scenario) {
		super(scenario);
	}

//	@Override
//	protected ScoringFunctionFactory loadScoringFunctionFactory() {
//		return new MyCharyparNagelScoringFunctionFactory(
//				config.planCalcScore(), network);
//	}

	public static void main(String[] args) {
		Controler controler = new MyControler(args[0]);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.setOverwriteFiles(true);

        controler.setScoringFunctionFactory( new MyCharyparNagelScoringFunctionFactory( controler.getConfig().planCalcScore(), controler.getScenario().getNetwork()) ) ;
		
		controler.run();
	}
}
