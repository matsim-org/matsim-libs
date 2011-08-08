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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

/**
 * @author mrieser
 * @author mzilske
 */
public class TransitQSimEngine implements  DepartureHandler, MobsimEngine {


	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static Logger log = Logger.getLogger(TransitQSimEngine.class);

	private Netsim qSim;

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private final Map<Person, MobsimAgent> agents = new HashMap<Person, MobsimAgent>(100);

	private boolean useUmlaeufe = false;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	private AbstractTransitDriverFactory transitDriverFactory = new UmlaufDriverFactory();

	public TransitQSimEngine(Netsim queueSimulation) {
		this.qSim = queueSimulation;
		this.schedule = ((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule();
		this.agentTracker = new TransitStopAgentTracker();
		afterConstruct();
	}

	private void afterConstruct() {
		qSim.setAgentFactory(new TransitAgentFactory(qSim, this.agents));
//		qSim.getNotTeleportedModes().add(TransportMode.pt);
	}


	@Override
	public Netsim getMobsim() {
		return this.qSim;
	}

	@Override
	public void onPrepareSim() {
		//nothing to do here
	}


	public Collection<MobsimAgent> createAdditionalAgents() {
		Collection<MobsimAgent> ptDrivers;
		if (useUmlaeufe ) {
			ptDrivers = createVehiclesAndDriversWithUmlaeufe(this.agentTracker);
		} else {
			ptDrivers = createVehiclesAndDriversWithoutUmlaeufe(this.schedule, this.agentTracker);
		}
		return ptDrivers;
	}


	@Override
	public void afterSim() {
		double now = this.qSim.getSimTimer().getTimeOfDay();
		for (Entry<Id, List<PassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {
			TransitStopFacility stop = this.schedule.getFacilities().get(agentsAtStop.getKey());

			for (PassengerAgent agent : agentsAtStop.getValue()) {
				this.qSim.getEventsManager().processEvent(
						new AgentStuckEventImpl(now, ((TransitAgent) agent).getPerson().getId(), stop.getLinkId(), 
								((TransitAgent) agent).getVehicle().getDriver().getMode()));

				this.qSim.getAgentCounter().decLiving();
				this.qSim.getAgentCounter().incLost();
			}
		}
	}

	private Collection<MobsimAgent> createVehiclesAndDriversWithUmlaeufe(TransitStopAgentTracker thisAgentTracker) {
		Scenario scenario = this.qSim.getScenario();
		TransitSchedule transitSchedule = ((ScenarioImpl) scenario).getTransitSchedule();
		Vehicles vehicles = ((ScenarioImpl) scenario).getVehicles();
		Collection<MobsimAgent> drivers = new ArrayList<MobsimAgent>();
		UmlaufCache umlaufCache = scenario.getScenarioElement(UmlaufCache.class) ;
		if (umlaufCache != null && umlaufCache.getTransitSchedule() == transitSchedule) { // has someone put a new transitschedule into the scenario?
			log.info("found pre-existing Umlaeufe in scenario, and the transit schedule is still the same, so using them.");
		} else {
			ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(scenario.getNetwork(),
					transitSchedule.getTransitLines().values(),
					vehicles,
					scenario.getConfig().planCalcScore());
			Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
			umlaufCache = new UmlaufCache(transitSchedule, umlaeufe);
			scenario.addScenarioElement(umlaufCache); // possibly overwriting the existing one
		}
		for (Umlauf umlauf : umlaufCache.getUmlaeufe()) {
			Vehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				MobsimAgent driver = createAndScheduleVehicleAndDriver(umlauf, basicVehicle, thisAgentTracker);
				drivers.add(driver);
			}
		}
		return drivers;
	}

	private AbstractTransitDriver createAndScheduleVehicleAndDriver(Umlauf umlauf,
			Vehicle vehicle, TransitStopAgentTracker thisAgentTracker) {
		TransitQVehicle veh = new TransitQVehicle(vehicle, 5);
		AbstractTransitDriver driver = this.transitDriverFactory.createTransitDriver(umlauf, thisAgentTracker, this.qSim);
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getVehicle()));
		driver.setVehicle(veh);
		Leg firstLeg = (Leg) driver.getNextPlanElement();
		NetsimLink qlink = this.qSim.getNetsimNetwork().getNetsimLinks().get(firstLeg.getRoute().getStartLinkId());
		if ( qlink==null ) {
			throw new RuntimeException("did not find link from transit route with id: " + firstLeg.getRoute().getStartLinkId() + " in network; aborting ...") ;
		}
		qlink.addParkedVehicle(veh);
		// yyyyyy this could, in principle, also be a method mobsim.addVehicle( ..., linkId), and then the qnetwork
		// would not need to be exposed at all.  kai, may'10

		this.qSim.scheduleActivityEnd(driver);
		this.qSim.getAgentCounter().incLiving();
		return driver;
	}

	private Collection<MobsimAgent> createVehiclesAndDriversWithoutUmlaeufe(TransitSchedule schedule,
			TransitStopAgentTracker agentTracker) {
		Vehicles vehicles = ((ScenarioImpl) this.qSim.getScenario()).getVehicles();
		Collection<MobsimAgent> drivers = new ArrayList<MobsimAgent>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (departure.getVehicleId() == null) {
						throw new NullPointerException("no vehicle id set for departure " + departure.getId() + " in route " + route.getId() + " from line " + line.getId());
					}
					TransitQVehicle veh = new TransitQVehicle(vehicles.getVehicles().get(departure.getVehicleId()), 5);
					TransitDriver driver = new TransitDriver(line, route, departure, agentTracker, this.qSim);
					veh.setDriver(driver);
					veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getVehicle()));
					driver.setVehicle(veh);
					NetsimLink qlink = this.qSim.getNetsimNetwork().getNetsimLinks().get(driver.getCurrentLeg().getRoute().getStartLinkId());
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

//	public void beforeHandleAgentArrival(PersonAgent agent) {
//
//	}
	// this method is not used anywhere.  kai, nov'10

	private void handleAgentPTDeparture(final MobsimAgent planAgent, Id linkId) {
		// this puts the agent into the transit stop.
		Leg leg = ((TransitAgent)planAgent).getCurrentLeg() ;
		
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + planAgent.getId().toString());
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
						+ planAgent.getId() + " route="
						+ route.getRouteDescription()
						+ ". The agent is removed from the simulation.");
			} else {
				TransitStopFacility stop = this.schedule.getFacilities().get(route.getAccessStopId());
				if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
					this.agentTracker.addAgentToStop((PassengerAgent) planAgent, stop.getId());
					this.getMobsim().registerAgentAtPtWaitLocation(planAgent) ;
				} else {
					throw new TransitAgentTriesToTeleportException("Agent "+planAgent.getId() + " tries to enter a transit stop at link "+stop.getLinkId()+" but really is at "+linkId+"!");
				}
			}
		}
	}


	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (agent.getMode().equals(TransportMode.pt)) {
			handleAgentPTDeparture(agent, linkId);
			return true ;
		}
		return false ;
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
		this.transitDriverFactory = abstractTransitDriverFactory;
	}

	@Override
	public void doSimStep(double time) {
		// Nothing to do here.
	}


}