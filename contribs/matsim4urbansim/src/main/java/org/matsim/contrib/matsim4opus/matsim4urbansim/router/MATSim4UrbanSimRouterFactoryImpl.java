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
package org.matsim.contrib.matsim4opus.matsim4urbansim.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.old.NetworkLegRouter;

/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimRouterFactoryImpl implements TripRouterFactory{

	private final Controler controler;
	private final PtMatrix ptMatrix;
	
	public MATSim4UrbanSimRouterFactoryImpl(final Controler controler, final PtMatrix ptMatrix) {
		this.controler = controler;
		this.ptMatrix  = ptMatrix;
	}
	
	@Override
	public TripRouter createTripRouter() {
		// initialize here - controller should be fully initialized by now
		// use fields to keep the rest of the code clean and comparable
		
		Config config = controler.getScenario().getConfig();
		Network network= controler.getScenario().getNetwork();
		Scenario scenario= controler.getScenario();
		
		
		TripRouter tripRouter = new TripRouter();
		
		// car routing
		tripRouter.setRoutingModule(TransportMode.car, new LegRouterWrapper(
				TransportMode.car,
				scenario.getPopulation().getFactory(),
				new NetworkLegRouter(
					network,
					controler.getLeastCostPathCalculatorFactory().createPathCalculator(
							scenario.getNetwork(), 
							controler.getTravelDisutilityFactory().createTravelDisutility(controler.getLinkTravelTimes(), config.planCalcScore()), 
							controler.getLinkTravelTimes()),
					((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory())));
		
		// pt routing (teleportation)
		tripRouter.setRoutingModule(TransportMode.pt, 
				new PseudoPtRoutingModule(controler, ptMatrix));
		
		return tripRouter;
	}
}
