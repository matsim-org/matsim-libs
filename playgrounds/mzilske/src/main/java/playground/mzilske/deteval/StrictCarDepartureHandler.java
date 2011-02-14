/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mzilske.deteval;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;

import playground.mrieser.core.mobsim.api.DepartureHandler;
import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.api.NewMobsimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.MobsimVehicle;
import playground.mrieser.core.mobsim.features.NetworkFeature;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

public class StrictCarDepartureHandler implements DepartureHandler {

	private final static Logger log = Logger.getLogger(StrictCarDepartureHandler.class);

	private final NetworkFeature networkFeature;
	private final NewMobsimEngine engine;

	public StrictCarDepartureHandler(final NewMobsimEngine engine, final NetworkFeature networkFeature, final Scenario scenario) {
		this.engine = engine;
		this.networkFeature = networkFeature;
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		MobsimLink link = this.networkFeature.getSimNetwork().getLinks().get(route.getStartLinkId());
		Person person = agent.getPlan().getPerson();
		Id vehId = person.getId();
		MobsimVehicle simVehicle = link.getParkedVehicle(vehId);
		if (simVehicle == null) {
			log.error("Agent " + person.getId() + " + wants to depart from link " + route.getStartLinkId() + ", but is missing its car. Removing agent from simulation.");
			engine.getEventsManager().processEvent(engine.getEventsManager().getFactory().createAgentStuckEvent(engine.getCurrentTime(), person.getId(), route.getStartLinkId(), leg.getMode()));
			return;
		}
		DriverAgent driver = new VehicleLeavingNetworkRouteDriver(agent, this.engine, route, simVehicle);
		simVehicle.setDriver(driver);

		driver.notifyMoveToNextLink();
		if (driver.getNextLinkId() == null) {
			this.engine.handleAgent(agent);
		} else {
			EventsManager eventsManager = this.engine.getEventsManager();
			eventsManager.processEvent(((EventsFactoryImpl) eventsManager.getFactory()).createPersonEntersVehicleEvent(engine.getCurrentTime(), person.getId(), simVehicle.getId(), null));
			link.continueVehicle(simVehicle);
		}
	}

}
