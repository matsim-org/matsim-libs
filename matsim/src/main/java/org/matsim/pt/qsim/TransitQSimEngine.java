/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.pt.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.ptproject.qsim.DepartureHandler;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QSimI;
import org.matsim.ptproject.qsim.SimEngine;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

/**
 * @author mrieser
 * @author mzilske
 */
public class TransitQSimEngine implements  DepartureHandler, SimEngine {


	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static Logger log = Logger.getLogger(TransitQSimEngine.class);

	private QSimI qSim;

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private final HashMap<Person, PersonDriverAgent> agents = new HashMap<Person, PersonDriverAgent>(100);

	private boolean useUmlaeufe = false;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();
	
	private AbstractTransitDriverFactory abstractTransitDriverFactory = new UmlaufDriverFactory();
	
	public TransitQSimEngine(QSimI queueSimulation) {
		this.qSim = queueSimulation;
		this.schedule = ((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule();
		this.agentTracker = new TransitStopAgentTracker();
		afterConstruct();
	}

	private void afterConstruct() {
		qSim.setAgentFactory(new TransitAgentFactory(qSim, this.agents));
		qSim.getNotTeleportedModes().add(TransportMode.pt);
	}
	

	@Override
	public QSimI getQSim() {
		return this.qSim;
	}

	@Override
	public void onPrepareSim() {
		//nothing to do here
	}


	public Collection<PersonAgent> createAgents() {
		TransitSchedule schedule = this.schedule;
		TransitStopAgentTracker agentTracker = this.agentTracker;
		Collection<PersonAgent> ptDrivers;
		if (useUmlaeufe ) {
			ptDrivers = createVehiclesAndDriversWithUmlaeufe(agentTracker);
		} else {
			ptDrivers = createVehiclesAndDriversWithoutUmlaeufe(schedule, agentTracker);
		}
		return ptDrivers;
	}


	public void afterSim() {
		double now = this.qSim.getSimTimer().getTimeOfDay();
		for (Entry<TransitStopFacility, List<PassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {

			for (PassengerAgent agent : agentsAtStop.getValue()) {
				this.qSim.getEventsManager().processEvent(
						new AgentStuckEventImpl(now, ((TransitAgent) agent).getPerson().getId(), agentsAtStop.getKey().getLinkId(), ((TransitAgent) agent).getVehicle().getDriver().getCurrentLeg().getMode()));

				this.qSim.getAgentCounter().decLiving();
				this.qSim.getAgentCounter().incLost();
			}
		}
	}

	private Collection<PersonAgent> createVehiclesAndDriversWithUmlaeufe(TransitStopAgentTracker thisAgentTracker) {
		Vehicles vehicles = ((ScenarioImpl) this.qSim.getScenario()).getVehicles();
		Collection<PersonAgent> drivers = new ArrayList<PersonAgent>();
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(this.qSim.getScenario().getNetwork(),((ScenarioImpl) this.qSim.getScenario()).getTransitSchedule().getTransitLines().values(), ((ScenarioImpl) this.qSim.getScenario()).getVehicles(), this.qSim.getScenario().getConfig().charyparNagelScoring());
		Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
		for (Umlauf umlauf : umlaeufe) {
			Vehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				PersonAgent driver = createAndScheduleVehicleAndDriver(umlauf, basicVehicle, thisAgentTracker);
				drivers.add(driver);
			}
		}
		return drivers;
	}

	private AbstractTransitDriver createAndScheduleVehicleAndDriver(Umlauf umlauf,
			Vehicle vehicle, TransitStopAgentTracker thisAgentTracker) {
		TransitQVehicle veh = new TransitQVehicle(vehicle, 5);
		AbstractTransitDriver driver = this.abstractTransitDriverFactory.createTransitDriver(umlauf, thisAgentTracker, this.qSim);
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getBasicVehicle()));
		driver.setVehicle(veh);
		QLink qlink = this.qSim.getQNetwork().getLinks().get(driver
				.getCurrentLeg().getRoute().getStartLinkId());
		qlink.addParkedVehicle(veh);
		// yyyyyy this could, in principle, also be a method mobsim.addVehicle( ..., linkId), and then the qnetwork
		// would not need to be exposed at all.  kai, may'10

		this.qSim.scheduleActivityEnd(driver);
		this.qSim.getAgentCounter().incLiving();
		return driver;
	}

