/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author ssix
 */

public class LinkStatusSpy implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
	//private Scenario scenario;
	private Id linkId;
	private Map<Double, Id> EnteringAgents;
	private Map<Double, Id> LeavingAgents;
	//private int enteringCount = 0;
	//private int leavingCount = 0;
	
	public LinkStatusSpy (/*Scenario scenario,*/ Id linkId){
		//this.scenario = scenario;
		this.linkId = linkId;
		this.EnteringAgents = new HashMap<Double,Id>();
		this.LeavingAgents = new HashMap<Double,Id>(); 
	}

	
	public boolean sameLeavingOrderAsEnteringOrder(){
		//System.out.println(enteringCount+" <> "+leavingCount);
		//Construction of two ArrayLists<Person>, that contain respectively entering and leaving agents chronologically ordered.
		SortedSet<Double> sortedKeys = new TreeSet<Double>();
		sortedKeys.addAll(EnteringAgents.keySet());
		//System.out.println("Put "+sortedKeys.size()+" times in the sorted set.");
		ArrayList<Id> EnteringAgentsOrdered = new ArrayList<Id>();
		/*
		//Method 1
		Iterator<Double> it = sortedKeys.iterator();//is supposed to iterate going chronologically forward...
		while (it.hasNext()){
			Double key = it.next();
			EnteringAgentsOrdered.add(EnteringAgents.get(key));
		}
		sortedKeys.clear();
		sortedKeys.addAll(LeavingAgents.keySet());
		it=sortedKeys.iterator();
		ArrayList<Person> LeavingAgentsOrdered = new ArrayList<Person>();
		while (it.hasNext()){
			Double key = it.next();
			LeavingAgentsOrdered.add(LeavingAgents.get(key));
		}
		*/
		/*Method 2*/
		Double[] keySortedArray = (Double[]) sortedKeys.toArray(new Double[sortedKeys.size()]);
		for (int i = 0; i < keySortedArray.length; i++){
			Double time = keySortedArray[i];
			EnteringAgentsOrdered.add(EnteringAgents.get(time));
		}
		//System.out.println("Put "+EnteringAgentsOrdered.size()+" agents in EnteringAgentsOrdered");
		System.out.println("EnteringAgentsOrdered: "+EnteringAgentsOrdered.toString());
		sortedKeys.clear();
		sortedKeys.addAll(LeavingAgents.keySet());
		ArrayList<Id> LeavingAgentsOrdered = new ArrayList<Id>();
		keySortedArray = (Double[]) sortedKeys.toArray(new Double[sortedKeys.size()]);
		for (int i = 0; i < keySortedArray.length; i++){
			Double time = keySortedArray[i];
			LeavingAgentsOrdered.add(LeavingAgents.get(time));
		}
		System.out.println("Put "+LeavingAgentsOrdered.size()+" agents in LeavingAgentsOrdered:");
		//System.out.println("LeavingAgentsOrdered: "+LeavingAgentsOrdered.toString());
		//*/
		
		//Comparing those two ordered lists in order to find out the wanted result		
		if (LeavingAgents.size() != EnteringAgents.size()){
			System.out.println("Some Agents have been lost or not yet accounted for. Different numbers of cars have entered and left the link!");
			return false;
		} else {
			for (int i=0; i<EnteringAgentsOrdered.size(); i++){
				if (!(EnteringAgentsOrdered.get(i).equals(LeavingAgentsOrdered.get(i))))
					return false; 
			}
		}
		return true;
	}
	
	public boolean didXLeaveLinkBeforeY(Id x, Id y){
		if ((LeavingAgents.containsValue(x)) && (LeavingAgents.containsValue(y))){
			Double keyX = new Double("0");
			Double keyY = new Double("0");
			for (Double key : LeavingAgents.keySet()){
				if (LeavingAgents.get(key).equals(x)){
					keyX = key;
				} else if (LeavingAgents.get(key).equals(y)) {
					keyY = key;
				} else {
					//Do nothing
				}
			}
			if (Double.compare(keyX, keyY) == -1){
				return true;
			}
		} else {
			System.out.println("The Person arguments do not belong to the simulation.");
			return false;
		}
		return false;
	}
	
	public void handleEvent(LinkEnterEvent event){
		if (event.getLinkId().equals(this.linkId)){
			//enteringCount++;
			//System.out.println("Time an agent enters: "+event.getTime());
			this.EnteringAgents.put(new Double(event.getTime()), event.getPersonId());
		}

	}
	
	public void handleEvent(LinkLeaveEvent event){
		if (event.getLinkId().equals(this.linkId)){
			//leavingCount++;
			this.LeavingAgents.put(new Double(event.getTime()),event.getPersonId());
		}
	}
	
	public void reset(int iteration){
		this.EnteringAgents.clear();
		this.LeavingAgents.clear();
		//this.enteringCount = 0;
		//this.leavingCount = 0;
	}


	public Id getLinkId() {
		return linkId;
	}

}
