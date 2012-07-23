/* *********************************************************************** *
 * project: org.matsim.*
 * CarCongestionHandler.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author Ihab
 *
 */
public class CarCongestionHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, AgentDepartureEventHandler {

	private final static Logger log = Logger.getLogger(CarCongestionHandler.class);
	private final Network network;
	private Map<Id, Double> personId2enteringTime = new HashMap<Id, Double>();
	private double tActMinusT0;
	
	public CarCongestionHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.personId2enteringTime.clear();
		this.tActMinusT0 = 0.;
	}
	
	@Override
		public void handleEvent(LinkEnterEvent event) {

			if (event.getPersonId().toString().contains("person") && event.getVehicleId().toString().contains("person")){
				// a car is entering a link
				this.personId2enteringTime.put(event.getPersonId(), event.getTime());
				
			}
		}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {

		if (event.getPersonId().toString().contains("person") && event.getVehicleId().toString().contains("person")){
			// a car is leaving a link
			if (this.personId2enteringTime.get(event.getPersonId()) == null){
				// person just started from an activity, therefore not calculating travel times...
				
			} else {
				double tActLink = event.getTime() - this.personId2enteringTime.get(event.getPersonId());
				Link link = this.network.getLinks().get(event.getLinkId());
				double t0Link = link.getLength() / link.getFreespeed();
				double diff = t0Link - tActLink;
				
				if (diff > 0) {
					log.warn(event.getPersonId() + " is faster than freespeed! Doesn't make sense!");
				}
				
				if (Math.abs(diff) < 0.1){
					// Abweichung kleiner als eine 0.1 sec
					diff = 0;
				}
							
				log.warn("diff: " + diff);
				log.warn(event.getLinkId() + " " + event.getPersonId());
				tActMinusT0 = tActMinusT0 + diff;
				
				this.personId2enteringTime.put(event.getPersonId(), null);
			}
		}
	}

	public double gettActMinusT0() {
		return tActMinusT0;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {

		if (event.getPersonId().toString().contains("person")){
			// a car is departing
			this.personId2enteringTime.put(event.getPersonId(), null);
			
		}
	}
	
}
