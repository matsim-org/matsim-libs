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

import java.util.function.Function;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.charging.ChargingLogic;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingInfrastructures {
	public static ChargingInfrastructure createChargingInfrastructure(
			ChargingInfrastructureSpecification infrastructureSpecification, Function<Id<Link>, Link> linkProvider,
			ChargingLogic.Factory chargingLogicFactory) {
		var chargers = infrastructureSpecification.getChargerSpecifications()
				.values()
				.stream()
				.map(s -> ChargerImpl.create(s, linkProvider.apply(s.getLinkId()), chargingLogicFactory))
				.collect(ImmutableMap.toImmutableMap(Charger::getId, c -> c));
		return () -> chargers;
	}

	public static ImmutableListMultimap<Id<Link>, Charger> getChargersAtLinks(ChargingInfrastructure infrastructure) {
		return infrastructure.getChargers()
				.values()
				.stream()
				.collect(ImmutableListMultimap.toImmutableListMultimap(c -> c.getLink().getId(), c -> c));
	}

	public static ChargingInfrastructure filterChargers(ChargingInfrastructure infrastructure,
			Predicate<Charger> filter) {
		var filteredChargers = infrastructure.getChargers()
				.values()
				.stream()
				.filter(filter)
				.collect(ImmutableMap.toImmutableMap(Charger::getId, c -> c));
		return () -> filteredChargers;
	}
}
