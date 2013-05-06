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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;

/**
 * @author thibautd
 */
public final class ControllerRegistry {
	private final Scenario scenario;
	private final EventsManager events;
	private final TravelTimeCalculator travelTime;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final CalcLegTimes legTimes;
	private final MobsimFactory mobsimFactory;
	private final TripRouterFactory tripRouterFactory;
	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final GroupIdentifier groupIdentifier;
	private final Iterable<GenericPlanAlgorithm<ReplanningGroup>> prepareForSimAlgorithms;
	private final PlanLinkIdentifier planLinkIdentifier;
	private final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory;

	ControllerRegistry(
			final Scenario scenario,
			final EventsManager events,
			final TravelTimeCalculator travelTime,
			final TravelDisutilityFactory travelDisutilityFactory,
			final ScoringFunctionFactory scoringFunctionFactory,
			final CalcLegTimes legTimes,
			final MobsimFactory mobsimFactory,
			final TripRouterFactory tripRouterFactory,
			final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
			final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory,
			final GroupIdentifier groupIdentifier,
			final Iterable<GenericPlanAlgorithm<ReplanningGroup>> prepareForSimAlgorithms,
			final PlanLinkIdentifier planLinkIdentifier,
			final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory) {
		this.scenario = scenario;
		this.events = events;
		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.legTimes = legTimes;
		this.mobsimFactory = mobsimFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory;
		this.groupIdentifier = groupIdentifier;
		this.prepareForSimAlgorithms = prepareForSimAlgorithms;
		this.planLinkIdentifier = planLinkIdentifier;
		this.incompatiblePlansIdentifierFactory = incompatiblePlansIdentifierFactory;
	}

	public Scenario getScenario() {
		return scenario;
	}
	
	public JointPlans getJointPlans() {
		return scenario.getScenarioElement( JointPlans.class );
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

	public PlanRoutingAlgorithmFactory getPlanRoutingAlgorithmFactory() {
		return planRoutingAlgorithmFactory;
	}

	// XXX ouch... if this thing starts to provide factory methods, not sure it is a "registry"...
	// though it is just a wrapper to see the object under another interface, so it is probably ok.
	public ReplanningContext createReplanningContext(final int iter) {
		final ControllerRegistry registry = this;
		return new ReplanningContext() {
			@Override
			public TripRouterFactory getTripRouterFactory() {
				return registry.getTripRouterFactory();
			}

			@Override
			public TravelDisutility getTravelCostCalculator() {
				return registry.getTravelDisutilityFactory().createTravelDisutility(
					registry.getTravelTime().getLinkTravelTimes(),
					registry.getScenario().getConfig().planCalcScore() );
			}

			@Override
			public TravelTime getTravelTimeCalculator() {
				return registry.getTravelTime().getLinkTravelTimes();
			}

			@Override
			public ScoringFunctionFactory getScoringFunctionFactory() {
				return registry.getScoringFunctionFactory();
			}

			@Override
			public int getIteration() {
				return iter;
			}
		};
	}

	public GroupIdentifier getGroupIdentifier() {
		return groupIdentifier;
	}

	public Iterable<GenericPlanAlgorithm<ReplanningGroup>> getPrepareForSimAlgorithms() {
		return prepareForSimAlgorithms;
	}

	public PlanLinkIdentifier getPlanLinkIdentifier() {
		return planLinkIdentifier;
	}

	public IncompatiblePlansIdentifierFactory getIncompatiblePlansIdentifierFactory() {
		return this.incompatiblePlansIdentifierFactory;
	}
}

