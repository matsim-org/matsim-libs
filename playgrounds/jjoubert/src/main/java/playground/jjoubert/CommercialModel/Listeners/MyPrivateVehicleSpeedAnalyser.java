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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.NetworkImpl;

import playground.jjoubert.CommercialTraffic.SAZone;

public class MyPrivateVehicleSpeedAnalyser implements LinkEnterEventHandler, 
													  LinkLeaveEventHandler, 
													  AgentArrivalEventHandler{
	private Map<Id, SAZone> map;
	private Map<Id, ArrayList<ArrayList<Double>>> linkSpeeds;
	private Map<Id, Double> eventMap;
	private NetworkImpl networkLayer;
	private Integer lowerAgentId;
	private Integer upperAgentId;
	private boolean weighLinksByUse = false;
	
	public MyPrivateVehicleSpeedAnalyser(Map<Id, SAZone> map, NetworkImpl nl, int lowerAgentId, int upperAgentId, int hours){
		this.map = map;
		this.networkLayer = nl;
		this.lowerAgentId = lowerAgentId;
		this.upperAgentId = upperAgentId;
		this.eventMap = new TreeMap<Id, Double>();
		
		linkSpeeds = new TreeMap<Id, ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<Double>> linkSpeedArray;
		for (Id key : this.map.keySet()) {
			linkSpeedArray = new ArrayList<ArrayList<Double>>(hours);
			ArrayList<Double> hourArray;
			for(int i = 0; i < hours; i++){
				hourArray = new ArrayList<Double>();
				linkSpeedArray.add(hourArray);				
			}
			linkSpeeds.put(key, linkSpeedArray);			
		}
	}

	public void handleEvent(LinkEnterEvent event) {

		Id linkId = event.getLinkId();
		if(map.containsKey(linkId)){
			int thisPersonNumber = Integer.parseInt(event.getPersonId().toString());
			if( thisPersonNumber >= lowerAgentId && thisPersonNumber <= upperAgentId ){
				Id personId = event.getPersonId();
				eventMap.put(personId, event.getTime());				
			}
		}
		
	}

	public void reset(int iteration) {
		
	}

	/**
	 * When handling the link-leave event, two implementations are done:
	 * <ul>
	 * 		<li> The speed across the link is calculated and added to the associated <b><i>zone</i></b>. 
	 * 			 This has the implication that the more a link is used, the more it's speed will 
	 * 			 impact the associated zone's average speed. So, if we are interested to weigh
	 * 			 the links according to their use - the appropriate method would be 
	 * 			 <code>addSpeedToZone</code>.
	 * 		<li> The speed across the link is calculated and added to the <b><i>link</i></b>. Once all
	 * 			 events are processed, the average speed for each link is determined. If no activities
	 * 			 occurred on the specific link, the free speed of the link is used. The average speed
	 * 			 for the zone is then calculated as the average speed of all the links associated with
	 * 			 the zone. Each link hence is weighed equally. If this is what we want then us the 
	 * 			 method <code>addSpeedToLink</code>. <i>(I still foresee a bias here: even if a link
	 * 			 has only <b>one</b> event, it will be used to calculate the average speed. Maybe this
	 * 			 is not an issue since then event's speed will probably be the free speed anyway.)</i>  
	 * </ul> 
	 */
	public void handleEvent(LinkLeaveEvent event) {
		if(this.weighLinksByUse){
			addSpeedToZone(event);			
		} else{
			addSpeedToLink(event);
		}
	}
	
	
	public void handleEvent(AgentArrivalEvent event) {
		eventMap.remove(event.getPersonId());		
	}

	
	private void addSpeedToZone(LinkLeaveEvent event){
		if(eventMap.containsKey(event.getPersonId())){
			int hour = (int) Math.floor((event.getTime()) / 3600);
			Double speed = (this.networkLayer.getLinks().get(event.getLinkId()).getLength() / 	// in meters 
					(event.getTime() - eventMap.get(event.getPersonId()))) *			// in seconds
					(3600/1000);														// convert m/s -> km/h 
			
			SAZone theZone = map.get(event.getLinkId());
			theZone.addToSpeedDetail(hour, speed);
			theZone.incrementSpeedCount(hour);
			
			eventMap.remove(event.getPersonId());
		}		
	}

	
	public void addSpeedToLink(LinkLeaveEvent event) {
		if(eventMap.containsKey(event.getPersonId())){
			int hour = (int) Math.floor((event.getTime()) / 3600);
			Double speed = (this.networkLayer.getLinks().get(event.getLinkId()).getLength() / 	// in meters 
					(event.getTime() - eventMap.get(event.getPersonId()))) *			// in seconds
					(3600/1000);														// convert m/s -> km/h 
			
			linkSpeeds.get(event.getLinkId()).get(hour).add(speed);
			
			eventMap.remove(event.getPersonId());
		}
		
	}
	
	public void doAnalysis(){
		/*
		 * TODO Calculate the average speed for each link during every hour. If a link does not have 
		 * ANY speed entries for a given hour, use the free speed of the link for that hour. Once all
		 * hours has an average speed, calculate an average speed for the link over the day.
		 * 
		 * TODO Maybe consider also keeping track of the min, median, and max? But that might be a 
		 * separate analysis for select zones.
		 */
		if(!this.weighLinksByUse){
			//TODO Calculate the average speed, or assign free speed if no events were recorded.
			for (Id linkKey : linkSpeeds.keySet()) {
				SAZone theZone = map.get(linkKey);
				ArrayList<ArrayList<Double>> link = linkSpeeds.get(linkKey);
				for (int hour = 0; hour < link.size(); hour++) {
					ArrayList<Double> speeds = link.get(hour);
					double total = 0;
					double avgSpeed = 0;
					if(speeds.size() > 0){
						for (Double speed : speeds) {
							total += speed;
						}
						avgSpeed = total / speeds.size();
					} else{			
						avgSpeed = this.networkLayer.getLinks().get(linkKey).getFreespeed(System.currentTimeMillis()) * (3600/1000);
					}
					theZone.addToSpeedDetail(hour, avgSpeed);
					theZone.incrementSpeedCount(hour);
				}
			}
		} 
//		else{
//			//TODO Must rewrite the addSpeedToZone method so that analysis can be done HERE.
//		}
		
	}


}
