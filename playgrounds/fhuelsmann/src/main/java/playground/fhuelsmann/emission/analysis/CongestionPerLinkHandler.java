/* *********************************************************************** *
 * project: org.matsim.*
 * CongestionPerLinkHandler.java
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


/**
 * @author friederike 
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

public class CongestionPerLinkHandler implements LinkEnterEventHandler,LinkLeaveEventHandler {
	

	private final Map<Id, Double> linkenter = new HashMap<Id, Double>();
	Map<Double, Map<Id,Double>> stopGoFractionSum = new HashMap<Double, Map<Id,Double>>();

	private final Network network;
	private final int noOfTimeBins;
	private final double timeBinSize;


	public CongestionPerLinkHandler(final Network network,double simulationEndTime, int noOfTimeBins) {
		this.network = network;
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
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
		double endOfTimeInterval = 0.0;
		
		
/**accumulated ratio of actual and freeflow travel time per link	**/
		
		Link link = this.network.getLinks().get(event.getLinkId());
	  	double freeTravelSpeed = link.getFreespeed();
		double stopGoFraction = 0.0;
		double distance = link.getLength(); 
	  
	  if(this.linkenter.containsKey(event.getPersonId())){
			enterTime = this.linkenter.get(personId);
			double travelTime = event.getTime()-enterTime;
			double freeflowTime= distance / freeTravelSpeed;

			if (travelTime <= freeflowTime){
				stopGoFraction = 0.0;
				}
			else {
				stopGoFraction = travelTime /freeflowTime;
				//	if(linkId.toString().equals("576273431-592536888"))
				//	System.out.println("linkId "+linkId+ " stopGoFraction "+stopGoFraction);
			}
		
			for(int i = 0; i < noOfTimeBins; i++){
				if(enterTime > i * timeBinSize && enterTime <= (i + 1) * timeBinSize){
					endOfTimeInterval = (i + 1) * timeBinSize;
					Map<Id, Double> linkId2stopGoFraction = new HashMap<Id,  Double>();;
				
					if(stopGoFractionSum.get(endOfTimeInterval) != null){
						linkId2stopGoFraction = stopGoFractionSum.get(endOfTimeInterval);

						if(linkId2stopGoFraction.get(linkId) != null){
							double warmEmissionsSoFar = linkId2stopGoFraction.get(linkId);
					
							Double newValue = stopGoFraction + warmEmissionsSoFar;
						
							linkId2stopGoFraction.put(linkId, newValue);
						} else {
							linkId2stopGoFraction.put(linkId, stopGoFraction);
						}
					} else {
						linkId2stopGoFraction.put(linkId, stopGoFraction);
				}	
				stopGoFractionSum.put(endOfTimeInterval, linkId2stopGoFraction);
				//if(linkId.toString().equals("576273431-592536888"))
				//	System.out.println("############################endOfTimeInterval "+endOfTimeInterval+ " aggegatedCongestion "+linkId2stopGoFraction);
				}
			}
	  }
	}
	
		
/**accumulated demand per link	**/
 	
 /*		if(this.linkenter.containsKey(event.getPersonId())){
			enterTime = this.linkenter.get(personId);
			
	
			for(int i = 0; i < noOfTimeBins; i++){
				if(enterTime > i * timeBinSize && enterTime <= (i + 1) * timeBinSize){
					endOfTimeInterval = (i + 1) * timeBinSize;
					Map<Id, Double> counts = new TreeMap<Id,  Double>();
					
					if (stopGoFractionSum.get(endOfTimeInterval) !=null){
						counts = stopGoFractionSum.get(endOfTimeInterval);
						
						if(counts.get(linkId) != null){
							double value = counts.get(linkId);
							Double newValue = value + 1.0;
							counts.put(linkId, newValue);
						}
						else {
							counts.put(linkId, 1.0);
						}
							
					} else {
						counts.put(linkId, 1.0);
					}
					stopGoFractionSum.put(endOfTimeInterval, counts);
				//	if(linkId.toString().equals("576273431-592536888"))
					//	System.out.println("############################endOfTimeInterval "+endOfTimeInterval+ " counts "+counts);
				}
			}
		}
	}*/
				
/**accumulated congestion length per link	**/
  	
	/*		Link link = this.network.getLinks().get(event.getLinkId());
	 		double freeTravelSpeed = link.getFreespeed();
			double stopGoFraction = 0.0;
			double distance = link.getLength(); 
	  		freeTravelSpeed = freeTravelSpeed*3.6;
 			double stopGoSpeed= 15.9152;
 			double stopGoTime = 0.0;

			
			
			
			 if(this.linkenter.containsKey(event.getPersonId())){
					enterTime = this.linkenter.get(personId);
					double travelTime = event.getTime()-enterTime;
					double averageSpeed=(distance/1000)/(travelTime/3600);
					
					
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
			
			for(int i = 0; i < noOfTimeBins; i++){
				if(enterTime > i * timeBinSize && enterTime <= (i + 1) * timeBinSize){
					endOfTimeInterval = (i + 1) * timeBinSize;
					Map<Id, Double> linkId2stopGoFraction = new HashMap<Id,  Double>();;
					
					if(stopGoFractionSum.get(endOfTimeInterval) != null){
						linkId2stopGoFraction = stopGoFractionSum.get(endOfTimeInterval);

						if(linkId2stopGoFraction.get(linkId) != null){
						double warmEmissionsSoFar = linkId2stopGoFraction.get(linkId);
						
								Double newValue = stopGoFraction + warmEmissionsSoFar;
							
								linkId2stopGoFraction.put(linkId, newValue);
						} else {
							linkId2stopGoFraction.put(linkId, stopGoFraction);
						}
					} else {
						linkId2stopGoFraction.put(linkId, stopGoFraction);
					}
					stopGoFractionSum.put(endOfTimeInterval, linkId2stopGoFraction);
					//if(linkId.toString().equals("576273431-592536888"))
					//	System.out.println("############################endOfTimeInterval "+endOfTimeInterval+ " aggegatedCongestion "+linkId2stopGoFraction);
				}
			}
		}
	}*/
	
	public Map<Double, Map<Id, Double>> getCongestionPerLinkAndTimeInterval() {
		Map<Double, Map<Id, Double>>stopGoFractionSum = new HashMap<Double, Map<Id,Double>>();

		for(Entry<Double, Map<Id, Double>> entry0 : this.stopGoFractionSum.entrySet()){
			Double endOfTimeInterval = entry0.getKey();
			Map<Id, Double> linkId2congestion = entry0.getValue();
			Map<Id, Double> linkId2congestionAsString = new HashMap<Id, Double>();

				for (Entry<Id, Double> entry1: linkId2congestion.entrySet()){
					Id linkId = entry1.getKey();
					Double value = entry1.getValue();
					linkId2congestionAsString.put(linkId, value);	
				}
				stopGoFractionSum.put(endOfTimeInterval, linkId2congestionAsString);
		}
		return stopGoFractionSum;
	}

}
