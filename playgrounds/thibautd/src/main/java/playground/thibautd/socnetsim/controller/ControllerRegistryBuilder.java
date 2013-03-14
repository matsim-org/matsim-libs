/* *********************************************************************** *
 * project: org.matsim.*
 * ControllerRegistryBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.Collection;
import java.util.Collections;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;

/**
 * Allows to build a ControllerRegistry with certain default values
 * @author thibautd
 */
public class ControllerRegistryBuilder {
	private final Scenario scenario;
	private final EventsManager events;

	// configurable elements
	private TravelTimeCalculator travelTime;
	private TravelDisutilityFactory travelDisutilityFactory;
	private ScoringFunctionFactory scoringFunctionFactory;
	private CalcLegTimes legTimes;
	private MobsimFactory mobsimFactory;
	private TripRouterFactory tripRouterFactory;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	private PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private GroupIdentifier groupIdentifier;

	// /////////////////////////////////////////////////////////////////////////
	// contrs
	/**
	 * Initializes a builder bith default fields
	 * @param scenario must contain a JointPlans element
	 */
	public ControllerRegistryBuilder( final Scenario scenario ) {
		this.scenario = scenario;

		// by default, no groups (results in individual replanning)
		this.groupIdentifier = new GroupIdentifier() {
			@Override
			public Collection<ReplanningGroup> identifyGroups(
					final Population population) {
				return Collections.<ReplanningGroup>emptyList();
			}
		};

		this.scoringFunctionFactory =
				new CharyparNagelScoringFunctionFactory(
					scenario.getConfig().planCalcScore(),
					scenario.getNetwork());
		// by default: do not care about joint trips, vehicles or what not
		this.planRoutingAlgorithmFactory = new PlanRoutingAlgorithmFactory() {
			@Override
			public PlanAlgorithm createPlanRoutingAlgorithm(
					final TripRouter tripRouter) {
				return new PlanRouter( tripRouter );
			}
		};

		this.mobsimFactory = new JointQSimFactory();

		this.events = EventsUtils.createEventsManager( scenario.getConfig() );

		// some analysis utils
		// should probably be moved somewhere else
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
		// XXX this will stay if travel time is replaced!
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

	// /////////////////////////////////////////////////////////////////////////
	// "with" methods
	public ControllerRegistryBuilder withTravelTimeCalculator(
			final TravelTimeCalculator travelTime2) {
		this.travelTime = travelTime2;
		return this;
	}

	public ControllerRegistryBuilder withTravelDisutilityFactory(
			final TravelDisutilityFactory travelDisutilityFactory2) {
		this.travelDisutilityFactory = travelDisutilityFactory2;
		return this;
	}

	public ControllerRegistryBuilder withScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory2) {
		this.scoringFunctionFactory = scoringFunctionFactory2;
		return this;
	}

	public ControllerRegistryBuilder withCalcLegTimes(
			final CalcLegTimes legTimes2) {
		this.legTimes = legTimes2;
		return this;
	}

	public ControllerRegistryBuilder withMobsimFactory(
			final MobsimFactory mobsimFactory2) {
		this.mobsimFactory = mobsimFactory2;
		return this;
	}

	public ControllerRegistryBuilder withTripRouterFactory(
			final TripRouterFactory tripRouterFactory2) {
		this.tripRouterFactory = tripRouterFactory2;
		return this;
	}

	public ControllerRegistryBuilder withLeastCostPathCalculatorFactory(
			final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory2) {
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory2;
		return this;
	}

	public ControllerRegistryBuilder withPlanRoutingAlgorithmFactory(
			final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory2) {
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory2;
		return this;
	}

	public ControllerRegistryBuilder withGroupIdentifier(
			final GroupIdentifier groupIdentifier2) {
		this.groupIdentifier = groupIdentifier2;
		return this;
	}

	// /////////////////////////////////////////////////////////////////////////
	// build
	public ControllerRegistry build() {
		return new ControllerRegistry(
			scenario,
			events,
			travelTime,
			travelDisutilityFactory,
			scoringFunctionFactory,
			legTimes,
			mobsimFactory,
			tripRouterFactory,
			leastCostPathCalculatorFactory,
			planRoutingAlgorithmFactory,
			groupIdentifier);
	}
}

