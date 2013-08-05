/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.ptSubModes.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;

/**
 * @author droeder
 *
 */
class PtSubModeRouterSet implements TransitRouter{
	private static final Logger log = Logger
			.getLogger(PtSubModeRouterSet.class);
	
	private HashMap<String, TransitRouter> modeRouter = null;

	
	/**
	 * This ''router`` isn't a real router. It provides a method to get PtSubMode-Router, used by the PtSubModeTripRouterFactory
	 *
	 * @param config
	 * @param networks
	 * @param timeAndDisutility
	 * @param routeOnSameMode 
	 */
	public PtSubModeRouterSet(TransitRouterConfig config, Map<String, TransitRouterNetwork> networks, TransitRouterNetworkTravelTimeAndDisutility timeAndDisutility, boolean routeOnSameMode){
		this.modeRouter = new HashMap<String, TransitRouter>();
		this.routeOnSameMode = routeOnSameMode;
		this.initRouter(config, networks, timeAndDisutility);
	}
	
	
	/**
	 * @param config2
	 * @param networks
	 * @param timeAndDisutility
	 */
	private void initRouter(TransitRouterConfig config2,
			Map<String, TransitRouterNetwork> networks,
			TransitRouterNetworkTravelTimeAndDisutility timeAndDisutility) {
		
		for(Entry<String, TransitRouterNetwork> e :networks.entrySet()){
			this.modeRouter.put(e.getKey(), new PtSubModeRouter(config2, e.getValue(), timeAndDisutility, timeAndDisutility));
		}
	}
	
	
	
	private boolean warn = true;

	private boolean routeOnSameMode;
	/**
	 * 
	 * @param mode
	 * @return
	 */
	public TransitRouter getModeRouter(String mode){
		if(this.modeRouter.containsKey(mode)){
			return this.modeRouter.get(mode);
		}
		// warn only, if we want to route on submodes...
		if(this.warn && this.routeOnSameMode){
			log.warn("Can not find router for single mode " + mode + ". Returning default pt-router..." +
					" Message thrown only once...");
			warn = false;
		}
		// This one should be available always, as it is the MATSim-default.
		return this.modeRouter.get(TransportMode.pt);
	}

	
	/**
	 *  This Method is not supported, as this ''Router`` just provides SubModeRouter
	 */
	@Override
	public List<Leg> calcRoute(Coord fromCoord, Coord toCoord, double departureTime, Person person) {
		throw new UnsupportedOperationException("use class own's calcRoute(Leg)! This class probably only works with PlansCalcSubModeDependendTransitRoute...");
	}

}

