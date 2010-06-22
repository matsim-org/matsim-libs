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

package playground.mrieser.core.sim.impl;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import playground.mrieser.core.sim.api.DepartureHandler;
import playground.mrieser.core.sim.api.DriverAgent;
import playground.mrieser.core.sim.api.NewSimEngine;
import playground.mrieser.core.sim.api.PlanAgent;
import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.features.NetworkFeature;
import playground.mrieser.core.sim.network.api.SimLink;

/**
 * @author mrieser
 */
public class CarDepartureHandler implements DepartureHandler {

	private final NetworkFeature networkFeature;
	private final NewSimEngine engine;

	public CarDepartureHandler(final NewSimEngine engine, final NetworkFeature networkFeature, final Scenario scenario) {
		this.engine = engine;
		this.networkFeature = networkFeature;
		placeVehicles(scenario);
	}

	private void placeVehicles(final Scenario scenario) {
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		for (Person p : scenario.getPopulation().getPersons().values()) {
			SimVehicle veh = new DefaultSimVehicle(new VehicleImpl(p.getId(), defaultVehicleType));
			SimLink link = this.networkFeature.getSimNetwork().getLinks().get(((Leg) p.getSelectedPlan().getPlanElements().get(1)).getRoute().getStartLinkId()); // TODO [MR] improve initialization
			link.insertVehicle(veh, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_PARKING);
		}
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		SimLink link = this.networkFeature.getSimNetwork().getLinks().get(route.getStartLinkId());
		SimVehicle simVehicle = link.getParkedVehicle(agent.getPlan().getPerson().getId());// TODO [MR] use vehicleId instead of personId
		DriverAgent driver = new NetworkRouteDriver(agent, this.engine, route, simVehicle);
		simVehicle.setDriver(driver);

		driver.notifyMoveToNextLink();
		if (driver.getNextLinkId() == null) {
			this.engine.handleAgent(agent);
		} else {
			link.continueVehicle(simVehicle);
		}
	}

}
