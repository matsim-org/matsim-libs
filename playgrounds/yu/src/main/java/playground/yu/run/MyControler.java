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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * temporarily solves the route.getDistance() NaN problem of
 * {@code LinkNetworkRouteImpl}
 *
 * @author yu
 *
 */
public class MyControler extends Controler {
	private static class MyCharyparNagelScoringFunctionFactory extends
			CharyparNagelScoringFunctionFactory {
		private final Network network;

		public MyCharyparNagelScoringFunctionFactory(
				PlanCalcScoreConfigGroup config, final Network network) {
			super(config);
			this.network = network;
		}

		@Override
		public ScoringFunction createNewScoringFunction(Plan plan) {
			ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
			scoringFunctionAccumulator
					.addScoringFunction(new ActivityScoringFunction(getParams()));
			scoringFunctionAccumulator
					.addScoringFunction(new MyLegScoringFunction(plan,
							getParams(), network));
			scoringFunctionAccumulator
					.addScoringFunction(new MoneyScoringFunction(getParams()));
			scoringFunctionAccumulator
					.addScoringFunction(new AgentStuckScoringFunction(
							getParams()));
			return scoringFunctionAccumulator;
		}

	}

	private static class MyLegScoringFunction extends LegScoringFunction {
		private final static Logger log = Logger
				.getLogger(MyLegScoringFunction.class);
		private static int distanceWrnCnt = 0;
		private final Network network;

		public MyLegScoringFunction(Plan plan,
				CharyparNagelScoringParameters params, final Network network) {
			super(params);
			this.network = network;
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
				if (params.marginalUtilityOfDistanceCar_m != 0.0) {
					Route route = leg.getRoute();
					dist = route.getDistance();
					if (Double.isNaN(dist)) {
						dist = RouteUtils.calcDistance((NetworkRoute) route,
								network);
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
				tmpScore += travelTime * params.marginalUtilityOfTraveling_s
						+ params.marginalUtilityOfDistanceCar_m * dist;
				tmpScore += params.constantCar;
			} else if (TransportMode.pt.equals(leg.getMode())) {
				if (params.marginalUtilityOfDistancePt_m != 0.0) {
					dist = leg.getRoute().getDistance();
				}
				tmpScore += travelTime * params.marginalUtilityOfTravelingPT_s
						+ params.marginalUtilityOfDistancePt_m * dist;
				tmpScore += params.constantPt;
			} else if (TransportMode.walk.equals(leg.getMode())
					|| TransportMode.transit_walk.equals(leg.getMode())) {
				if (params.marginalUtilityOfDistanceWalk_m != 0.0) {
					dist = leg.getRoute().getDistance();
				}
				tmpScore += travelTime
						* params.marginalUtilityOfTravelingWalk_s
						+ params.marginalUtilityOfDistanceWalk_m * dist;
				tmpScore += params.constantWalk;
			} else if (TransportMode.bike.equals(leg.getMode())) {
				tmpScore += travelTime
						* params.marginalUtilityOfTravelingBike_s;
				tmpScore += params.constantBike;
			} else {
				if (params.marginalUtilityOfDistanceCar_m != 0.0) {
					dist = leg.getRoute().getDistance();
				}
				// use the same values as for "car"
				tmpScore += travelTime * params.marginalUtilityOfTraveling_s
						+ params.marginalUtilityOfDistanceCar_m * dist;
				tmpScore += params.constantCar;
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

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new MyCharyparNagelScoringFunctionFactory(
				config.planCalcScore(), network);
	}

	public static void main(String[] args) {
		Controler controler = new MyControler(args[0]);
		controler.setCreateGraphs(false);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
