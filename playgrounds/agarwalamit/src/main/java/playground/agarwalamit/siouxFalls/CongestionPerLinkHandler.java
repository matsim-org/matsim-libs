/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * @author amit
 */
public class CongestionPerLinkHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	private final Logger logger = Logger.getLogger(CongestionPerLinkHandler.class);

	private Map<Double, Map<Id, Double>> delaysPerLink = new HashMap<Double, Map<Id, Double>>();
	private Map<Id, Map<Id, Double>> linkId2PersonIdLinkEnterTime = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Double> linkId2FreeSpeedLinkTravelTime = new HashMap<Id, Double>();
	private double totalDelay = 0;

	final int noOfTimeBins;
	final double timeBinSize;
	final Scenario scenario;
	public CongestionPerLinkHandler(int noOfTimeBins, double simulationEndTime, Scenario scenario){
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.scenario = scenario;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			linkId2PersonIdLinkEnterTime.put(link.getId(), new HashMap<Id, Double>());
			Double freeSpeedLinkTravelTime = Double.valueOf(Math.floor(link.getLength()/link.getFreespeed())+1);
			linkId2FreeSpeedLinkTravelTime.put(link.getId(), freeSpeedLinkTravelTime);
		}
	}

	@Override
	public void reset(int iteration) {
		this.delaysPerLink.clear();
		this.linkId2PersonIdLinkEnterTime.clear();
		this.linkId2FreeSpeedLinkTravelTime.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		if(linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId)){
			// Person is already on the link. Cannot happen.
			throw new RuntimeException();
		} 
		
		Map<Id, Double> personId2LinkEnterTime = linkId2PersonIdLinkEnterTime.get(linkId);
		double derivedLinkEnterTime = event.getTime()+1-linkId2FreeSpeedLinkTravelTime.get(linkId);
		personId2LinkEnterTime.put(personId, Double.valueOf(derivedLinkEnterTime));
		linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double time = event.getTime(); 
		if(time ==0.0) time = this.timeBinSize;
		double endOfTimeInterval = 0.0;
		endOfTimeInterval = Math.ceil(time/timeBinSize)*timeBinSize;
		if(endOfTimeInterval<=0.0)endOfTimeInterval=timeBinSize;
		Map<Id, Double> delayOnLink;

		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();

		double actualTravelTime = event.getTime()-linkId2PersonIdLinkEnterTime.get(linkId).get(personId);
		linkId2PersonIdLinkEnterTime.get(linkId).remove(personId);
		double freeSpeedTime = linkId2FreeSpeedLinkTravelTime.get(linkId);
		double currentDelay =	actualTravelTime-freeSpeedTime;
		
		if(currentDelay>=1.){
			totalDelay=totalDelay+currentDelay;
			if(delaysPerLink.get(endOfTimeInterval)!= null){
				delayOnLink = delaysPerLink.get(endOfTimeInterval);

				if(delayOnLink.get(linkId)!=null){
					double delaySoFar = delayOnLink.get(linkId);
					double delayNewValue = currentDelay+delaySoFar;
					delayOnLink.put(linkId, Double.valueOf(delayNewValue));
				} else {
					delayOnLink.put(linkId, Double.valueOf(currentDelay));
				}
			} else {
				delayOnLink = new HashMap<Id, Double>();
				delayOnLink.put(linkId, Double.valueOf(currentDelay));
				delaysPerLink.put(endOfTimeInterval, delayOnLink);
			}
		} else {
			// ignore delays less than 1 sec(delay=1sec are included) because of rounding errors.
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Double time = Double.valueOf(event.getTime());
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		if(linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId)){
			// Person is already on the link. Cannot happen.
			throw new RuntimeException();
		} 
		
		Map<Id, Double> personId2LinkEnterTime = linkId2PersonIdLinkEnterTime.get(linkId);
		personId2LinkEnterTime.put(personId, time);
		linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		linkId2PersonIdLinkEnterTime.get(event.getLinkId()).remove(event.getPersonId());
	}
	
	public Map<Double, Map<Id, Double>> getDelayPerLinkAndTimeInterval(){
		return this.delaysPerLink;
	}
	
	public double getTotalDelayInHours(){
		return totalDelay/3600;
	}
}