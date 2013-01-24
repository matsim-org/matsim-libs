/* *********************************************************************** *
 * project: org.matsim.*
 * ControllerRegistry.java
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
package playground.thibautd.socnetsim.controller;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;

import playground.thibautd.socnetsim.population.PlanLinks;
import playground.thibautd.socnetsim.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;

/**
 * @author thibautd
 */
public final class ControllerRegistry {
	private final Scenario scenario;
	private final PlanLinks jointPlans;
	private final EventsManager events;
	private final TravelTimeCalculator travelTime;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final CalcLegTimes legTimes;
	private final MobsimFactory mobsimFactory;
	private final TripRouterFactory tripRouterFactory;
	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public ControllerRegistry(
			final Scenario scenario,
			final PlanLinks jointPlans,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.scenario = scenario;
		this.jointPlans = jointPlans;
		this.scoringFunctionFactory = scoringFunctionFactory;

		this.events = EventsUtils.createEventsManager( scenario.getConfig() );

	 	this.mobsimFactory = new JointQSimFactory();

		// some analysis utils
		this.events.addHandler(
				new VolumesAnalyzer(
					3600, 24 * 3600 - 1,
					scenario.getNetwork()));
		this.legTimes = new CalcLegTimes();
		this.events.addHandler( legTimes );
		this.travelTime =
			new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator());
		this.events.addHandler(travelTime);	

		this.travelDisutilityFactory = new TravelCostCalculatorFactoryImpl();
		switch (scenario.getConfig().controler().getRoutingAlgorithmType()) {
			case AStarLandmarks:
				this.leastCostPathCalculatorFactory =
						new AStarLandmarksFactory(
									scenario.getNetwork(),
									travelDisutilityFactory.createTravelDisutility(
										travelTime.getLinkTravelTimes(),
										scenario.getConfig().planCalcScore()));
				break;
			case Dijkstra:
				PreProcessDijkstra ppd = new PreProcessDijkstra();
				ppd.run( scenario.getNetwork() );
				this.leastCostPathCalculatorFactory = new DijkstraFactory( ppd );
				break;
			case FastAStarLandmarks:
				this.leastCostPathCalculatorFactory =
						new FastAStarLandmarksFactory(
									scenario.getNetwork(),
									travelDisutilityFactory.createTravelDisutility(
										travelTime.getLinkTravelTimes(),
										scenario.getConfig().planCalcScore()));
				break;
			case FastDijkstra:
				PreProcessDijkstra ppfd = new PreProcessDijkstra();
				ppfd.run( scenario.getNetwork() );
				this.leastCostPathCalculatorFactory = new FastDijkstraFactory( ppfd );
				break;
			default:
				throw new IllegalArgumentException( "unkown algorithm "+scenario.getConfig().controler().getRoutingAlgorithmType() );
		}

		this.tripRouterFactory = new JointTripRouterFactory(
				scenario,
				travelDisutilityFactory,
				travelTime.getLinkTravelTimes(),
				leastCostPathCalculatorFactory,
				null); // last arg: transit router factory.
	}

	public Scenario getScenario() {
		return scenario;
	}
	
	public PlanLinks getJointPlans() {
		return jointPlans;
	}

	public EventsManager getEvents() {
		return events;
	}

	public TravelTimeCalculator getTravelTime() {
		return travelTime;
	}

	public TravelDisutilityFactory getTravelDisutilityFactory() {
		return travelDisutilityFactory;
	}

	public ScoringFunctionFactory getScoringFunctionFactory() {
		return scoringFunctionFactory;
	}

	public CalcLegTimes getLegTimes() {
		return legTimes;
	}

	public MobsimFactory getMobsimFactory() {
		return mobsimFactory;
	}

	public TripRouterFactory getTripRouterFactory() {
		return tripRouterFactory;
	}

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return leastCostPathCalculatorFactory;
	}
}