	private Collection<PersonAgent> createVehiclesAndDriversWithoutUmlaeufe(TransitSchedule schedule,
			TransitStopAgentTracker agentTracker) {
		Vehicles vehicles = ((ScenarioImpl) this.qSim.getScenario()).getVehicles();
		Collection<PersonAgent> drivers = new ArrayList<PersonAgent>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (departure.getVehicleId() == null) {
						throw new NullPointerException("no vehicle id set for departure " + departure.getId() + " in route " + route.getId() + " from line " + line.getId());
					}
					TransitQVehicle veh = new TransitQVehicle(vehicles.getVehicles().get(departure.getVehicleId()), 5);
					TransitDriver driver = new TransitDriver(line, route, departure, agentTracker, this.qSim);
					veh.setDriver(driver);
					veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getBasicVehicle()));
					driver.setVehicle(veh);
					QLink qlink = this.qSim.getQNetwork().getLinks().get(driver.getCurrentLeg().getRoute().getStartLinkId());
					// yyyyyy this could, in principle, also be a method mobsim.addVehicle( ..., linkId), and then the qnetwork
					// would not need to be exposed at all.  kai, may'10
					qlink.addParkedVehicle(veh);
					this.qSim.scheduleActivityEnd(driver);
					this.qSim.getAgentCounter().incLiving();
					drivers.add(driver);
				}
			}
		}
		return drivers;
	}

	public void beforeHandleAgentArrival(PersonAgent agent) {

	}

	private void handleAgentPTDeparture(final PersonAgent agent, Id linkId, Leg leg) {
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + agent.getPerson().getId().toString());
			log.info("route: "
							+ leg.getRoute().getClass().getCanonicalName()
							+ " "
							+ (leg.getRoute() instanceof GenericRoute ? ((GenericRoute) leg.getRoute()).getRouteDescription() : ""));
			this.qSim.getAgentCounter().decLiving();
			this.qSim.getAgentCounter().incLost();
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			if (route.getAccessStopId() == null) {
				// looks like this agent has a bad transit route, likely no
				// route could be calculated for it
				this.qSim.getAgentCounter().decLiving();
				this.qSim.getAgentCounter().incLost();
				log.error("Agent has bad transit route! agentId="
						+ agent.getPerson().getId() + " route="
						+ route.getRouteDescription()
						+ ". The agent is removed from the simulation.");
			} else {
				TransitStopFacility stop = this.schedule.getFacilities().get(route.getAccessStopId());
				if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
					this.agentTracker.addAgentToStop((PassengerAgent) agent, stop);
				} else {
					throw new TransitAgentTriesToTeleportException("Agent "+agent.getPerson().getId() + " tries to enter a transit stop at link "+stop.getLinkId()+" but really is at "+linkId+"!");
				}
			}
		}
	}


	@Override
	public void handleDeparture(double now, PersonAgent agent, Id linkId, Leg leg) {
		if (leg.getMode() == TransportMode.pt) {
			handleAgentPTDeparture(agent, linkId, leg);
		}
	}

	public TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}

	public void setUseUmlaeufe(boolean useUmlaeufe) {
		this.useUmlaeufe = useUmlaeufe;
	}

	public void setTransitStopHandlerFactory(final TransitStopHandlerFactory stopHandlerFactory) {
		this.stopHandlerFactory = stopHandlerFactory;
	}
	
	public void setAbstractTransitDriverFactory(final AbstractTransitDriverFactory abstractTransitDriverFactory) {
		this.abstractTransitDriverFactory = abstractTransitDriverFactory;
	}


}