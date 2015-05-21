/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouterWithVehicleRessourcesFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.sharedvehicles;

import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;

/**
 * @author thibautd
 */
public class PlanRouterWithVehicleRessourcesFactory implements PlanRoutingAlgorithmFactory {
	private final PlanRoutingAlgorithmFactory delegate;

	public PlanRouterWithVehicleRessourcesFactory(
			final PlanRoutingAlgorithmFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public PlanAlgorithm createPlanRoutingAlgorithm(final TripRouter tripRouter) {
		return new PlanRouterWithVehicleRessources(
				delegate.createPlanRoutingAlgorithm(
					tripRouter ));
	}
}

