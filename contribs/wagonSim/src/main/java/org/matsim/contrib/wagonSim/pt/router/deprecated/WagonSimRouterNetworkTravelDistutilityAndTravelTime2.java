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

package org.matsim.contrib.wagonSim.pt.router.deprecated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener.VehicleLoad;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.DepartureImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * this class calculates disutility/traveltime completely independent.
 * Thus, it might handle some parts different than the default {@link TransitRouterNetworkTravelTimeAndDisutility},
 * one the other hand some parts are very similar.
 * Sometimes this is necessary because we e.g. do not want the first departure, when there is a later one 
 * which is not full.
 * 
 * @author droeder
 *
 */
public class WagonSimRouterNetworkTravelDistutilityAndTravelTime2 implements TransitTravelDisutility,
																		TravelTime{

	private static final Logger log = Logger
			.getLogger(WagonSimRouterNetworkTravelDistutilityAndTravelTime2.class);
	private static final double ADDITIONAL_DISUTILITY = 100;
	
	private VehicleLoad vehLoad;
	private ObjectAttributes wagonAttribs;
	private ObjectAttributes locomotiveAttribs;
	private TransitRouterConfig transitRouterConfig;
	private Map<String, List<Departure>> sortedDepartures;
	private TransitRouterNetworkTravelTimeAndDisutility delegate;

	public WagonSimRouterNetworkTravelDistutilityAndTravelTime2(
										TransitRouterConfig config,
										PreparedTransitSchedule preparedTransitSchedule, 
										VehicleLoad vehLoad, 
										ObjectAttributes locomotiveAttribs,
										ObjectAttributes wagonAttribs) {
		this.transitRouterConfig = config;
		this.delegate = new TransitRouterNetworkTravelTimeAndDisutility(config, preparedTransitSchedule);
		this.vehLoad = vehLoad;
		this.locomotiveAttribs = locomotiveAttribs;
		this.wagonAttribs = wagonAttribs;
		this.sortedDepartures = new HashMap<String, List<Departure>>();
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double tt = 0;
		TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
		if(l.getRoute() == null){
			// it's a transfer-link, time is fixed by definition
			tt = transitRouterConfig.getAdditionalTransferTime();
		}else{
			double nextDeparture = getNextDeparture(l, time, person, vehicle);
			// calc the traveltime
			TransitRouteStop toStop = l.toNode.stop;
			double arrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
			tt = (nextDeparture - time) + (arrivalOffset - l.getFromNode().stop.getDepartureOffset());
		}
		return tt;
	}
	
	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle, CustomDataManager dataManager) {
		double disutility = 0;
		TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
		if(l.getRoute() == null){
			// it is a transfer-link
			disutility = - (getLinkTravelTime(link, time, person, vehicle) * transitRouterConfig.getMarginalUtilityOfTravelTimePt_utl_s()) 
					- transitRouterConfig.getUtilityOfLineSwitch_utl();
		}else{
			// calcNextDeparture
			double nextDepartureTime = getNextDeparture(l, time, person, vehicle);
			TransitRouteStop fromStop = l.fromNode.stop;
			// calc the different components of the disutility
			double fromStopArrivalOffset = (fromStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? fromStop.getArrivalOffset() : fromStop.getDepartureOffset();
			double vehArrivalTime = nextDepartureTime - (fromStop.getDepartureOffset() - fromStopArrivalOffset);
			double offVehWaitTime=0;
			if (time < vehArrivalTime) offVehWaitTime = vehArrivalTime-time;
			disutility = - (getLinkTravelTime(link,time, person, vehicle) - offVehWaitTime)      * transitRouterConfig.getMarginalUtilityOfTravelTimePt_utl_s() // time in the vehicle 
					-offVehWaitTime   * transitRouterConfig.getMarginalUtilityOfWaitingPt_utl_s() // time outside the vehicle
					-link.getLength() * transitRouterConfig.getMarginalUtilityOfTravelDistancePt_utl_m(); // travelled distance
			// check if we have to punish this departure
			Departure d = getEarliestPossibleDeparture(l, time, person, vehicle);
			if(punishDeparture(link, person, d)){
				if(d.getDepartureTime() + fromStop.getDepartureOffset() > time){
					// punish only departures that happen ``today''
					disutility -= ADDITIONAL_DISUTILITY;
				}
			}
		}
		return disutility;
	}
	
	@Override
	public double getTravelTime(Person person, Coord coord, Coord toCoord) {
		return this.delegate.getTravelTime(person, coord, toCoord);
	}

	@Override
	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		double cost = this.delegate.getTravelDisutility(person, coord, toCoord); 
//		double distance = CoordUtils.calcDistance(coord,toCoord);
//		if(distance > 0) cost -= ADDITIONAL_DISUTILITY;
		return  cost;
	}

	
	private double getNextDeparture(TransitRouterNetworkLink l, double time,
			Person person, Vehicle vehicle){
		// init the departure-time-cache
		initDepartures(l);
		// get the earliest departure
		Departure d = getEarliestPossibleDeparture(l, time, person, vehicle);
		double nextDeparture = d.getDepartureTime() + l.getFromNode().stop.getDepartureOffset();
		// maybe we got a departure somewhere back in time, thus check the next mornings
		while(nextDeparture < time){
			nextDeparture += 24.0*3600;
		}
		return nextDeparture;
	}
	/**
	 * 
	 * 
	 * @param link
	 * @param time
	 * @param person
	 * @param vehicle
	 * @return, a) a possible/unpunished departure, b) the first possible departure, 
	 * c) the very first departure (even if (dep + offset < time)) 
	 */
	private Departure getEarliestPossibleDeparture(TransitRouterNetworkLink l, double time,
			Person person, Vehicle vehicle) {
		String id = l.getLine().getId().toString() + "---" + l.getRoute().getId().toString();
		double offset = l.getFromNode().getStop().getDepartureOffset();
		double check = Double.MIN_VALUE;
		Departure temp = null;
		
		for(Departure d : sortedDepartures.get(id)){
			// check consistency
			if(d.getDepartureTime() < check){
				log.warn("seems departures are not sorted...");
			}
			// we will not miss this departure
			if(d.getDepartureTime() + offset >= time){
				// and there is no additional Disutility. that is, it is the earliest departure we can and
				// we are allowed to catch
				if(!punishDeparture(l, person, d)){
					return d;
				}
				if(temp == null){
					// store the earliest possible departure
					temp = d;
				}
			}
			check = d.getDepartureTime();
		}
		// check if we got a possible departure. If not, return the first of the day.
		temp = (temp == null) ? sortedDepartures.get(id).get(0) : temp;
		// we didn't find a reasonable, not punished departure. Thus, we use the earliest possible time for further search.
		return temp;
	}

	/**
	 * in some way this provides a functionality like {@link PreparedTransitSchedule}, 
	 * but we have access to the departure-ids
	 * 
	 * @param l
	 */
	private void initDepartures(TransitRouterNetworkLink l) {
		String id = l.getLine().getId().toString() + "---" + l.getRoute().getId().toString();
		if(!sortedDepartures.containsKey(id)){
			List<Departure> temp = new ArrayList<Departure>(l.getRoute().getDepartures().values());
			Collections.sort(temp, new MyComparator());
			this.sortedDepartures.put(id, temp);
		}
	}


	
	private boolean warn = true;
	/**
	 * @param link
	 * @param time
	 * @param person
	 * @param vehicle
	 * @param dataManager
	 * @return
	 */
	private boolean punishDeparture(Link link, Person person, Departure d) {
		if(warn){
			warn = false;
			log.warn("check if the length and weight of the locomotive is included/excluded in the max. weight/length. " +
					"Message thrown only once.");
		}
		// collect necessary data
		TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
		TransitRouteStop s = l.getFromNode().getStop();
		int i = l.getRoute().getStops().indexOf(s);
//		String vehDepId = WagonSimVehicleLoadListener.getIdentifier(d.getVehicleId(), s.getStopFacility().getId(), i);
//		double expectedLengthOfTheTrain = this.vehLoad.getAdditionalLengthAtStopForVehicle(vehDepId);
//		double expectedWeightOfTheTrain = this.vehLoad.getAdditionalWeightAtStopForVehicle(vehDepId);
//		Double maxLoadOfTrain = (Double) this.locomotiveAttribs.getAttribute(d.getVehicleId().toString(), 
//				WagonSimConstants.TRAIN_MAX_WEIGHT);
//		Double maxLengthOfTrain = (Double) this.locomotiveAttribs.getAttribute(d.getVehicleId().toString(), 
//				WagonSimConstants.TRAIN_MAX_LENGTH);
//		Double lengthOfWagon = (Double) this.wagonAttribs.getAttribute(person.getId().toString(), WagonSimConstants.WAGON_LENGTH);
//		Double weightOfWagon = (Double) this.wagonAttribs.getAttribute(person.getId().toString(), WagonSimConstants.WAGON_GROSS_WEIGHT);
//		// check if the data is available (it should). otherwise, throw error 
//		if((lengthOfWagon == null) || (weightOfWagon == null) || (maxLengthOfTrain == null) || (maxLoadOfTrain == null) ){
//			throw new RuntimeException("Either the locomotive or the wagon or the necessary attributes are unknown. " +
//					"This should not happen.");
//		}
//		if((maxLengthOfTrain < (expectedLengthOfTheTrain + lengthOfWagon)) || 
//				(maxLoadOfTrain < (expectedWeightOfTheTrain + weightOfWagon))){
//			// either the train is to long or to heavy, thus this connection should be unattractive
//			return true;
//		}
//		// no additional disutility, neither the connection is not available nor it is full. A not accessible connection should not be returned from delegate
//		return false;
		
		// punish if there is no free capacity
		return (!WagonSimVehicleLoadListener.freeCapacityInVehicle(
				d.getVehicleId().toString(), 
				s.getStopFacility().getId().toString(), 
				i, 
				person.getId().toString(), 
				vehLoad, 
				locomotiveAttribs, 
				wagonAttribs));
	}


	
	
	
	private static class MyComparator implements Comparator{
		@Override
		public int compare(Object o1, Object o2) {
			if((!(o1 instanceof DepartureImpl)) || (!(o2 instanceof DepartureImpl))){
				return 0;
			}
			Departure d1 = (Departure) o1;
			Departure d2 = (Departure) o2;
			
			if(d1.getDepartureTime() < d2.getDepartureTime()) return -1;
			if(d1.getDepartureTime() == d2.getDepartureTime()) return 0;
			return 1;
		}
		
	}

}

