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
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.router.TransitRouterFactory;

/**
 * @author cdobler
 */
public class TransitTripRouterFactory implements TripRouterFactory {
	
	private static final Logger log = Logger.getLogger(TransitTripRouterFactory.class);
		
	private final TripRouterFactory delegateFactory;
	private final TransitRouterFactory transitRouterFactory;
	private final Scenario scenario;
		
	public TransitTripRouterFactory(Scenario scenario, TripRouterFactory delegateFactory,
			TransitRouterFactory transitRouterFactory) {
		this.scenario = scenario;
		this.delegateFactory = delegateFactory;
		this.transitRouterFactory = transitRouterFactory;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {

		TripRouter tripRouter = this.delegateFactory.instantiateAndConfigureTripRouter(routingContext);
		
		if (this.scenario.getConfig().scenario().isUseTransit()) {

            PopulationFactory populationFactory = this.scenario.getPopulation().getFactory();
			ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
			PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();
			RoutingModule transitWalkRoutingModule;
			
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
			Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());

			// if transit_walk should also be simulated by the multi-modal simulation, re-use the RoutingModule created by the delegate
			if (simulatedModes.contains(TransportMode.transit_walk)) {
				transitWalkRoutingModule = tripRouter.getRoutingModule(TransportMode.walk);
			} else {
				transitWalkRoutingModule = LegRouterWrapper.createTeleportationRouter(TransportMode.transit_walk, populationFactory, 
						routeConfigGroup.getModeRoutingParams().get( TransportMode.walk )
						);
			}
			
        	TransitRouterWrapper routingModule = new TransitRouterWrapper(transitRouterFactory.createTransitRouter(), scenario.getTransitSchedule(),
        			scenario.getNetwork(), transitWalkRoutingModule);
        	
            for (String mode : scenario.getConfig().transit().getTransitModes()) {
                tripRouter.setRoutingModule(mode, routingModule);
            }
		}
		
		return tripRouter;
	}
}