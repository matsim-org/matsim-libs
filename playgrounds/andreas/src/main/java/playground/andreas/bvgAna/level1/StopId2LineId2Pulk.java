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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * Evaluates the headway between two vehicles following each other. If they are considered to bunch a <code>StopId2LineId2PulkData</code> entry is stored at the corresponding stop and line.
 * 
 * @author aneumann
 *
 */
public class StopId2LineId2Pulk implements TransitDriverStartsEventHandler, VehicleDepartsAtFacilityEventHandler{
	
	private final Logger log = Logger.getLogger(StopId2LineId2Pulk.class);
	private final Level logLevel = Level.DEBUG;
	
	private TreeMap<Id, Id> vehId2LineMap = new TreeMap<Id, Id>();
	private TreeMap<Id, TreeMap<Id, VehicleDepartsAtFacilityEvent>> stopId2LineId2VehDepEvent = new TreeMap<Id, TreeMap<Id,VehicleDepartsAtFacilityEvent>>();
	private TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>> stopId2LineId2PulkDataList = new TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>>();
	
	public StopId2LineId2Pulk(){
		this.log.setLevel(this.logLevel);
	}

	/**
	 * @return Returns a map containing all occurred bunching event in form of a <code>StopId2LineId2PulkData</code> entry for each stop sorted by line
	 */
	public TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>> getStopId2LineId2PulkDataList(){
		return this.stopId2LineId2PulkDataList;
	}
	
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if(this.stopId2LineId2VehDepEvent.get(event.getFacilityId()) == null){
			this.stopId2LineId2VehDepEvent.put(event.getFacilityId(), new TreeMap<Id, VehicleDepartsAtFacilityEvent>());
		}
		
		Id lineId = this.vehId2LineMap.get(event.getVehicleId());		
		VehicleDepartsAtFacilityEvent oldEvent = this.stopId2LineId2VehDepEvent.get(event.getFacilityId()).get(lineId);

		if(oldEvent != null){
			StopId2LineId2PulkData pulkData = new StopId2LineId2PulkData(lineId, oldEvent, event);

			if(pulkData.isPulk()){
				if(this.stopId2LineId2PulkDataList.get(event.getFacilityId()) == null){
					this.stopId2LineId2PulkDataList.put(event.getFacilityId(), new TreeMap<Id, List<StopId2LineId2PulkData>>());
				}

				if(this.stopId2LineId2PulkDataList.get(event.getFacilityId()).get(lineId) == null){
					this.stopId2LineId2PulkDataList.get(event.getFacilityId()).put(lineId, new ArrayList<StopId2LineId2PulkData>());
				}

				this.stopId2LineId2PulkDataList.get(event.getFacilityId()).get(lineId).add(pulkData);
			}
		}

		this.stopId2LineId2VehDepEvent.get(event.getFacilityId()).put(lineId, event);
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2LineMap.put(event.getVehicleId(), event.getTransitLineId());		
	}	
	
	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}
}
