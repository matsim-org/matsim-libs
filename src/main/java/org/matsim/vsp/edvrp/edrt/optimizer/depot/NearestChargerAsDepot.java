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

package org.matsim.vsp.edvrp.edrt.optimizer.depot;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.Depots;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ChargingInfrastructure;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class NearestChargerAsDepot implements DepotFinder {
	private final Set<Link> chargerLinks = new HashSet<>();

	@Inject
	public NearestChargerAsDepot(ChargingInfrastructure chargingInfrastructure) {
		for (Charger c : chargingInfrastructure.getChargers().values()) {
			chargerLinks.add(c.getLink());
		}
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	@Override
	public Link findDepot(Vehicle vehicle) {
		return Depots.findStraightLineNearestDepot(vehicle, chargerLinks);
	}
}
