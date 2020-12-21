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

import java.util.Objects;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.Depots;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * @author michalm
 */
public class NearestChargerAsDepot implements DepotFinder {
	static final Logger log = Logger.getLogger(NearestChargerAsDepot.class);

	private final ImmutableSet<Link> chargerLinks;

	@Inject
	public NearestChargerAsDepot(ChargingInfrastructureSpecification chargingInfrastructure, Network modalNetwork,
			String mode) {
		chargerLinks = chargingInfrastructure.getChargerSpecifications()
				.values()
				.stream()
				.map(ChargerSpecification::getLinkId)
				.map(modalNetwork.getLinks()::get)
				.filter(Objects::nonNull)
				.collect(ImmutableSet.toImmutableSet());

		int chargerCount = chargingInfrastructure.getChargerSpecifications().size();
		int unreachableChargerCount = chargerCount - chargerLinks.size();
		if (unreachableChargerCount > 0) {
			log.warn(unreachableChargerCount
					+ "out of "
					+ chargerCount
					+ "chargers (depots) are not reachable for mode: "
					+ mode);
		}
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	@Override
	public Link findDepot(DvrpVehicle vehicle) {
		return Depots.findStraightLineNearestDepot(vehicle, chargerLinks);
	}
}
