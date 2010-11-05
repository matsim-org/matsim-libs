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

import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * Collects <code>VehicleDepartsAtFacilityEvent</code> for each vehicle
 * 
 * @author aneumann
 *
 */
public class VehId2DelayAtStopMap implements VehicleDepartsAtFacilityEventHandler, TransitDriverStartsEventHandler{

	private final Logger log = Logger.getLogger(VehId2DelayAtStopMap.class);
	private final Level logLevel = Level.DEBUG;
	
	private TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>> vehId2DelayAtStopMap = new TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>>();
	
	public VehId2DelayAtStopMap(){
		this.log.setLevel(this.logLevel);
	}
	
	/**
	 * @return A map containing the <code>VehicleDepartsAtFacilityEvent</code> for each route starting with a <code>TransitDriverStartsEvent</code> for each vehicle
	 */
	public TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>> getVehId2DelayAtStopMap(){
		return this.vehId2DelayAtStopMap;
	}
	
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehId2DelayAtStopMap.get(event.getVehicleId()).getLast().addVehicleDepartsAtFacilityEvent(event);
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(this.vehId2DelayAtStopMap.get(event.getVehicleId()) == null){
			this.vehId2DelayAtStopMap.put(event.getVehicleId(), new LinkedList<VehId2DelayAtStopMapData>());
		}
		
		this.vehId2DelayAtStopMap.get(event.getVehicleId()).add(new VehId2DelayAtStopMapData(event));
	}
	
	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");		
	}
}
