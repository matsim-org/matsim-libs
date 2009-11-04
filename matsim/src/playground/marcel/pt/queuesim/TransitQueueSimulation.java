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

package playground.marcel.pt.queuesim;

import java.rmi.RemoteException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

import playground.marcel.pt.otfvis.FacilityDrawer;
import playground.marcel.pt.routes.ExperimentalTransitRoute;

/**
 * @author mrieser
 */
public class TransitQueueSimulation extends QueueSimulation {

	private static final Logger log = Logger.getLogger(TransitQueueSimulation.class);

	private OnTheFlyServer otfServer = null;

	private TransitSchedule schedule = null;
	/*package*/ final TransitStopAgentTracker agentTracker;
	private final HashMap<PersonImpl, DriverAgent> agents = new HashMap<PersonImpl, DriverAgent>(100);

	public TransitQueueSimulation(final ScenarioImpl scenario, final EventsManagerImpl events) {
		super(scenario, events);

		this.schedule = scenario.getTransitSchedule();
		this.setAgentFactory(new TransitAgentFactory(this, this.agents));
		this.agentTracker = new TransitStopAgentTracker();
	}

	public void startOTFServer(final String serverName) {
		this.otfServer = OnTheFlyServer.createInstance(serverName, this.network, this.plans, getEvents(), false);
		this.otfServer.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(this.schedule, this.agentTracker));
		try {
			this.otfServer.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void cleanupSim() {
		if (this.otfServer != null) {
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

	public Object getAgent(final PersonImpl p) {
		return this.agents.get(p);
	}

	@Override
	protected void createAgents() {
		super.createAgents();

		if (this.schedule != null) {
			BasicVehicles vehicles = ((ScenarioImpl) this.scenario).getVehicles();

			for (TransitLine line : this.schedule.getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (Departure departure : route.getDepartures().values()) {
						TransitDriver driver = new TransitDriver(line, route, departure, this.agentTracker, this);
						if (departure.getVehicleId() == null) {
							throw new NullPointerException("no vehicle id set for departure " + departure.getId() + " in route " + route.getId() + " from line " + line.getId());
						}
						TransitQueueVehicle veh = new TransitQueueVehicle(vehicles.getVehicles().get(departure.getVehicleId()), 5);
						veh.setDriver(driver);
						driver.setVehicle(veh);
						QueueLink qlink = this.network.getQueueLink(driver.getCurrentLeg().getRoute().getStartLinkId());
						qlink.addParkedVehicle(veh);

						this.scheduleActivityEnd(driver);
						Simulation.incLiving();
					}
				}
			}
		}
	}

	@Override
	public void agentDeparts(final DriverAgent agent, final Link link) {
		LegImpl leg = agent.getCurrentLeg();
		if (leg.getMode() == TransportMode.pt) {
			if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
				log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + agent.getPerson().getId().toString());
				log.info("route: " + leg.getRoute().getClass().getCanonicalName() + " " + (leg.getRoute() instanceof GenericRoute ? ((GenericRoute) leg.getRoute()).getRouteDescription() : ""));
				Simulation.decLiving();
				Simulation.incLost();
				return;
			}
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			if (route.getAccessStopId() == null) {
				// looks like this agent has a bad transit route, likely no route could be calculated for it
				Simulation.decLiving();
				Simulation.incLost();
				log.error(
						"Agent has bad transit route! agentId=" + agent.getPerson().getId()
								+ " route=" + route.getRouteDescription()
								+ ". The agent is removed from the simulation.");
				return;
			}
			getEvents().processEvent(new AgentDepartureEventImpl(SimulationTimer.getTime(), agent.getPerson(), (LinkImpl) link, leg));

			TransitStopFacility stop = this.schedule.getFacilities().get(route.getAccessStopId());
			this.agentTracker.addAgentToStop((PassengerAgent) agent, stop);
		} else {
			super.agentDeparts(agent, link);
		}
	}

}
