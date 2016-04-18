/* *********************************************************************** *
 * project: org.matsim.*
 * TransitTripRouterFactory.java
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

package org.matsim.contrib.multimodal.router;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Provider;

/**
 * @author cdobler
 */
public class TransitTripRouterFactory implements Provider<TripRouter> {
	
	private static final Logger log = Logger.getLogger(TransitTripRouterFactory.class);
		
	private final Provider<TripRouter> delegateFactory;
	private final Provider<TransitRouter> transitRouterFactory;
	private final Scenario scenario;
		
	public TransitTripRouterFactory(Scenario scenario, Provider<TripRouter> delegateFactory,
			Provider<TransitRouter> transitRouterFactory) {
		this.scenario = scenario;
		this.delegateFactory = delegateFactory;
		this.transitRouterFactory = transitRouterFactory;
	}
	
	@Override
	public TripRouter get() {

		TripRouter tripRouter = this.delegateFactory.get();
		
		if (this.scenario.getConfig().transit().isUseTransit()) {

            PopulationFactory populationFactory = this.scenario.getPopulation().getFactory();
			RouteFactoryImpl modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getRouteFactory();
			PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();
			RoutingModule transitWalkRoutingModule;
			
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
			Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());

			// if transit_walk should also be simulated by the multi-modal simulation, re-use the RoutingModule created by the delegate
			if (simulatedModes.contains(TransportMode.transit_walk)) {
				transitWalkRoutingModule = tripRouter.getRoutingModule(TransportMode.walk);
			} else {
				transitWalkRoutingModule = DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, populationFactory, 
						routeConfigGroup.getModeRoutingParams().get( TransportMode.walk )
						);
			}
			
        	TransitRouterWrapper routingModule = new TransitRouterWrapper(transitRouterFactory.get(), scenario.getTransitSchedule(),
        			scenario.getNetwork(), transitWalkRoutingModule);
        	
            for (String mode : scenario.getConfig().transit().getTransitModes()) {
                tripRouter.setRoutingModule(mode, routingModule);
            }
		}
		
		return tripRouter;
	}
}