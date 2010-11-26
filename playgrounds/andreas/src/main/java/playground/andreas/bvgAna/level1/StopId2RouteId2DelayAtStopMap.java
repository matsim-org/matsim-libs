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

package playground.andreas.bvgAna.level1;

import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * Counts planned and realized departures at one stop for a specific route of a line.
 * <b>All</b> departures will be taken into account, regardless of the line or route served.
 *
 * @author aneumann
 *
 */
public class StopId2RouteId2DelayAtStopMap implements VehicleDepartsAtFacilityEventHandler, TransitDriverStartsEventHandler{

	private final Logger log = Logger.getLogger(StopId2RouteId2DelayAtStopMap.class);
	private final Level logLevel = Level.OFF;

	private TreeMap<Id, TransitDriverStartsEvent> veh2LastStartsEvent = new TreeMap<Id, TransitDriverStartsEvent>();
	private TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>> stopId2RouteId2DelayAtStopMap = new TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>>();

	public StopId2RouteId2DelayAtStopMap(){
		this.log.setLevel(this.logLevel);
	}

	/**
	 * Returns the data collected
	 * @return A map containing a <code>VehDelayAtStopContainer</code> for each stop id and route id.
	 */
	public TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>> getStopId2RouteId2DelayAtStopMap(){
		return this.stopId2RouteId2DelayAtStopMap;
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {

		if(this.stopId2RouteId2DelayAtStopMap.get(event.getFacilityId()) == null){
			this.log.debug("Adding new TreeMap for stop " + event.getFacilityId() + " to map.");
			this.stopId2RouteId2DelayAtStopMap.put(event.getFacilityId(), new TreeMap<Id, StopId2RouteId2DelayAtStopMapData>());
		}

		TreeMap<Id, StopId2RouteId2DelayAtStopMapData> route2DelayMap = this.stopId2RouteId2DelayAtStopMap.get(event.getFacilityId());
		TransitDriverStartsEvent correspondingRouteServed = this.veh2LastStartsEvent.get(event.getVehicleId());

		if(correspondingRouteServed == null){
			this.log.warn("This should never happen");
		} else {

		if(route2DelayMap.get(correspondingRouteServed.getTransitRouteId()) == null){
			this.log.debug("Adding new VehDelayAtStopContainer for stop " + event.getFacilityId() + " and route " + correspondingRouteServed.getTransitRouteId() + " to map.");
			route2DelayMap.put(correspondingRouteServed.getTransitRouteId(), new StopId2RouteId2DelayAtStopMapData(event.getFacilityId(), correspondingRouteServed.getTransitLineId(), correspondingRouteServed.getTransitRouteId()));
		}

		route2DelayMap.get(correspondingRouteServed.getTransitRouteId()).addDepartureEvent(event);
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(this.veh2LastStartsEvent.get(event.getVehicleId()) != null){
			this.log.debug(event.getVehicleId() + " served route " + this.veh2LastStartsEvent.get(event.getVehicleId()).getTransitRouteId());
		} else {
			this.log.debug(event.getVehicleId() + " served no route");
		}
		this.veh2LastStartsEvent.put(event.getVehicleId(), event);
		this.log.debug(event.getVehicleId() + " now serves route " + this.veh2LastStartsEvent.get(event.getVehicleId()).getTransitRouteId());
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}
}