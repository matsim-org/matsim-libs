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
import java.util.TreeMap;
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
	

	private final Map<Id, Double> linkenter = new HashMap<Id, Double>();
	Map<Id, Map<Integer,Double>> stopGoFractionSum = new HashMap<Id, Map<Integer,Double>>();

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
			double timeClass = enterTime / 3600;
			int timeClassrounded = (int) timeClass+1;
			double travelTime = event.getTime()-enterTime;
			double averageSpeed=distance/travelTime;

			if (averageSpeed < stopGoSpeed){
					stopGoFraction = distance/1000;
				}
			else if (averageSpeed > freeTravelSpeed){
					stopGoFraction = 0.0;
				}
			else {
					stopGoTime= (distance / 1000) / averageSpeed - (distance / 1000) / freeTravelSpeed;
					stopGoFraction = stopGoSpeed * stopGoTime;
				//	if(linkId.toString().equals("576273431-592536888"))
					//	System.out.println("linkId "+linkId+ " stopGoFraction "+stopGoFraction);
			}
			
			if(!stopGoFractionSum.containsKey(linkId)){
			
				Map<Integer, Double> timeClass2CongLength = new TreeMap<Integer, Double>();
				timeClass2CongLength.put(timeClassrounded,stopGoFraction);
				stopGoFractionSum.put(linkId, timeClass2CongLength);
				if(linkId.toString().equals("576273431-592536888"))
				System.out.println("linkId "+linkId+ " timeClass2CongLength "+timeClass2CongLength);
			}
			else{		
				if(stopGoFractionSum.get(linkId).containsKey(timeClassrounded)){
					Map<Integer, Double> newValue = stopGoFractionSum.get(linkId);
					newValue.put(timeClassrounded,newValue.get(timeClassrounded)+stopGoFraction);
					stopGoFractionSum.put(linkId, newValue);
				}
				else{
					Map<Integer, Double> newValue = stopGoFractionSum.get(linkId);
			
					newValue.put(timeClassrounded,stopGoFraction);
					stopGoFractionSum.put(linkId, newValue);
					//if(linkId.toString().equals("576273431-592536888"))
					//	System.out.println("++++++++++++++++++++++++++++++linkId "+linkId+ " newValue "+newValue);
				}
			}
		}
	}
}
