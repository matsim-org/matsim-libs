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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class CarCongestionHandler implements TransitDriverStartsEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, 
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final static Logger log = Logger.getLogger(CarCongestionHandler.class);
	private final Network network;
	
	private Map<Id<Person>, Double> personId2enteringTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2t0MinusTAkt = new HashMap<Id<Person>, Double>();
	
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<>();
	private final List<Id<Person>> ptDriverIDs = new ArrayList<>();

	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public CarCongestionHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.personId2enteringTime.clear();
		this.personId2t0MinusTAkt.clear();
		this.ptVehicleIDs.clear();
		this.ptDriverIDs.clear();
		this.delegate.reset(iteration);
	}
	
	@Override
		public void handleEvent(LinkEnterEvent event) {

			if (!ptVehicleIDs.contains(event.getVehicleId())){
				// a car is entering a link
				this.personId2enteringTime.put(delegate.getDriverOfVehicle(event.getVehicleId()), event.getTime());
			}
		}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {

		Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
		
		if (!ptVehicleIDs.contains(event.getVehicleId())){
			// a car is leaving a link
			
			if (this.personId2enteringTime.get(driverId) == null){
				// person just started from an activity, therefore not calculating travel times...
				
			} else {
				double tActLink = event.getTime() - this.personId2enteringTime.get(driverId);
				Link link = this.network.getLinks().get(event.getLinkId());
				double t0Link = link.getLength() / link.getFreespeed();
				double diff = t0Link - tActLink;
				
				if (diff > 0) {
					log.warn(driverId + " is faster than freespeed! Doesn't make sense!");
				}
				
				if (Math.abs(diff) < 0.1){
					// Abweichung kleiner als eine 0.1 sec
					diff = 0;
				}
							
				if (this.personId2t0MinusTAkt.get(driverId) == null){
					this.personId2t0MinusTAkt.put(driverId, diff);
				} else {
					double diffSumThisPerson = this.personId2t0MinusTAkt.get(driverId);
					this.personId2t0MinusTAkt.put(driverId, diffSumThisPerson + diff);
				}
				
				this.personId2enteringTime.put(driverId, null);
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

		if (!this.ptDriverIDs.contains(event.getPersonId())){
			// a car is departing
			this.personId2enteringTime.put(event.getPersonId(), null);
			
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}	
		
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}	
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);		
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);		
	}
	
}
