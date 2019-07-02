/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.infrastructure;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.util.LinkProvider;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingInfrastructures {
	public static ChargingInfrastructure createChargingInfrastructure(
			ChargingInfrastructureSpecification infrastructureSpecification, LinkProvider<Id<Link>> linkProvider,
			ChargingLogic.Factory chargingLogicFactory) {
		ImmutableMap<Id<Charger>, Charger> chargers = infrastructureSpecification.getChargerSpecifications()
				.values()
				.stream()
				.map(s -> ChargerImpl.create(s, linkProvider.apply(s.getLinkId()), chargingLogicFactory))
				.collect(ImmutableMap.toImmutableMap(Charger::getId, ch -> ch));
		return () -> chargers;
	}

	public static ImmutableListMultimap<Id<Link>, Charger> getChargersAtLinks(ChargingInfrastructure infrastructure) {
		return infrastructure.getChargers()
				.values()
				.stream().collect(ImmutableListMultimap.toImmutableListMultimap(c -> c.getLink().getId(), c -> c));
	}
}
