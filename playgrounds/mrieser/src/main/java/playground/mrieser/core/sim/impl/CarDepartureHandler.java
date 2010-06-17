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

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;

import playground.mrieser.core.sim.api.DepartureHandler;
import playground.mrieser.core.sim.api.DriverAgent;
import playground.mrieser.core.sim.api.PlanAgent;
import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.features.NetworkFeature;
import playground.mrieser.core.sim.network.api.SimLink;

/**
 * @author mrieser
 */
public class CarDepartureHandler implements DepartureHandler {

	private final NetworkFeature networkFeature;

	public CarDepartureHandler(final NetworkFeature networkFeature) {
		this.networkFeature = networkFeature;
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		SimLink link = this.networkFeature.getSimNetwork().getLinks().get(route.getStartLinkId());
		DriverAgent driver = new NetworkRouteDriver(route);
		SimVehicle simVehicle = link.getParkedVehicle(agent.getPlan().getPerson().getId());// TODO [MR] use vehicleId instead of personId
		simVehicle.setDriver(driver);

		simVehicle.getDriver().notifyMoveToNextLink();
		link.insertVehicle(simVehicle, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_AS_SOON_AS_SPACE_AVAILABLE); // current QSim behavior
	}

}
