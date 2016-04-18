/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.transEnergySim.visualization.charging.inductiveAtRoads;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingOutputLog;
import org.matsim.core.network.NetworkImpl;

/**
 * 
 * @author rashid_waraich
 *
 */
public class LinkVisualizationQueue {
	HashMap<Id, PriorityQueue<SortableMapObject<LinkEvent>>> processedEvents;
	private HashMap<Id, PriorityQueue<SortableMapObject<LinkEvent>>> linkEventQueues;
	private DoubleValueHashMap<Id> initValueAtLinks;
	private double previousQueryTime=-1.0;
	private DoubleValueHashMap<Id> currentValueAtLink;
	
	// TODO: perhaps save state later for improving performance
	//private DoubleValueHashMap<Id> currentTimeAtLink;
	//private DoubleValueHashMap<Id> currentValueAtLink;
	
	private double minValue = Double.MAX_VALUE;
	private double maxValue = -1 * Double.MAX_VALUE;


	public LinkVisualizationQueue(){
		linkEventQueues=new HashMap<Id, PriorityQueue<SortableMapObject<LinkEvent>>>();
		initValueAtLinks=new DoubleValueHashMap<Id>();
		processedEvents=new HashMap<Id, PriorityQueue<SortableMapObject<LinkEvent>>>();
		currentValueAtLink=new DoubleValueHashMap<Id>();
		
	}
	/*
	public void initValueAtLinks(){
		for (Id linkId:linkEventQueues.keySet()){
			double currentValue=0;
			Link link = network.getLinks().get(linkId);
			double freeSpeedTravelTimeBuffer= link.getLength()/ link.getFreespeed()*1.5;
			
			LinkedList<SortableMapObject<LinkEvent>> tmp=new LinkedList<SortableMapObject<LinkEvent>>();
			PriorityQueue<SortableMapObject<LinkEvent>> priorityQueue = linkEventQueues.get(linkId);
			
			while (true){
				if (priorityQueue.peek().getScore()<freeSpeedTravelTimeBuffer){
					currentValue+=object.getKey().getValue();
				}
				
				
			}
			
			for (SortableMapObject<LinkEvent> object:tmp){
				priorityQueue.add(object);
			}
			
		}
		
		
		
		
	}
	*/
	
	
	/**
	 * TODO: performance of this could be futher improved by separating this into several methods,
	 * where one method gives back only those links, which have changed.
	 * @param linkId
	 * @param time
	 * @return
	 */
	public double getValue(Id linkId, double time){
		if (time<previousQueryTime){
			resetPriorityQueues();
			resetCurrentValueAtLink();
		}
		previousQueryTime=time;
		
		PriorityQueue<SortableMapObject<LinkEvent>> priorityQueue = linkEventQueues.get(linkId);
		
		while (priorityQueue!=null && priorityQueue.size()>0 && priorityQueue.peek().getWeight()<=time){
			SortableMapObject<LinkEvent> poll = priorityQueue.poll();
			
			currentValueAtLink.incrementBy(linkId, poll.getKey().getValue());
			
			if (!processedEvents.containsKey(linkId)){
				processedEvents.put(linkId, new PriorityQueue<SortableMapObject<LinkEvent>>());
			}
			
			processedEvents.get(linkId).add(poll);
		}
		
		return currentValueAtLink.get(linkId);
	}
	
	private void resetPriorityQueues() {
		for (Id linkId:linkEventQueues.keySet()){
			for (SortableMapObject<LinkEvent> object:processedEvents.get(linkId)){
				linkEventQueues.get(linkId).add(object);
			}
			processedEvents.clear();
		}
	}



	public void addEvent(LinkEvent event) {
		Id linkId = event.getLinkId();
		if (!linkEventQueues.containsKey(linkId)){
			linkEventQueues.put(linkId,new PriorityQueue<SortableMapObject<LinkEvent>>());
		}
		
		linkEventQueues.get(linkId).add(new SortableMapObject<LinkEvent>(event, event.getTime()));

		if (event.getValue() < minValue) {
			minValue = event.getValue();
		}

		if (event.getValue() > maxValue) {
			maxValue = event.getValue();
		}
	}

	public double getMinimumValue() {
		return minValue;
	}

	public double getMaximumValue() {
		return maxValue;
	}
	
	public void resetCurrentValueAtLink(){
		currentValueAtLink=new DoubleValueHashMap<Id>();
		for (Id linkId:initValueAtLinks.keySet()){
			currentValueAtLink.put(linkId, initValueAtLinks.get(linkId));
		}
	}

	public void setInitValues(DoubleValueHashMap<Id> initValueAtLinks) {
		this.initValueAtLinks=initValueAtLinks;
		
		resetCurrentValueAtLink();
	}
}
