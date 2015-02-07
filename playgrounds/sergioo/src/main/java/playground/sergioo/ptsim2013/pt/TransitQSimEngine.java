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

package playground.sergioo.ptsim2013.pt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.SimpleTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.sergioo.ptsim2013.QSim;

/**
 * @author mrieser
 * @author mzilske
 */
public class TransitQSimEngine implements  DepartureHandler, MobsimEngine, AgentSource {


	private Collection<MobsimAgent> ptDrivers;

	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static Logger log = Logger.getLogger(TransitQSimEngine.class);

	private QSim qSim;

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private boolean useUmlaeufe = false;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	private AbstractTransitDriverFactory transitDriverFactory = new UmlaufDriverFactory();

	private InternalInterface internalInterface = null ;
	
	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
	}

	public TransitQSimEngine(QSim queueSimulation) {
		this.qSim = queueSimulation;
		this.schedule = queueSimulation.getScenario().getTransitSchedule();
		this.agentTracker = new TransitStopAgentTracker(this.qSim.getEventsManager());
	}

	// For tests (which create an Engine, and externally create Agents as well).
	public InternalInterface getInternalInterface() {
		return this.internalInterface;
	}

	@Override
	public void onPrepareSim() {
		//nothing to do here
	}


	@Override
	public void afterSim() {
		double now = this.qSim.getSimTimer().getTimeOfDay();
		for (Entry<Id<TransitStopFacility>, List<PTPassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {
			TransitStopFacility stop = this.schedule.getFacilities().get(agentsAtStop.getKey());
			for (PTPassengerAgent agent : agentsAtStop.getValue()) {
				this.qSim.getEventsManager().processEvent(new PersonStuckEvent( now, agent.getId(), stop.getLinkId(), ((MobsimAgent)agent).getMode()));
				this.qSim.getAgentCounter().decLiving();
				this.qSim.getAgentCounter().incLost();
			}
		}
	}

	private Collection<MobsimAgent> createVehiclesAndDriversWithUmlaeufe(TransitStopAgentTracker thisAgentTracker) {
		Scenario scenario = this.qSim.getScenario();
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		Vehicles vehicles = ((ScenarioImpl) scenario).getTransitVehicles();
		Collection<MobsimAgent> drivers = new ArrayList<MobsimAgent>();
		UmlaufCache umlaufCache = (UmlaufCache) scenario.getScenarioElement(UmlaufCache.ELEMENT_NAME) ;
		if (umlaufCache != null && umlaufCache.getTransitSchedule() == transitSchedule) { // has someone put a new transitschedule into the scenario?
			log.info("found pre-existing Umlaeufe in scenario, and the transit schedule is still the same, so using them.");
		} else {
			ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(scenario.getNetwork(),
					transitSchedule.getTransitLines().values(),
					vehicles,
					scenario.getConfig().planCalcScore());
			Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
			umlaufCache = new UmlaufCache(transitSchedule, umlaeufe);
			scenario.addScenarioElement(UmlaufCache.ELEMENT_NAME, umlaufCache); // possibly overwriting the existing one
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
		TransitQVehicle veh = new TransitQVehicle(vehicle);
		AbstractTransitDriver driver = this.transitDriverFactory.createTransitDriver(umlauf, thisAgentTracker, internalInterface );
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getVehicle()));
		driver.setVehicle(veh);
		Leg firstLeg = (Leg) driver.getNextPlanElement();
		Id<Link> startLinkId = firstLeg.getRoute().getStartLinkId();
		this.qSim.addParkedVehicle(veh, startLinkId);
		this.qSim.insertAgentIntoMobsim(driver); 
		return driver;
	}

	private Collection<MobsimAgent> createVehiclesAndDriversWithoutUmlaeufe(TransitSchedule schedule,
			TransitStopAgentTracker agentTracker) {
		Vehicles vehicles = ((ScenarioImpl) this.qSim.getScenario()).getTransitVehicles();
		Collection<MobsimAgent> drivers = new ArrayList<MobsimAgent>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (departure.getVehicleId() == null) {
						throw new NullPointerException("no vehicle id set for departure " + departure.getId() + " in route " + route.getId() + " from line " + line.getId());
					}
					TransitQVehicle veh = new TransitQVehicle(vehicles.getVehicles().get(departure.getVehicleId()));
					TransitDriver driver = new TransitDriver(line, route, departure, agentTracker, internalInterface );
					veh.setDriver(driver);
					veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getVehicle()));
					driver.setVehicle(veh);
					Id<Link> startLinkId = driver.getCurrentLeg().getRoute().getStartLinkId();
					this.qSim.addParkedVehicle(veh, startLinkId);
					this.qSim.insertAgentIntoMobsim(driver); 
					drivers.add(driver);
				}
			}
		}
		return drivers;
	}

	private void handleAgentPTDeparture(final MobsimAgent planAgent, Id<Link> linkId) {
		// this puts the agent into the transit stop.
		Id<TransitStopFacility> accessStopId = ((PTPassengerAgent) planAgent).getDesiredAccessStopId();
		if (accessStopId == null) {
			// looks like this agent has a bad transit route, likely no
			// route could be calculated for it
			log.error("pt-agent doesn't know to what transit stop to go. Removing agent from simulation. Agent " + planAgent.getId().toString());
			this.qSim.getAgentCounter().decLiving();
			this.qSim.getAgentCounter().incLost();
			return;
		}
		TransitStopFacility stop = this.schedule.getFacilities().get(accessStopId);
		if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
			double now = this.qSim.getSimTimer().getTimeOfDay();
			this.agentTracker.addAgentToStop(now, (PTPassengerAgent) planAgent, stop.getId());
			this.internalInterface.registerAdditionalAgentOnLink(planAgent) ;
		} else {
			throw new TransitAgentTriesToTeleportException("Agent "+planAgent.getId() + " tries to enter a transit stop at link "+stop.getLinkId()+" but really is at "+linkId+"!");
		}
	}


	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
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

	@Override
	public void insertAgentsIntoMobsim() {
		if (useUmlaeufe ) {
			ptDrivers = createVehiclesAndDriversWithUmlaeufe(this.agentTracker);
		} else {
			ptDrivers = createVehiclesAndDriversWithoutUmlaeufe(this.schedule, this.agentTracker);
		}
	}

	public Collection<MobsimAgent> getPtDrivers() {
		return Collections.unmodifiableCollection(ptDrivers);
	}


}