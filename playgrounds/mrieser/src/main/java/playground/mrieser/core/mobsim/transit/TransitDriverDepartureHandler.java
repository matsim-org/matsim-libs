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

package playground.mrieser.core.mobsim.transit;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.qsim.SimpleTransitStopHandlerFactory;
import org.matsim.pt.qsim.TransitStopHandlerFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.mrieser.core.mobsim.api.DepartureHandler;
import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.api.NewMobsimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.features.NetworkFeature;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

/**
 * @author mrieser
 */
public class TransitDriverDepartureHandler implements DepartureHandler {

	private final static Logger log = Logger.getLogger(TransitDriverDepartureHandler.class);

	private final NetworkFeature networkFeature;
	private final NewMobsimEngine engine;
	private final Map<Id, Id> vehicleLocations;
	private boolean teleportVehicles = false;
	private final TransitFeature ptFeature;
	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();
	private final Vehicles vehicles;

	public TransitDriverDepartureHandler(final NewMobsimEngine engine, final NetworkFeature networkFeature, final TransitFeature ptFeature, final Scenario scenario) {
		this.engine = engine;
		this.networkFeature = networkFeature;
		this.ptFeature = ptFeature;
		this.vehicles = ((ScenarioImpl) scenario).getVehicles();
		this.vehicleLocations = new HashMap<Id, Id>(10000);
	}

	public void setTransitStopHandlerFactory(final TransitStopHandlerFactory stopHandlerFactory) {
		this.stopHandlerFactory = stopHandlerFactory;
	}

	public void setTeleportVehicles(final boolean teleportVehicles) {
		this.teleportVehicles = teleportVehicles;
	}

	public boolean isTeleportVehicles() {
		return this.teleportVehicles;
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		if (!(agent instanceof TransitDriverPlanAgent)) {
			log.error("TransitDriverDepartureHandler only supports agents of type " + TransitDriverPlanAgent.class.getCanonicalName());
		}
		TransitDriverPlanAgent driver = (TransitDriverPlanAgent) agent;

		Leg leg = (Leg) agent.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		MobsimLink link = this.networkFeature.getSimNetwork().getLinks().get(route.getStartLinkId());
		Id vehId = driver.getCurrentUmlaufStueck().getDeparture().getVehicleId();
		TransitMobsimVehicle simVehicle = (TransitMobsimVehicle) link.getParkedVehicle(vehId);
		if (simVehicle == null) {
			Id linkId = this.vehicleLocations.get(vehId);
			if (linkId == null) {
				Vehicle veh = this.vehicles.getVehicles().get(vehId);
				simVehicle = new DefaultTransitMobsimVehicle(veh, 5.0, this.stopHandlerFactory.createTransitStopHandler(veh));
				link.insertVehicle(simVehicle, MobsimLink.POSITION_AT_TO_NODE, MobsimLink.PRIORITY_PARKING);
			} else if (this.teleportVehicles) {
				log.warn("Transit vehicles should depart on link " + route.getStartLinkId() + ", but is on link " + linkId + ". Teleporting the vehicle.");
				MobsimLink link2 = this.networkFeature.getSimNetwork().getLinks().get(linkId);
				simVehicle = (TransitMobsimVehicle) link2.getParkedVehicle(vehId);
				link2.removeVehicle(simVehicle);
				link.insertVehicle(simVehicle, MobsimLink.POSITION_AT_TO_NODE, MobsimLink.PRIORITY_PARKING);
			} else {
				log.error("Agent departs on link " + route.getStartLinkId() + ", but vehicle is on link " + linkId + ". Agent is removed from simulation.");
				return;
			}
		}
		this.vehicleLocations.put(vehId, route.getEndLinkId()); // vehicle should show up there later
		DriverAgent simDriver = new TransitDriverAgent(driver, this.engine, route, simVehicle, this.ptFeature);
		simVehicle.setDriver(simDriver);

		simDriver.notifyMoveToNextLink();
		if (simDriver.getNextLinkId() == null) {
			this.engine.handleAgent(agent);
		} else {
			link.continueVehicle(simVehicle);
		}
	}


}
