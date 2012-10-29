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
package playground.droeder.southAfrica.old.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author droeder
 *
 */
public class PtSubModeRouterOld implements TransitRouter{
	private static final Logger log = Logger
			.getLogger(PtSubModeRouterOld.class);
	
	private TransitRouterConfig config;
	private HashMap<String, TransitRouter> modeRouter = null;

//	/**
//	 * 
//	 * This router got two functionalities
//	 * <ul>
//	 *	<li>it keeps the pt-'submodes' like train/bus/tram</li>
//	 *	<li>it routes a leg on the (sub-)mode specified in it, if routeOnSameMode is true
//	 *</ul>
//	 *	It uses the original MATSim-TransitRouter 
//	 *
//	 * @param sc
//	 * @param routeOnSameMode
//	 */
//	public PtSubModeDependendRouter(Scenario sc, boolean routeOnSameMode){
//		this.config = new TransitRouterConfig(sc.getConfig());
//		this.modeRouter = new HashMap<String, TransitRouter>();
//		this.initTransitRouter(sc, routeOnSameMode);
//		//use TransportMode.pt as default
//		this.modeRouter.put(TransportMode.pt, new TransitRouterImpl(this.config, sc.getTransitSchedule()));
//	}
	
	/**
	 * This router got two functionalities
	 * <ul>
	 *	<li>it keeps the pt-'submodes' like train/bus/tram</li>
	 *	<li>it routes a leg on the (sub-)mode specified in it, if routeOnSameMode is true
	 *</ul>
	 *	It uses the original MATSim-TransitRouter
	 *
	 * @param config
	 * @param networks
	 * @param timeAndDisutility
	 */
	public PtSubModeRouterOld(TransitRouterConfig config, Map<String, TransitRouterNetwork> networks, TransitRouterNetworkTravelTimeAndDisutility timeAndDisutility){
		this.config = config;
		this.modeRouter = new HashMap<String, TransitRouter>();
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
			this.modeRouter.put(e.getKey(), new TransitRouterImpl(config2, e.getValue(), timeAndDisutility, timeAndDisutility));
		}
	}
	
	
	
	private boolean warn = true;
	/**
	 * 
	 * @param mode
	 * @return
	 */
	public TransitRouter getModeRouter(String mode){
		if(this.modeRouter.containsKey(mode)){
			return this.modeRouter.get(mode);
		}
		if(warn){
			log.warn("Can not find router for single mode " + mode + ". Returning default pt-router..." +
					" Message thrown only once...");
			warn = false;
		}
		return this.modeRouter.get(TransportMode.pt);
	}

	


	// ################# this is used for the old implementation ###########################
	
	@Override
	public List<Leg> calcRoute(Coord fromCoord, Coord toCoord, double departureTime, Person person) {
		throw new UnsupportedOperationException("use class own's calcRoute(Leg)! This class probably only works with PlansCalcSubModeDependendTransitRoute...");
	}
	
	/**
	 * @param person
	 * @param leg
	 * @param fromAct
	 * @param toAct
	 * @param depTime
	 * @return
	 */
	public List<Leg> calcRoute(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		if(this.modeRouter.containsKey(leg.getMode())){
			return this.modeRouter.get(leg.getMode()).calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime, person);
		}else{
			return this.modeRouter.get(TransportMode.pt).calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime, person);
		}
	}

	/**
	 * @param mode
	 * @return
	 */
	public boolean calculatedRouteForMode(String mode) {
		return this.modeRouter.containsKey(mode);
	}
	
	// #####################################################################################

}

