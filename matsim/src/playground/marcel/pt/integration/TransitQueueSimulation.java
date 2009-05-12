/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulation.java
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

package playground.marcel.pt.integration;

import java.rmi.RemoteException;
import java.util.HashMap;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.mobsim.queuesim.QueueVehicleImpl;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.network.NetworkLayer;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

import playground.marcel.pt.otfvis.FacilityDrawer;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitSchedule;

public class TransitQueueSimulation extends QueueSimulation {
	
	private final OnTheFlyServer otfServer;
	

	private final Facilities facilities;
	private TransitSchedule schedule = null;
	/*package*/ final TransitStopAgentTracker agentTracker;
	private final HashMap<Person, DriverAgent> agents = new HashMap<Person, DriverAgent>(100);

	public TransitQueueSimulation(final NetworkLayer network, final Population population, final Events events, final Facilities facilities) {
		super(network, population, events);
		this.facilities = facilities;

		this.setAgentFactory(new TransitAgentFactory(this, this.agents));

		this.agentTracker = new TransitStopAgentTracker();
		
		this.otfServer = OnTheFlyServer.createInstance("OTFServer_Transit", this.network, this.plans, getEvents(), false);
		try {
			this.otfServer.pause();
			this.otfServer.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(this.facilities, this.agentTracker));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void cleanupSim() {
		this.otfServer.cleanup();
		super.cleanupSim();
	}
	
	@Override
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);
		this.otfServer.updateStatus(time);
	}

	public void setTransitSchedule(final TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public Object getAgent(final Person p) {
		return this.agents.get(p);
	}

	@Override
	protected void createAgents() {
		super.createAgents();

		if (this.schedule != null) {

			for (TransitLine line : this.schedule.getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (Departure departure : route.getDepartures().values()) {
						TransitDriver driver = new TransitDriver(line, route, departure, this);

						QueueVehicle veh = new QueueVehicleImpl(driver.getPerson().getId());
						veh.setDriver(driver);
						driver.setVehicle(new TransitQueueVehicle(20, getEvents()));
						QueueLink qlink = this.network.getQueueLink(driver.getCurrentLeg().getRoute().getStartLinkId());
						qlink.addParkedVehicle(veh);

						this.scheduleActivityEnd(driver);
						Simulation.incLiving();
					}
				}
			}
		}

	}
	
	public void agentDeparts(final DriverAgent agent, final Link link) {
		Leg leg = agent.getCurrentLeg();
		if (leg.getMode() == TransportMode.pt) {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			Facility stop = this.facilities.getFacilities().get(route.getAccessStopId());
			this.agentTracker.addAgentToStop(agent, stop);
		} else {
			super.agentDeparts(agent, link);
		}
	}

}
