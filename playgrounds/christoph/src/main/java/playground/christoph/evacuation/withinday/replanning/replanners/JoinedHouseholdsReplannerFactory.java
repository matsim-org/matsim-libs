/* *********************************************************************** *
 * project: org.matsim.*
 * JoinedHouseholdsReplannerFactory.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTimeCalculator;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifier;

public class JoinedHouseholdsReplannerFactory extends WithinDayDuringActivityReplannerFactory {

	private final Scenario scenario;
	private final DecisionDataProvider decisionDataProvider;
	private final JoinedHouseholdsIdentifier identifier;
	private final SwissPTTravelTimeCalculator ptTravelTime;
	private final TripRouterFactory tripRouterFactory;
	private final RoutingContext routingContext;
	
	public JoinedHouseholdsReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
			DecisionDataProvider decisionDataProvider, JoinedHouseholdsIdentifier identifier,
			SwissPTTravelTimeCalculator ptTravelTime, TripRouterFactory tripRouterFactory, RoutingContext routingContext) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.decisionDataProvider = decisionDataProvider;
		this.identifier = identifier;
		this.ptTravelTime = ptTravelTime;
		this.tripRouterFactory = tripRouterFactory;
		this.routingContext = routingContext;
	}

	@Override
	public WithinDayDuringActivityReplanner createReplanner() {
		WithinDayDuringActivityReplanner replanner = new JoinedHouseholdsReplanner(super.getId(), scenario, 
				this.getWithinDayEngine().getActivityRescheduler(), decisionDataProvider, identifier, ptTravelTime,
				this.tripRouterFactory.instantiateAndConfigureTripRouter(this.routingContext));
		return replanner;
	}

}