/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.vehDelayHandler;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

/**
 * Searches the next planned and next realized departures for a given stop and time. Calculates the number of missed vehicles.
 * 
 * @author aneumann
 *
 */
public class VehDelayAnalyzer {
	
	private final Logger log = Logger.getLogger(VehDelayAnalyzer.class);
	private final Level logLevel = Level.DEBUG;
	
	private final String planned = "planned";
	private final String realized = "realized";
	
	private TreeMap<Id, TreeMap<Id, VehDelayAtStopContainer>> stopId2Route2DelayAtStopMap;
	
	
	public VehDelayAnalyzer(VehDelayHandler vehicleDelayHandler){
		this.log.setLevel(this.logLevel);
		this.stopId2Route2DelayAtStopMap = vehicleDelayHandler.getStopId2Route2DelayAtStopMap();
	}
	
	/**
	 * Tries to approximate the number of vehicles an agent has missed due to its delay.
	 * The results may get confused in case vehicles of one specific route<br>
	 * a) suffer from severe delay and <br>
	 * b) are allowed to overtake each other.<br>
	 * e.g. an agent has a positive delay (is 10 minutes late) but due to the vehicles running late as well,
	 * the agent can enter a vehicle running ahead of the scheduled one. 
	 * 
	 * @param stopId The stop
	 * @param plannedDepartureTime The time the agent was scheduled for departure
	 * @param realizedDepartureTime The time the agent departed in the simulation
	 * @param lineId
	 * @param routeId
	 * @return The number of missed vehicles, if positive. Negative number, if the agent could get some vehicles earlier. Zero, if the agent took the vehicle as scheduled.
	 */
	public int getNumberOfMissedVehicles(Id stopId, double plannedDepartureTime, double realizedDepartureTime, Id lineId, Id routeId){
		Tuple<Double, Integer> plannedDeparture = this.getNextDepartureTime(stopId, plannedDepartureTime, lineId, routeId, this.planned);
		Tuple<Double, Integer> realizedDeparture = this.getNextDepartureTime(stopId, realizedDepartureTime, lineId, routeId, this.realized);
		
		int numberOfMissedVehicles = realizedDeparture.getSecond().intValue() - plannedDeparture.getSecond().intValue();
		
		if(this.logLevel == Level.DEBUG){
			double delay = realizedDepartureTime - plannedDepartureTime;
			if(numberOfMissedVehicles < 0 && delay > 0){
				this.log.debug("Positive delay despite the fact that the agent could get a vehicle earlier. This could be right in case vehicles suffer from severe delay, please check.");
			}
			if(numberOfMissedVehicles > 0 && delay < 0){
				this.log.debug("Negative delay despite the fact that the agent missed its vehicle. This could be right in case vehicles are allowed to run ahead of schedule, please check.");
			}
		}
		return numberOfMissedVehicles;		
	}
	
	/** 
	 * @param stopId The stop
	 * @param time The time
	 * @param lineId
	 * @param routeId
	 * @return Returns the next planned departure time in the future for a given stop and time.
	 */
	public double getNextPlannedDepartureTime(Id stopId, double time, Id lineId, Id routeId){
		return this.getNextDepartureTime(stopId, time, lineId, routeId, this.planned).getFirst().doubleValue();
	}
	
	/** 
	 * @param stopId The stop
	 * @param time The time
	 * @param lineId
	 * @param routeId
	 * @return Returns the next realized departure time in the future for a given stop and time.
	 */
	public double getNextRealizedDepartureTime(Id stopId, double time, Id lineId, Id routeId){
		return this.getNextDepartureTime(stopId, time, lineId, routeId, this.realized).getFirst().doubleValue();
	}

	private Tuple<Double, Integer> getNextDepartureTime(Id stopId, double time, Id lineId, Id routeId, String type){
		this.log.debug("TransitAgent.getEnterTransitRoute only checks for line id and stops to come ignoring route id. Agent probably too fast in simulation compared to plan.");
		TreeMap<Id, VehDelayAtStopContainer> possibleRoutes = this.stopId2Route2DelayAtStopMap.get(stopId);		
		Tuple<Double, Integer> closestDepartureTimeAndSlot = new Tuple<Double, Integer>(new Double(Double.POSITIVE_INFINITY), new Integer(Integer.MIN_VALUE));
		
		for (VehDelayAtStopContainer delayContainer : possibleRoutes.values()) {
			
			if(delayContainer.getLineId().toString().equalsIgnoreCase(lineId.toString()) && delayContainer.getRouteId().toString().equalsIgnoreCase(routeId.toString())){
				ArrayList<Double> departures;
				if(type.equalsIgnoreCase(this.planned)){
					departures = delayContainer.getPlannedDepartures();
					this.log.debug("Searching planned departures");
				} else if (type.equalsIgnoreCase(this.realized) ){
					departures = delayContainer.getRealizedDepartures();
					this.log.debug("Searching realized departures");
				} else {
					this.log.error("Unknown type " + type + ". Don't know what to do. Returning positive Inf");
					return null;
				}
				for (int i = 0; i < departures.size(); i++) {
					if(departures.get(i).doubleValue() > time){
						if(departures.get(i).doubleValue() < closestDepartureTimeAndSlot.getFirst().doubleValue()){
							closestDepartureTimeAndSlot = new Tuple<Double, Integer>(new Double(departures.get(i).doubleValue()), new Integer(i));
						}					
					}
				}
			}
		}
		
		if(closestDepartureTimeAndSlot.getFirst().doubleValue() == Double.POSITIVE_INFINITY){
			this.log.warn("Could not find next " + type + " departure time for stop " + stopId + ", line " + lineId + " route " + routeId + " and time " + time + "s. Returning positive Inf");
		}
		return closestDepartureTimeAndSlot;
	}
}
