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

package playground.vsp.andreas.bvgAna.level2;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.OptionalTime;

import playground.vsp.andreas.bvgAna.level1.StopId2RouteId2DelayAtStopMap;
import playground.vsp.andreas.bvgAna.level1.StopId2RouteId2DelayAtStopMapData;

/**
 * Searches the next planned and next realized departures for a given stop and time. Calculates the number of missed vehicles.
 *
 * @author aneumann
 *
 */
public class VehiclePlannedRealizedMissedDepartures {

	private final Logger log = LogManager.getLogger(VehiclePlannedRealizedMissedDepartures.class);
//	private final Level logLevel = Level.OFF;

	private final String planned = "planned";
	private final String realized = "realized";

	private TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>> stopId2Route2DelayAtStopMap;


	public VehiclePlannedRealizedMissedDepartures(StopId2RouteId2DelayAtStopMap vehicleDelayHandler){
//		this.log.setLevel(this.logLevel);
		this.stopId2Route2DelayAtStopMap = vehicleDelayHandler.getStopId2RouteId2DelayAtStopMap();
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
		Tuple<OptionalTime, Integer> plannedDeparture = this.getNextDepartureTime(stopId, plannedDepartureTime, lineId, routeId, this.planned);
		Tuple<OptionalTime, Integer> realizedDeparture = this.getNextDepartureTime(stopId, realizedDepartureTime, lineId, routeId, this.realized);

		int numberOfMissedVehicles = realizedDeparture.getSecond() - plannedDeparture.getSecond();

		if(log.getLevel() == Level.DEBUG){
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
		return this.getNextDepartureTime(stopId, time, lineId, routeId, this.planned).getFirst().seconds();
	}

	/**
	 * @param stopId The stop
	 * @param time The time
	 * @param lineId
	 * @param routeId
	 * @return Returns the next realized departure time in the future for a given stop and time.
	 */
	public double getNextRealizedDepartureTime(Id stopId, double time, Id lineId, Id routeId){
		return this.getNextDepartureTime(stopId, time, lineId, routeId, this.realized).getFirst().seconds();
	}

	private Tuple<OptionalTime, Integer> getNextDepartureTime(Id stopId, double time, Id lineId, Id routeId, String type){
		this.log.debug("TransitAgent.getEnterTransitRoute only checks for line id and stops to come ignoring route id. Agent probably too fast in simulation compared to plan.");
		TreeMap<Id, StopId2RouteId2DelayAtStopMapData> possibleRoutes = this.stopId2Route2DelayAtStopMap.get(stopId);
		if (possibleRoutes == null) {
			/* this could happen if a stop is served only by few lines (e.g. night-buses), but the simulation
			 * stops before ever a single vehicle could reach that stop
			 */
			return new Tuple<>(OptionalTime.undefined(), 1);
		}

		Tuple<OptionalTime, Integer> closestDepartureTimeAndSlot = new Tuple<>(OptionalTime.defined(Double.POSITIVE_INFINITY), Integer.MIN_VALUE);

		for (StopId2RouteId2DelayAtStopMapData delayContainer : possibleRoutes.values()) {
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
					if(departures.get(i) > time){
						if(departures.get(i) < closestDepartureTimeAndSlot.getFirst().seconds()){
							closestDepartureTimeAndSlot = new Tuple<OptionalTime, Integer>(OptionalTime.defined(departures.get(i)), i);
						}
					}
				}
			}
		}

		if(closestDepartureTimeAndSlot.getFirst().seconds() == Double.POSITIVE_INFINITY){
			this.log.warn("Could not find next " + type + " departure time for stop " + stopId + ", line " + lineId + " route " + routeId + " and time " + time + "s. Returning positive Inf");
		}
		return closestDepartureTimeAndSlot;
	}
}
