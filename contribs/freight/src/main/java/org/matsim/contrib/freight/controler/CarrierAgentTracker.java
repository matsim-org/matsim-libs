/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;

/**
 * This keeps track of all carrierAgents during simulation.
 * 
 * @author mzilske, sschroeder
 *
 */
public final class CarrierAgentTracker implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,
						     LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
						     PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler 
{
	// yyyy not sure if this _should_ be public, but current LSP design makes this necessary.  kai, sep'20

	private static final Logger log = Logger.getLogger( CarrierAgentTracker.class ) ;

	private final Carriers carriers;
	private final Vehicle2DriverEventHandler vehicle2DriverEventHandler = new Vehicle2DriverEventHandler();
	private final Collection<CarrierAgent> carrierAgents = new ArrayList<>();
	private final Map<Id<Person>, CarrierAgent> driverAgentMap = new HashMap<>();
	private final EventsManager events;
	private Collection<LSPEventCreator> lspEventCreators;

	CarrierAgentTracker( Carriers carriers, CarrierScoringFunctionFactory carrierScoringFunctionFactory, EventsManager events ) {
		this.events = events;
		this.carriers = carriers;

		for (Carrier carrier : this.carriers.getCarriers().values()) {
			carrierAgents.add( new CarrierAgent( carrier, carrierScoringFunctionFactory.createScoringFunction(carrier ) ) );
			// (since the tracker is recreated for every iteration, the agent and the scoring function are also recreated every iteration)
		}
	}
	public CarrierAgentTracker( Carriers carriers, Collection<LSPEventCreator> creators, EventsManager events ) {
		// yyyy needs to be public because of LSP. kai, sep'20

		this.carriers = carriers;
		this.lspEventCreators = creators;
		this.events = events;

		for (Carrier carrier : this.carriers.getCarriers().values()) {
			carrierAgents.add( new CarrierAgent( this, carrier ) );
		}

		Gbl.assertNotNull( this.lspEventCreators );
	}

	/**
	 * Request all carrier agents to score their plans.
	 * 
	 */
	void scoreSelectedPlans() {
		for (Carrier carrier : carriers.getCarriers().values()) {
			CarrierAgent agent = getCarrierAgentFromCarrier(carrier.getId() );
			agent.scoreSelectedPlan();
		}
	}

	@Override
	public void reset(int iteration) {
		vehicle2DriverEventHandler.reset(iteration );
	}

	void notifyEventHappened( Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId, int activityCounter ) {
		for( org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator lspEventCreator : lspEventCreators ) {
			Event customEvent = lspEventCreator.createEvent(event, carrier, activity, scheduledTour, driverId, activityCounter);
			if(customEvent != null) {
				events.processEvent(customEvent);
			}
		}
	}
	@Override
	public void handleEvent(ActivityEndEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		final Id<Person> driverId = vehicle2DriverEventHandler.getDriverOfVehicle( event.getVehicleId() );
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}


	@Override
	public void handleEvent(PersonArrivalEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2DriverEventHandler.handleEvent(event );
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2DriverEventHandler.handleEvent(event );
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		final Id<Person> driverId = vehicle2DriverEventHandler.getDriverOfVehicle( event.getVehicleId() );
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		final Id<Person> driverId = event.getPersonId();
		CarrierAgent carrierAgent = getCarrierAgentFromDriver( driverId );
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event, driverId );
	}
	// ---
	private CarrierAgent getCarrierAgentFromDriver( Id<Person> driverId ) {
		CarrierAgent carrier = driverAgentMap.get(driverId);
		if(carrier != null){
			return carrier;
		}
		for(CarrierAgent ca : carrierAgents){
			if(ca.getDriverIds().contains(driverId)){
				driverAgentMap.put(driverId, ca);
				return ca;
			}
		}
		return null;
	}
	private CarrierAgent getCarrierAgentFromCarrier( Id<Carrier> id ) {
		for (CarrierAgent agent : carrierAgents) {
			if (agent.getId().equals(id)) {
				return agent;
			}
		}
		return null;
	}
	CarrierDriverAgent getDriver(Id<Person> driverId){
		CarrierAgent carrierAgent = getCarrierAgentFromDriver(driverId );
		if(carrierAgent == null) throw new IllegalStateException("missing carrier agent. cannot find carrierAgent to driver " + driverId);
		return carrierAgent.getDriver(driverId);
	}
	Collection<CarrierAgent> getCarrierAgents(){
		return carrierAgents;
	}


}
