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
package playground.ikaddoura.optimization.users;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

/**
 * @author Ihab
 *
 */
public class CarCongestionHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler {

	private final static Logger log = Logger.getLogger(CarCongestionHandler.class);
	private final Network network;
	private Map<Id<Person>, Double> personId2enteringTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2t0MinusTAkt = new HashMap<Id<Person>, Double>();
	
	public CarCongestionHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.personId2enteringTime.clear();
		this.personId2t0MinusTAkt.clear();
	}
	
	@Override
		public void handleEvent(LinkEnterEvent event) {

			if (event.getDriverId().toString().contains("person") && event.getVehicleId().toString().contains("person")){
				// a car is entering a link
				this.personId2enteringTime.put(event.getDriverId(), event.getTime());
			}
		}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {

		if (event.getDriverId().toString().contains("person") && event.getVehicleId().toString().contains("person")){
			// a car is leaving a link
			if (this.personId2enteringTime.get(event.getDriverId()) == null){
				// person just started from an activity, therefore not calculating travel times...
				
			} else {
				double tActLink = event.getTime() - this.personId2enteringTime.get(event.getDriverId());
				Link link = this.network.getLinks().get(event.getLinkId());
				double t0Link = link.getLength() / link.getFreespeed();
				double diff = t0Link - tActLink;
				
				if (diff > 0) {
					log.warn(event.getDriverId() + " is faster than freespeed! Doesn't make sense!");
				}
				
				if (Math.abs(diff) < 0.1){
					// Abweichung kleiner als eine 0.1 sec
					diff = 0;
				}
							
				if (this.personId2t0MinusTAkt.get(event.getDriverId()) == null){
					this.personId2t0MinusTAkt.put(event.getDriverId(), diff);
				} else {
					double diffSumThisPerson = this.personId2t0MinusTAkt.get(event.getDriverId());
					this.personId2t0MinusTAkt.put(event.getDriverId(), diffSumThisPerson + diff);
				}
				
				this.personId2enteringTime.put(event.getDriverId(), null);
			}
		}
	}

	public double getTActMinusT0Sum() {
		double diffSum = 0;
		for (Double diff : this.personId2t0MinusTAkt.values()){
			diffSum = diffSum + diff;
		}
		return diffSum;
	}
	
	public double getAvgTActMinusT0PerPerson() {
		int n = 0;
		double diffSum = 0;
		for (Double diff : this.personId2t0MinusTAkt.values()){
			diffSum = diffSum + diff;
			n++;
		}
		return diffSum / n;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		if (event.getPersonId().toString().contains("person")){
			// a car is departing
			this.personId2enteringTime.put(event.getPersonId(), null);
			
		}
	}
	
}
