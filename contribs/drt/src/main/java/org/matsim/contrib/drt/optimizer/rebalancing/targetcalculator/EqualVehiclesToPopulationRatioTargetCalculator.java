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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;

/**
 * Calculates population size per zone by counting first activites per zone in the selected plans.
 * Returns the share of population of the total population inside the drt service area for the given zone multiplied with the overall fleet size.
 * Should lead lead to rebalancing target values dependent on number of inhabitants (in vehicle units).
 *
 * @author tschlenther
 */
public final class EqualVehiclesToPopulationRatioTargetCalculator implements RebalancingTargetCalculator {

	private static final Logger log = Logger.getLogger(EqualVehiclesToPopulationRatioTargetCalculator.class);

	private final DrtZonalSystem zonalSystem;
	private final FleetSpecification fleetSpecification;
	private final Map<DrtZone, Integer> activitiesPerZone;
	private final int totalNrActivities;

	public EqualVehiclesToPopulationRatioTargetCalculator(DrtZonalSystem zonalSystem, Population population,
			@NotNull FleetSpecification fleetSpecification) {
		this.zonalSystem = zonalSystem;
		this.fleetSpecification = fleetSpecification;

		log.info("nr of zones: " + this.zonalSystem.getZones().size() + "\t nr of persons = " + population.getPersons()
				.size());
		activitiesPerZone = countFirstActsPerZone(population);

		totalNrActivities = this.activitiesPerZone.values().stream().mapToInt(Integer::intValue).sum();
		log.info("nr of persons that have their first activity inside the service area = " + this.totalNrActivities);
	}

	private Map<DrtZone, Integer> countFirstActsPerZone(Population population) {
		return population.getPersons()
				.values()
				.stream()
				.map(person -> (Activity)person.getSelectedPlan().getPlanElements().get(0))
				.map(activity -> zonalSystem.getZoneForLinkId(activity.getLinkId()))
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(zone -> zone, collectingAndThen(counting(), Long::intValue)));
	}

	@Override
	public ToIntFunction<DrtZone> calculate(double time, Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		//decided to take Math.floor rather than Math.round as we want to avoid global undersupply which would 'paralyze' the rebalancing algorithm
		int fleetSize = this.fleetSpecification.getVehicleSpecifications().size();
		return zoneId -> (int)Math.floor(
				(this.activitiesPerZone.getOrDefault(zoneId, 0).doubleValue() / totalNrActivities) * fleetSize);
	}
}
