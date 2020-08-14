/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.drt.analysis.zonal;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Calculates population size per zone by counting first activites per zone in the selected plans.
 * Returns the share of population of the total population inside the drt service area for the given zone multiplied with the overall fleet size.
 * Should lead lead to rebalancing target values dependent on number of inhabitants (in vehicle units).
 *
 * @author tschlenther
 */
public final class FleetSizeWeightedByPopulationShareDemandAggregator implements ZonalDemandAggregator {

	private static Logger log = Logger.getLogger(FleetSizeWeightedByPopulationShareDemandAggregator.class);

	private final DrtZonalSystem zonalSystem;
	private Map<String, Integer> activitiesPerZone = new HashMap<>();
	private final int fleetSize;
	private Integer totalNrActivities;

	public FleetSizeWeightedByPopulationShareDemandAggregator(DrtZonalSystem zonalSystem, Population population, @NotNull FleetSpecification fleetSpecification) {
		this.zonalSystem = zonalSystem;
		prepareZones();
		countFirstActsPerZone(population);
		this.fleetSize = fleetSpecification.getVehicleSpecifications().size();
	}

	private void countFirstActsPerZone(Population population) {
		log.info("start counting how many first activities each rebalancing zone has");
		log.info("nr of zones: " + this.zonalSystem.getZones().size() + "\t nr of persons = " + population.getPersons().size());

		population.getPersons().values().stream()
				.map(person -> person.getSelectedPlan().getPlanElements().get(0))
				.forEach(element -> {
					if (! (element instanceof Activity) ) throw new RuntimeException("first plan element is not an activity");
					Activity activity = (Activity) element;
					String zone = zonalSystem.getZoneForLinkId(activity.getLinkId());
					if (zone != null){
						Integer oldDemandValue = this.activitiesPerZone.get(zone);
						this.activitiesPerZone.put(zone, oldDemandValue + 1);
					}
				});

		this.totalNrActivities = this.activitiesPerZone.values().stream().collect(Collectors.summingInt(Integer::intValue));
		log.info("nr of persons that have their first activity inside the service area = " + this.totalNrActivities);
	}

	public ToIntFunction<String> getExpectedDemandForTimeBin(double time) {
		//decided to take Math.floor rather than Math.round as we want to avoid global undersupply which would 'paralyze' the rebalancing algorithm
		return zoneId ->  (int) Math.floor( ( this.activitiesPerZone.getOrDefault(zoneId, 0).doubleValue() / totalNrActivities ) * fleetSize);
	}

	private void prepareZones() {
		for (String zone : zonalSystem.getZones().keySet()) {
			activitiesPerZone.put(zone, 0);
		}
	}

}
