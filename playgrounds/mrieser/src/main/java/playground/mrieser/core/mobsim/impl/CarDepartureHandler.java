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

package playground.mrieser.core.mobsim.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import playground.mrieser.core.mobsim.api.DepartureHandler;
import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.api.NewSimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.SimVehicle;
import playground.mrieser.core.mobsim.features.NetworkFeature;
import playground.mrieser.core.mobsim.network.api.SimLink;

/**
 * This DepartureHandler assigns departing agents to a vehicle which is placed on
 * the network to be moved around. A vehicle will be created for each agent the first
 * time no vehicle can be found on the departing link. *
 * <br />
 * Requirements / Assumptions of the code:
 * <ul>
 * 	<li>handleDeparture() assumes the currentPlanElement of agents to be of type {@link Leg}</li>
 *  <li>handleDeparture() assumes the route of legs to be of type {@link NetworkRoute}</li>
 * </ul>
 *
 * @author mrieser
 */
public class CarDepartureHandler implements DepartureHandler {

	private final static Logger log = Logger.getLogger(CarDepartureHandler.class);

	private final NetworkFeature networkFeature;
	private final NewSimEngine engine;
	private final Map<Id, Id> vehicleLocations;
	private final VehicleType defaultVehicleType;
	private boolean teleportVehicles = false;

	public CarDepartureHandler(final NewSimEngine engine, final NetworkFeature networkFeature, final Scenario scenario) {
		this.engine = engine;
		this.networkFeature = networkFeature;
		this.vehicleLocations = new HashMap<Id, Id>((int) (scenario.getPopulation().getPersons().size() * 1.4));
		this.defaultVehicleType = new VehicleTypeImpl(new IdImpl("auto-generated vehicle"));
	}

	public void setTeleportVehicles(final boolean teleportVehicles) {
		this.teleportVehicles = teleportVehicles;
	}

	public boolean isTeleportVehicles() {
		return this.teleportVehicles;
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		SimLink link = this.networkFeature.getSimNetwork().getLinks().get(route.getStartLinkId());
		Id vehId = agent.getPlan().getPerson().getId(); // TODO [MR] use vehicleId instead of personId
		SimVehicle simVehicle = link.getParkedVehicle(vehId);
		if (simVehicle == null) {
			Id linkId = this.vehicleLocations.get(vehId);
			if (linkId == null) {
				simVehicle = new DefaultSimVehicle(new VehicleImpl(vehId, this.defaultVehicleType));
				link.insertVehicle(simVehicle, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_PARKING);
			} else if (this.teleportVehicles) {
				log.warn("Agent departs on link " + route.getStartLinkId() + ", but vehicle is on link " + linkId + ". Teleporting the vehicle.");
				SimLink link2 = this.networkFeature.getSimNetwork().getLinks().get(linkId);
				simVehicle = link2.getParkedVehicle(vehId);
				link2.removeVehicle(simVehicle);
				link.insertVehicle(simVehicle, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_PARKING);
			} else {
				log.error("Agent departs on link " + route.getStartLinkId() + ", but vehicle is on link " + linkId + ". Agent is removed from simulation.");
				return;
			}
		}
		this.vehicleLocations.put(vehId, route.getEndLinkId()); // vehicle should show up there later
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
