/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator;

import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;

/**
 * Calculates population size per zone by counting first activites per zone in the selected plans.
 * Returns the share of population of the total population inside the drt service area for the given zone multiplied with the overall fleet size.
 * Should lead lead to rebalancing target values dependent on number of inhabitants (in vehicle units).
 *
 * @author tschlenther
 */
public final class EqualVehiclesToPopulationRatioTargetCalculator implements RebalancingTargetCalculator {

	private static final Logger log = LogManager.getLogger(EqualVehiclesToPopulationRatioTargetCalculator.class);

	private final int fleetSize;
	private final Map<Zone, Integer> activitiesPerZone;
	private final int totalNrActivities;

	public EqualVehiclesToPopulationRatioTargetCalculator(ZoneSystem zonalSystem, Population population,
														  @NotNull FleetSpecification fleetSpecification) {
		log.debug("nr of zones: " + zonalSystem.getZones().size() + "\t nr of persons = " + population.getPersons()
				.size());
		fleetSize = fleetSpecification.getVehicleSpecifications().size();
		activitiesPerZone = countFirstActsPerZone(zonalSystem, population);
		totalNrActivities = this.activitiesPerZone.values().stream().mapToInt(Integer::intValue).sum();
		log.debug("nr of persons that have their first activity inside the service area = " + this.totalNrActivities);
	}

	private Map<Zone, Integer> countFirstActsPerZone(ZoneSystem zonalSystem, Population population) {
		return population.getPersons()
				.values()
				.stream()
				.map(person -> (Activity)person.getSelectedPlan().getPlanElements().get(0))
				.map(activity -> zonalSystem.getZoneForLinkId(activity.getLinkId()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.groupingBy(zone -> zone, collectingAndThen(counting(), Long::intValue)));
	}

	@Override
	public ToDoubleFunction<Zone> calculate(double time,
			Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		if (totalNrActivities == 0) {
			return zoneId -> 0;
		}

		double factor = (double)fleetSize / totalNrActivities;
		return zoneId -> this.activitiesPerZone.getOrDefault(zoneId, 0).doubleValue() * factor;
	}
}
