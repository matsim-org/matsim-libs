/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.edrt.optimizer.depot;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.Depots;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * @author michalm
 */
public class NearestChargerAsDepot implements DepotFinder {
	private final ImmutableSet<Link> chargerLinks;

	@Inject
	public NearestChargerAsDepot(ChargingInfrastructure chargingInfrastructure) {
		chargerLinks = chargingInfrastructure.getChargers()
				.values()
				.stream()
				.map(Charger::getLink)
				.collect(ImmutableSet.toImmutableSet());
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	@Override
	public Link findDepot(DvrpVehicle vehicle) {
		return Depots.findStraightLineNearestDepot(vehicle, chargerLinks);
	}
}
