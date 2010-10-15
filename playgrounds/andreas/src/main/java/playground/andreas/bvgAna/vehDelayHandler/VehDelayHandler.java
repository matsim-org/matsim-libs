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

import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * Counts planned and realized departures at one stop for a specific route of a line.
 * <b>All</b> departures will be taken into account, regardless of the line or route served.
 * 
 * @author aneumann
 *
 */
public class VehDelayHandler implements VehicleDepartsAtFacilityEventHandler{
	
	private final Logger log = Logger.getLogger(VehDelayHandler.class);
	private final Level logLevel = Level.DEBUG;
	
	private TreeMap<Id, VehDelayAtStopContainer> stopId2DelayAtStopMap = new TreeMap<Id, VehDelayAtStopContainer>();
	
	public VehDelayHandler(){
		this.log.setLevel(this.logLevel);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		
		if(this.stopId2DelayAtStopMap.get(event.getFacilityId()) == null){
			this.log.debug("Adding new VehDelayAtStopContainer for stop " + event.getFacilityId() + " to map.");
			this.stopId2DelayAtStopMap.put(event.getFacilityId(), new VehDelayAtStopContainer(event.getFacilityId(), null, null));
			this.log.debug("Had to set lineId and routeId to null, due to missing information");
		}
		
		this.stopId2DelayAtStopMap.get(event.getFacilityId()).addDepartureEvent(event);		
	}
	
	/**
	 * Returns the data collected
	 * @return A map containing a <code>VehDelayAtStopContainer</code> for each stop id.
	 */
	public TreeMap<Id, VehDelayAtStopContainer> getStopId2DelayAtStopMap(){
		return this.stopId2DelayAtStopMap;
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");		
	}

}