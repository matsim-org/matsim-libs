/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.wagonSim.pt.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener.VehicleLoad;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

/**
 * @author droeder
 *
 */
final class WagonSimRouterNetworkTravelDistutilityAndTravelTime implements TransitTravelDisutility,
																		TravelTime{

	private static final Logger log = Logger
			.getLogger(WagonSimRouterNetworkTravelDistutilityAndTravelTime.class);

	
	private TransitRouterNetworkTravelTimeAndDisutility delegate;
	private VehicleLoad vehLoad;
	private ObjectAttributes wagonAttribs;
	private ObjectAttributes locomotiveAttribs;
	private TransitRouterConfig config;

	WagonSimRouterNetworkTravelDistutilityAndTravelTime(
										TransitRouterConfig config,
										PreparedTransitSchedule preparedTransitSchedule, 
										VehicleLoad vehLoad, 
										ObjectAttributes locomotiveAttribs,
										ObjectAttributes wagonAttribs) {
		this.delegate = new TransitRouterNetworkTravelTimeAndDisutility(config, preparedTransitSchedule);
		this.vehLoad = vehLoad;
		this.locomotiveAttribs = locomotiveAttribs;
		this.wagonAttribs = wagonAttribs;
		this.config = config;
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person,
			Vehicle vehicle) {
		TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
		if(l.getRoute() == null){
			// it's a transfer-link
			return this.config.getAdditionalTransferTime();
		}
		return this.delegate.getLinkTravelTime(link, time, person, vehicle);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle, CustomDataManager dataManager) {
		double additionalUtility = 0;
		TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
		if(l.getRoute() != null){
			// this assumes there is only one departure per route. Usually this is not the case.
			// Therefore the data is preprocessed WagonSimRouterFactoryImpl#createRouterNetwork(...)
			Departure d = l.getRoute().getDepartures().values().iterator().next();
			additionalUtility += calcAdditionalTravelDisutility(link, time, person, d);
		}
		return (this.delegate.getLinkTravelDisutility(link, time, person, vehicle, dataManager) 
				- additionalUtility);
	}
	
//	private boolean warn = true;
	/**
	 * @param link
	 * @param time
	 * @param person
	 * @param vehicle
	 * @param dataManager
	 * @return
	 */
	private double calcAdditionalTravelDisutility(Link link, double time, Person person, Departure d) {
//		if(warn){
//			warn = false;
//			log.warn("check if the length and weight of the locomotive is included/excluded in the max. weight/length. " +
//					"Message thrown only once.");
//		}
		// collect necessary data
		TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
		TransitRouteStop s = l.getFromNode().getStop();
		int i = l.getRoute().getStops().indexOf(s);
		if(WagonSimVehicleLoadListener.freeCapacityInVehicle(
				d.getVehicleId().toString(), 
				s.getStopFacility().getId().toString(), 
				i, 
				person.getId().toString(), 
				vehLoad, 
				locomotiveAttribs, 
				wagonAttribs)){
			return 0;
		}
		return WagonSimConstants.ADDITIONAL_DISUTILITY;
	}

	@Override
	public double getTravelTime(Person person, Coord coord, Coord toCoord) {
		return this.delegate.getTravelTime(person, coord, toCoord);
	}

	@Override
	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		return this.delegate.getTravelDisutility(person, coord, toCoord);
	}
	
}

