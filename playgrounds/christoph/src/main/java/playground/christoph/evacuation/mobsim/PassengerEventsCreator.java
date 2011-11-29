/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerEventsCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.mobsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

/**
 * Duplicates the events of given driver agents to the passengers
 * in the same vehicle.
 * 
 * To duplicate the events, some calls to quite internal methods of
 * the QSim are necessary - therefore the code might be a bit crappy...
 * 
 * It would be nice to be able to directly add agents as passengers
 * to a QVehicle. Probably a future ToDo...
 * 
 * So far, only ride_passenger is supported. However, other modes like
 * walk_passenger could be added (the slowest person of a group walks,
 * all other persons follow with the same speed).
 * 
 * @author cdobler
 */
public class PassengerEventsCreator implements AgentDepartureEventHandler, AgentArrivalEventHandler,
	LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler, 
	SimulationInitializedListener, SimulationAfterSimStepListener, DepartureHandler {

	public final static String passengerTransportMode = "ride_passenger";
	
	private final EventsManager eventsManager;
	private final Map<Id, MobsimAgent> agents;
	private final Map<Id, List<Id>> driverPassengerMap;
	private final Map<Id, Leg> currentDriverLegs;
	private final List<Id> departedDrivers;
	
	public PassengerEventsCreator(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
		
		this.agents = new HashMap<Id, MobsimAgent>();
		this.departedDrivers = new ArrayList<Id>();
		this.driverPassengerMap = new HashMap<Id, List<Id>>();
		this.currentDriverLegs = new HashMap<Id, Leg>();
	}
	
	public void addDriverPassengersSet(Id driverId, List<Id> passengerIds) {
		this.driverPassengerMap.put(driverId, passengerIds);
	}
	
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (agent.getMode().equals(passengerTransportMode)) {
			if ( agent instanceof MobsimDriverAgent ) {
				handlePassengerDeparture(now, (MobsimDriverAgent)agent, linkId);
				return true;
			} else {
				throw new UnsupportedOperationException("Agent is not a passenger!") ;
			}
		}
		return false;
	}
	
	private void handlePassengerDeparture(double now, MobsimDriverAgent agent, Id linkId) {
		/*
		 * Anything to do here???
		 */
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		List<Id> passengers = driverPassengerMap.get(event.getPersonId());
		
		if (passengers != null) {
			departedDrivers.add(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		// In case the driver stops in the same time step as he has departed.
		departedDrivers.remove(event.getPersonId());
				
		List<Id> passengers = driverPassengerMap.get(event.getPersonId());
		
		if (passengers != null) {
			for (Id passengerId : passengers) {
				/*
				 * The AgentArrivalEvent for the passenger is created 
				 * within the endLegAndAssumeControl method. Moreover,
				 * the currently performed leg of the agent is ended.
				 */
				MobsimAgent passenger = agents.get(passengerId);
				passenger.endLegAndAssumeControl(event.getTime());				
			}
		}
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		List<Id> passengers = driverPassengerMap.get(event.getPersonId());
		
		if (passengers != null) {
			for (Id passengerId : passengers) {
				// set the next link within the logic of the agent
				((DriverAgent) this.agents.get(passengerId)).notifyMoveOverNode(event.getLinkId());
				((DriverAgent) this.agents.get(passengerId)).chooseNextLinkId();
				
				Event e = eventsManager.getFactory().createLinkEnterEvent(event.getTime(), passengerId, event.getLinkId(), null);
				eventsManager.processEvent(e);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		List<Id> passengers = driverPassengerMap.get(event.getPersonId());
		
		if (passengers != null) {
			for (Id passengerId : passengers) {
				Event e = eventsManager.getFactory().createLinkLeaveEvent(event.getTime(), passengerId, event.getLinkId(), null);
				eventsManager.processEvent(e);
			}
		}
	}
	
	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		List<Id> passengers = driverPassengerMap.get(event.getPersonId());
		
		if (passengers != null) {
			for (Id passengerId : passengers) {
				Event e = eventsManager.getFactory().createAgentWait2LinkEvent(event.getTime(), passengerId, event.getLinkId(), null);
				eventsManager.processEvent(e);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.agents.clear();
		this.departedDrivers.clear();
		this.currentDriverLegs.clear();
		this.driverPassengerMap.clear();
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		for (MobsimAgent agent : ((QSim)e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), agent);
		}
	}

	/*
	 * Copy the drivers route also to all passengers. We cannot do this when the departure
	 * event is created because probably some of the passengers have not yet departed
	 * during the time-step. 
	 */
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		for (Id driverId : departedDrivers) {
			PlanAgent driver = (PlanAgent) agents.get(driverId);
			Route driversRoute = ((Leg) driver.getCurrentPlanElement()).getRoute(); 
			List<Id> passengers = this.driverPassengerMap.get(driverId);
			
			if (passengers != null) {
				for (Id passengerId : passengers) {
					PlanAgent agent = (PlanAgent) agents.get(passengerId);
					Leg leg = (Leg) agent.getCurrentPlanElement();
					// Should we clone the route???
					leg.setRoute(driversRoute);
					
					// set the next link within the logic of the agent
					((DriverAgent) this.agents.get(passengerId)).chooseNextLinkId();
				}
			}
		}
		departedDrivers.clear();
	}
	
}