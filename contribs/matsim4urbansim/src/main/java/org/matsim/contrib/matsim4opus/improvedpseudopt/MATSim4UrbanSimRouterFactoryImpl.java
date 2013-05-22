/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
package org.matsim.contrib.matsim4opus.improvedpseudopt;

import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PseudoTransitLegRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.NetworkUtils;

/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimRouterFactoryImpl implements TripRouterFactory{
	private static final Logger log = Logger
			.getLogger(MATSim4UrbanSimRouterFactoryImpl.class);


	private final PtMatrix ptMatrix;
	// The single instance of ptMatrix is passed to multiple instances of the TripRouter.  Looks to me like this will work, since
	// there is only read access to ptMatrix.  kai, may'13

	private Controler  controler; 
	
	public MATSim4UrbanSimRouterFactoryImpl(final Controler controler, final PtMatrix ptMatrix) {
		this.controler = controler;
		this.ptMatrix  = ptMatrix;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter() {
		//initialize triprouter
		TripRouter tripRouter = new TripRouter();

		// initialize here - controller should be fully initialized by now
		// use fields to keep the rest of the code clean and comparable
		Config config = controler.getScenario().getConfig();
		Network network= controler.getScenario().getNetwork();
		TravelDisutilityFactory travelDisutilityFactory = controler.getTravelDisutilityFactory();
		TravelTime travelTime = controler.getLinkTravelTimes();
		LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory = controler.getLeastCostPathCalculatorFactory();
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) controler.getScenario().getPopulation().
				getFactory()).getModeRouteFactory();
		PopulationFactory populationFactory = controler.getPopulation().getFactory();
		PlansCalcRouteConfigGroup routeConfigGroup = controler.getConfig().plansCalcRoute();
		

		
		/*
		 *  c&p code from TripRouterFactoryImpl. Might be easier to extend TripRouterFactoryImpl, 
		 *  as only the pt-routing-module is overwritten here compared to TripRouterFactoryImpl. However, not all fields
		 *  are initialized when the constructor of this class is called. Daniel, May '13
		 */
		TravelDisutility travelCost =
				travelDisutilityFactory.createTravelDisutility(
						travelTime,
						config.planCalcScore() );
		
		LeastCostPathCalculator routeAlgo =
				leastCostPathAlgorithmFactory.createPathCalculator(
						network,
						travelCost,
						travelTime);
		
		FreespeedTravelTimeAndDisutility ptTimeCostCalc =
				new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		LeastCostPathCalculator routeAlgoPtFreeFlow =
				leastCostPathAlgorithmFactory.createPathCalculator(
						network,
						ptTimeCostCalc,
						ptTimeCostCalc);

		if ( NetworkUtils.isMultimodal( network ) ) {
			// note: LinkImpl has a default allowed mode of "car" so that all links
			// of a monomodal network are actually restricted to car, making the check
			// of multimodality unecessary from a behavioral point of view.
			// However, checking the mode restriction for each link is expensive,
			// so it is not worth doing it if it is not necessary. (td, oct. 2012)
			if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
				((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(
					Collections.singleton( TransportMode.car ));
				((IntermodalLeastCostPathCalculator) routeAlgoPtFreeFlow).setModeRestriction(
					Collections.singleton( TransportMode.car ));
				log.warn("You use a multi-modal simulation with matsim4urbansim. Make sure this is what you want. Functionallity has not been tested yet.");
			}
			else {
				// this is impossible to reach when using the algorithms of org.matsim.*
				// (all implement IntermodalLeastCostPathCalculator)
				log.warn( "network is multimodal but least cost path algorithm is not an instance of IntermodalLeastCostPathCalculator!" );
			}
		}
		
		// the way teleported modes are initialized is very dangerous. routingmodules for freespeed-factors are overwritten without any
		// warning when freespeedFactor and speed are set in config. However, this is the way MATSim is initialized per default.
		// Because of that I decided to use reimplement the default behavior here. Daniel, May '13
		for (String mainMode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						populationFactory,
						new PseudoTransitLegRouter(
							network,
							routeAlgoPtFreeFlow,
							routeConfigGroup.getTeleportedModeFreespeedFactors().get( mainMode ),
							routeConfigGroup.getBeelineDistanceFactor(),
							modeRouteFactory)));
		}

		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						populationFactory,
						new TeleportationLegRouter(
							modeRouteFactory,
							routeConfigGroup.getTeleportedModeSpeeds().get( mainMode ),
							routeConfigGroup.getBeelineDistanceFactor())));
		}

		for ( String mainMode : routeConfigGroup.getNetworkModes() ) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						populationFactory,
						new NetworkLegRouter(
							network,
							routeAlgo,
							modeRouteFactory)));
		}
		// end c&p from TripRouterFactoryImpl
		
		if ( config.scenario().isUseTransit() ) {
			throw new IllegalArgumentException("You try to use the physical simulation of transit in combination with the enhanced " +
					"pseudo-pt-router from matsim4urbansim. This will lead to confusing results. Please make sure to use either the physical" +
					"transit simulation or the enhanced pseudo pt and restart the simulation.");
		}
		
		
		// overwrite setting for pt with enhanced pt routing (teleportation)
		tripRouter.setRoutingModule(TransportMode.pt, 
				new PseudoPtRoutingModule(controler, ptMatrix));
		
		return tripRouter;
	}
}
