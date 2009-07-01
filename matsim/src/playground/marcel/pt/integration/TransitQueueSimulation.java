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
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.transitSchedule.TransitStopFacility;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

import playground.marcel.pt.otfvis.FacilityDrawer;
import playground.marcel.pt.transitSchedule.api.Departure;
import playground.marcel.pt.transitSchedule.api.TransitLine;
import playground.marcel.pt.transitSchedule.api.TransitRoute;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;

public class TransitQueueSimulation extends QueueSimulation {
	
	private OnTheFlyServer otfServer = null;

	private TransitSchedule schedule = null;
	/*package*/ final TransitStopAgentTracker agentTracker;
	private final HashMap<PersonImpl, DriverAgent> agents = new HashMap<PersonImpl, DriverAgent>(100);

	public TransitQueueSimulation(final Network network, final Population population, final Events events) {
		super(network, population, events);
	
		this.setAgentFactory(new TransitAgentFactory(this, this.agents));
		this.agentTracker = new TransitStopAgentTracker();
	}

	public void startOTFServer(final String serverName) {
		this.otfServer = OnTheFlyServer.createInstance(serverName, this.network, this.plans, getEvents(), false);
		try {
			this.otfServer.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void cleanupSim() {
		if (otfServer != null) {
			this.otfServer.cleanup();
		}
		super.cleanupSim();
	}
	
	@Override
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);
		if (this.otfServer != null) {
			this.otfServer.updateStatus(time);
		}
	}

	public void setTransitSchedule(final TransitSchedule schedule) {
		this.schedule = schedule;
		if (this.otfServer != null) {
			this.otfServer.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(this.schedule, this.agentTracker));
		}
	}

	public Object getAgent(final PersonImpl p) {
		return this.agents.get(p);
	}

	@Override
	protected void createAgents() {
		super.createAgents();

		if (this.schedule != null) {

			BasicVehicleType vehicleType = new BasicVehicleTypeImpl(new IdImpl("transitVehicleType"));
			BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
			capacity.setSeats(Integer.valueOf(101));
			capacity.setStandingRoom(Integer.valueOf(0));
			vehicleType.setCapacity(capacity);
			
			for (TransitLine line : this.schedule.getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (Departure departure : route.getDepartures().values()) {
						TransitDriver driver = new TransitDriver(line, route, departure, this);

						TransitQueueVehicle veh = new TransitQueueVehicle(new BasicVehicleImpl(driver.getPerson().getId(), vehicleType), 5);
						veh.setDriver(driver);
						driver.setVehicle(veh);
						departure.setVehicle(veh.getBasicVehicle());
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
		LegImpl leg = agent.getCurrentLeg();
		if (leg.getMode() == TransportMode.pt) {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			TransitStopFacility stop = this.schedule.getFacilities().get(route.getAccessStopId());
			this.agentTracker.addAgentToStop(agent, stop);
		} else {
			super.agentDeparts(agent, link);
		}
	}

}
