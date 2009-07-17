/* *********************************************************************** *
 * project: org.matsim.*
 * MyPrivateVehicleSpeedListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.CommercialModel.Listeners;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.network.NetworkLayer;

import playground.jjoubert.CommercialTraffic.SAZone;

public class MyPrivateVehicleSpeedAnalyser implements BasicLinkEnterEventHandler, 
													  BasicLinkLeaveEventHandler, 
													  BasicAgentArrivalEventHandler{
	private Map<Id, SAZone> map;
	private Map<Id, Double> eventMap;
	private NetworkLayer networkLayer;
	private Integer lowerId;
	private Integer upperId;
	
	public MyPrivateVehicleSpeedAnalyser(Map<Id, SAZone> map, NetworkLayer nl, int lowerId, int upperId){
		this.map = map;
		this.networkLayer = nl;
		this.lowerId = lowerId;
		this.upperId = upperId;
		this.eventMap = new TreeMap<Id, Double>();
	}

	public void handleEvent(BasicLinkEnterEvent event) {

		Id linkId = event.getLinkId();
		if(map.containsKey(linkId)){
			int thisPersonNumber = Integer.parseInt(event.getPersonId().toString());
			if( thisPersonNumber >= lowerId && thisPersonNumber <= upperId ){
				Id personId = event.getPersonId();
				eventMap.put(personId, event.getTime());				
			}
		}
		
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void handleEvent(BasicLinkLeaveEvent event) {
		if(eventMap.containsKey(event.getPersonId())){
			int hour = (int) Math.floor((event.getTime()) / 3600);
			Double speed = (this.networkLayer.getLink(event.getLinkId()).getLength() / 	// in meters 
						   (event.getTime() - eventMap.get(event.getPersonId()))) *		// in seconds
						   (3600/1000);													// convert m/s -> km/h 
			
			SAZone theZone = map.get(event.getLinkId());
			theZone.addToSpeedDetail(hour, speed);
			theZone.incrementSpeedCount(hour);
			
			eventMap.remove(event.getPersonId());
		}
		
	}

	public void handleEvent(BasicAgentArrivalEvent event) {
		eventMap.remove(event.getPersonId());		
	}



}
