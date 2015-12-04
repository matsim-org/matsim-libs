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
package playground.ikaddoura.analysis.congestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class CarCongestionHandlerAdvanced implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, TransitDriverStartsEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {

	private final static Logger log = Logger.getLogger(CarCongestionHandlerAdvanced.class);
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	
	private final Network network;
	private Map<Id<Person>, Double> personId2enteringTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Boolean> personId2justDeparted = new HashMap<Id<Person>, Boolean>();
	private double delay = 0.;

	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<Id<Vehicle>>();
	private final List<Id<Person>> ptDriverIDs = new ArrayList<Id<Person>>();
	
	public CarCongestionHandlerAdvanced(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.personId2enteringTime.clear();
		this.personId2justDeparted.clear();
		
		delegate.reset(iteration);

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
	public void handleEvent(LinkEnterEvent event) {

		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
		
		} else {
			// car!
			this.personId2justDeparted.put(delegate.getDriverOfVehicle(event.getVehicleId()), false);
			this.personId2enteringTime.put(delegate.getDriverOfVehicle(event.getVehicleId()), event.getTime());
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
	
		} else {
			// car!
			double t0Link;
			
			if (this.personId2justDeparted.get(delegate.getDriverOfVehicle(event.getVehicleId()))){
				// person just started from an activity
				t0Link = 1.;
				
			} else {
				Link link = this.network.getLinks().get(event.getLinkId());
				t0Link = link.getLength() / link.getFreespeed();
			}
			
			double tActLink = event.getTime() - this.personId2enteringTime.get(delegate.getDriverOfVehicle(event.getVehicleId()));
			double diff = tActLink - t0Link;
			
			if (diff < 0) {
				log.warn(delegate.getDriverOfVehicle(event.getVehicleId()) + " is faster than freespeed! Doesn't make sense!");
			}
			
			if (Math.abs(diff) <= 0.1){
				diff = 0;
			}
			
			this.delay = this.delay + diff;
						
			this.personId2enteringTime.put(delegate.getDriverOfVehicle(event.getVehicleId()), null);
		}
	}

	public double getTActMinusT0Sum() {
		return this.delay;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			
			this.personId2justDeparted.put(event.getPersonId(), true);
			this.personId2enteringTime.put(event.getPersonId(), event.getTime());
			
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);		
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);		
	}
	
}
