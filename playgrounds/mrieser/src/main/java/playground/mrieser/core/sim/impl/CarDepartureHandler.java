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
import org.matsim.api.core.v01.population.Plan;

import playground.mrieser.core.sim.api.DepartureHandler;
import playground.mrieser.core.sim.features.NetworkFeature;

/**
 * @author mrieser
 */
public class CarDepartureHandler implements DepartureHandler {

	private final NetworkFeature networkFeature;

	public CarDepartureHandler(final NetworkFeature networkFeature) {
		this.networkFeature = networkFeature;
	}

	@Override
	public void handleDeparture(Leg leg, Plan plan) {
		// TODO Auto-generated method stub

	}

}
