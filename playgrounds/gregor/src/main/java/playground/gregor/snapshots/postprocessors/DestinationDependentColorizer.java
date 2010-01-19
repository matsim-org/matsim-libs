/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationDependentColorizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.snapshots.postprocessors;

import java.util.HashMap;

import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.gregor.snapshots.writers.PositionInfo;


public class DestinationDependentColorizer implements PostProcessorI, LinkLeaveEventHandler {

	private final static int NUM_OF_COLOR_SLOTS = 256;
	
	private final HashMap<String,String> destNodeMapping = new HashMap<String,String>();


//	private final Population plans;
//
//	public DestinationDependentColorizer(Population plans){
//		this.plans = plans;
//	}

	
	public String[] processEvent(final String[] event) {
		String id = event[0];
		String color = getColor(id);
		event[15] = color;
		return event;
	}

	public void processPositionInfo(PositionInfo pos) {
		String color = getColor(pos.getId().toString());
		pos.setUserData(Integer.parseInt(color));
	}

	public String getColor(final String id) {
		if(!this.destNodeMapping.containsKey(id)){
			return "0";
		}
		return this.destNodeMapping.get(id);
	}

//	private synchronized void addMapping(Id id) {
//		Leg leg = ((Leg)this.plans.getPerson(id).getSelectedPlan().getActsLegs().get(1)); 
//		Id nodeId = leg.getRoute().getRoute().get(leg.getRoute().getRoute().size()-2).getId();
//		int mapping = Integer.parseInt(nodeId.toString()) % NUM_OF_COLOR_SLOTS; 
//		destNodeMapping.put(id,  Integer.toString(mapping));
//	}

//	public void handleEvent(EventAgentArrival event) {
//		
//		// TODO Auto-generated method stub
//		
//	}

	public void handleEvent(final LinkLeaveEvent event) {
		if (event.getLinkId().toString().contains("shelter")) {
			this.destNodeMapping.put(event.getPersonId().toString(),event.getLinkId().toString().replace("shelter", ""));
		} else if (event.getLinkId().toString().contains("rev_el")) {
			this.destNodeMapping.put(event.getPersonId().toString(),event.getLinkId().toString().replace("rev_el", ""));
		} else if (event.getLinkId().toString().contains("el")) {
			this.destNodeMapping.put(event.getPersonId().toString(),event.getLinkId().toString().replace("el", ""));
		}  else if (event.getLinkId().toString().contains("rev_")) {
			this.destNodeMapping.put(event.getPersonId().toString(),event.getLinkId().toString().replace("rev_", ""));
		}
	}

	public void reset(final int iteration) {
		// TODO Auto-generated method stub
		
	}


//	public void handleEvent(final AgentArrivalEvent event) {
//		
//		if (event.linkId.contains("el")) {
//			this.destNodeMapping.put(event.agentId,event.linkId.replace("el", ""));
//		}
//	}
}
