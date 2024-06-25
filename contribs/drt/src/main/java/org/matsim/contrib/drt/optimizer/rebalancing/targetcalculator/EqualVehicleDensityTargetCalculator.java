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

import com.google.common.base.Preconditions;
import jakarta.validation.constraints.NotNull;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * This class does not really calculate the expected demand but aims to
 * distribute the fleet vehicles equally over all zones, weighted by zone area size.
 * <p>
 *
 * @author tschlenther
 */
public final class EqualVehicleDensityTargetCalculator implements RebalancingTargetCalculator {

	private final Map<Id<Zone>, Double> zoneAreaShares = new IdMap<>(Zone.class);
	private final FleetSpecification fleetSpecification;

	public EqualVehicleDensityTargetCalculator(@NotNull ZoneSystem zonalSystem,
			@NotNull FleetSpecification fleetSpecification) {
		initAreaShareMap(zonalSystem);
		this.fleetSpecification = fleetSpecification;
	}

	@Override
	public ToDoubleFunction<Zone> calculate(double time,
			Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		return zone -> zoneAreaShares.getOrDefault(zone.getId(), 0.) * fleetSpecification.getVehicleSpecifications().size();
	}

	private void initAreaShareMap(ZoneSystem zonalSystem) {
		double areaSum = zonalSystem.getZones()
				.values()
				.stream()
				.mapToDouble(z -> z.getPreparedGeometry().getGeometry().getArea())
				.sum();

		for (Zone zone : zonalSystem.getZones().values()) {
			double areaShare = zone.getPreparedGeometry().getGeometry().getArea() / areaSum;
			Preconditions.checkState(areaShare >= 0. && areaShare <= 1.);
			zoneAreaShares.put(zone.getId(), areaShare);
		}
	}
}
