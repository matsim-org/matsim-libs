/* *********************************************************************** *
 * project: org.matsim.*
 * FhEventHandling
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

package playground.fhuelsmann.emission;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;




public class TravelTimeCalculation implements LinkEnterEventHandler,LinkLeaveEventHandler, 
AgentArrivalEventHandler,AgentDepartureEventHandler {
	
	
	

	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new TreeMap<Id, Double>();

		
	//LinkID as key --> PersonID as key -->Strings sind in einer Liste gespeichert, wie z.B. SingleEvent 
	public Map<String,Map<String, LinkedList<String>>> getTravelTimes() {
		return travelTimes;
	}
	
/*	public double getNetworkLength() {
		return this.length;
	}*/

	private final Map<String,Map<String, LinkedList<String>>> travelTimes= new
	TreeMap<String,Map<String, LinkedList<String>>>();
	
	public Map<Id, Double> getLinkenter() {
		return linkenter;
	}

	public Map<Id, Double> getAgentarrival() {
		return agentarrival;
	}

	public Map<Id, Double> getAgentdeparture() {
		return agentdeparture;
	}


	public void reset(int iteration) {
		
		this.linkenter.clear();
		this.agentarrival.clear();
		this.agentdeparture.clear();
		//for attributes of the network
	
	
		System.out.println("reset...");
	}

	public void handleEvent(LinkEnterEvent event) {
		this.linkenter.put(event.getPersonId(), event.getTime());
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		this.agentarrival.put(event.getPersonId(), event.getTime());
	}
	
	public void handleEvent(AgentDepartureEvent event) {
		this.agentdeparture.put(event.getPersonId(), event.getTime());
	
	}

	public void handleEvent(LinkLeaveEvent event) {	
	
				
		if (this.linkenter.containsKey(event.getPersonId())) {
			
			Id personId= event.getPersonId();
			Id linkId = event.getLinkId();
						
			String personalId= personId.toString();
			//mit Aktivität
			if (this.agentarrival.containsKey(personId)) {
				double enterTime = this.linkenter.get(personId);
				double arrivalTime = this.agentarrival.get(personId);
				double departureTime = this.agentdeparture.get(personId);

				double travelTime = event.getTime() - enterTime -departureTime+ arrivalTime;
				
					
			
				this.agentarrival.remove(personId);
				
				//Data structure is built --> Map(Map(linkedList); 
				//Map is as string defined, could also be defined as double, integer (linkId), id
				//to avoid overriding the same linkID, if there are two links with the same LInkID the second one will be saved in the first one 
				if (this.travelTimes.get(linkId+"") != null){				
					//adds the Objects of the second LinkID to the List
					if (this.travelTimes.get(linkId+"").containsKey(personalId)){
						this.travelTimes.get(linkId+"").get(personalId+"").push("----mit Aktivität---,"+travelTime +","+  enterTime);
						}
					//List must be created when there is no PersonID
					else{
							LinkedList<String> list = new LinkedList<String>();
							list.push("----mit Aktivität---,"+travelTime + ","+ enterTime);
							this.travelTimes.get(linkId+"").put(personalId+"",list);}}
				else{
					
							LinkedList<String> list = new LinkedList<String>();
							list.push("----mit Aktivität---,"+travelTime + ","+ enterTime);
							Map<String,LinkedList<String>> map = new TreeMap<String,LinkedList<String>>();
							map.put(personalId+"", list);
							this.travelTimes.put(linkId+"",map);
							
				}
			}
			else { // Ohne Aktivität
					
					
					double enterTime = this.linkenter.get(personId);
					double travelTime = event.getTime() - enterTime;
												
				
					
					//System.out.println(personalId);
			//		System.out.println("TravelTime: " + travelTime);
					
			//		System.out.println("entertime: " + enterTime);
					
					if (this.travelTimes.get(linkId+"") != null){
						if (this.travelTimes.get(linkId+"").containsKey(personalId)){
							this.travelTimes.get(linkId+"").get(personalId).push("----mit Aktivität---,"+travelTime +","+  enterTime);
							}else{
								LinkedList<String> list = new LinkedList<String>();
								list.push("----ohne Aktivität---,"+travelTime + ","+ enterTime);
								this.travelTimes.get(linkId+"").put(personalId,list);}}
					else{
						
						//empty list is created
						LinkedList<String> list = new LinkedList<String>();
						//strings are saved is the list
						list.push("----ohne Aktivität---,"+travelTime +","+ enterTime);
						//empty map is created
						Map<String,LinkedList<String>> map = new TreeMap<String,LinkedList<String>>();
						//second map
						map.put(personalId, list);
						//second map is added to travelTimes map
						this.travelTimes.put(linkId +"",map);
						}				
					}
			}	
		}
	
					    
}
