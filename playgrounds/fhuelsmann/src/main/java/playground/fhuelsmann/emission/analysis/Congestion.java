/* *********************************************************************** *
 * project: org.matsim.*
 * Congestion.java
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
package playground.fhuelsmann.emission.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import org.matsim.core.network.NetworkImpl;


/**
 * @author friederike
 * 
 * calculates the length of congestion on each link over a day; TO DO calculate the length of congestion per hour
 *
 */

public class Congestion implements LinkEnterEventHandler,LinkLeaveEventHandler {

//	private final static int NUM_OF_HOURS = 30;

	//private double[] congestionLength = new double[NUM_OF_HOURS];
	//private int[] travelDistanceCnt = new int[NUM_OF_HOURS];

	private final Map<Id, Double> linkenter = new HashMap<Id, Double>();
	Map<Id, Double> stopGoFractionSum = new HashMap<Id, Double>();

	private final Network network;

	public Congestion(final Network network) {
		this.network = network;

	}
	
	public void reset(final int iteration) {
		}
	
	public void handleEvent(LinkEnterEvent event) {
		this.linkenter.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(final LinkLeaveEvent event) {
		Id linkId=event.getLinkId();
		Id personId=event.getPersonId();
		Double enterTime = 0.0;
		Link link = this.network.getLinks().get(event.getLinkId());
		double freeTravelSpeed = link.getFreespeed();
		freeTravelSpeed = freeTravelSpeed*3.6;
		double stopGoSpeed= 12.4;
		double stopGoFraction = 0.0;
		double stopGoTime = 0.0;
		double distance = link.getLength(); 
		
		if(this.linkenter.containsKey(event.getPersonId())){
			enterTime = this.linkenter.get(personId);
		
			double travelTime = event.getTime()-enterTime;
			double averageSpeed=distance/travelTime;

				if (averageSpeed < stopGoSpeed){
					stopGoFraction = distance/1000;
				}
				if (averageSpeed > freeTravelSpeed){
					stopGoFraction = 0.0;
				}
				else {
					stopGoTime= (distance / 1000) / averageSpeed - (distance / 1000) / freeTravelSpeed;
					stopGoFraction = stopGoSpeed * stopGoTime;

				}
			}
			
		if(!stopGoFractionSum.containsKey(linkId)){
			stopGoFractionSum.put(linkId, stopGoFraction);
		}
		else{		
				Double previousValue = stopGoFractionSum.get(linkId);
				Double newValue = previousValue + stopGoFraction;
				stopGoFractionSum.put(linkId, newValue);
			//	if(linkId.toString().equals("576273431-592536888"))
			//	System.out.println("++++++++++++++++++++++++++++++linkId "+linkId+ " previousValue "+previousValue+" newValue "+newValue);
		}
	}
}
