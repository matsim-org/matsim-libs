/* *********************************************************************** *
 * project: org.matsim.*
 * DensityInfoCollector.java
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

package playground.wrashid.nan;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

public class DensityInfoCollector implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler{

	
	public static void main(String[] args) {
		
	}
	
	
	HashMap<Id, Double> linkEntranceTime=new HashMap<Id, Double>();
	HashMap<Id, Double> linkTravelTime=new HashMap<Id, Double>();
	
	// TODO:
	// statistics per link per time bin (matrix).
	// update the statistics 
	
	// define MobSimDEQSimNetralLinkEvents: => entrance time, link id, agent id, leave link time.
	// LinkTravelInformationHandler
	// handleEvent(LinkTraveledEvent event)
	
	// when defining the handler, you can specify the maximum speed on the links to filter out events on the ends, so that the simulation
	// makes sense... [in m/s]
	
	
	// define cutoff speed => anything above that should be cut out...
	// 
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (linkEntranceTime.get(event.getPersonId())==null){
			return;
		}
		
		double linkTravelTime=event.getTime() - linkEntranceTime.get(event.getPersonId());
		
		System.out.println("link travel time:" + linkTravelTime);
		
	}

	
	private void registerEnteringLink(){
		
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkEntranceTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		// TODO Auto-generated method stub
		
	}



}
