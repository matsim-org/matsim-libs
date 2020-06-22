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

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Geometry;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;

/**
 * This class does not really calculate the expected demand but aims to
 * distribute the fleet vehicles equally over all zones, weighted by zone area size.
 *
 * TODO:test
 *
 * @author tschlenther
 */
public class EqualVehicleDensityZonalDemandAggregator implements ZonalDemandAggregator {

	private final Map<String, MutableInt> vehiclesPerZone = new HashMap<>();
	private static final MutableInt ZERO =  new MutableInt(0);

	public EqualVehicleDensityZonalDemandAggregator(@NotNull DrtZonalSystem zonalSystem, @NotNull FleetSpecification fleetSpecification) {
		this.compute(zonalSystem,fleetSpecification.getVehicleSpecifications().size());
	}

	public ToIntFunction<String> getExpectedDemandForTimeBin(double time) {
		return zoneId-> vehiclesPerZone.getOrDefault(zoneId,ZERO).intValue();
	}

	private void compute(@NotNull DrtZonalSystem zonalSystem, int fleetSize) {
		vehiclesPerZone.clear();

		double areaSum = zonalSystem.getZones().values().stream()
				.mapToDouble(Geometry::getArea)
				.sum();

		for(String zone : zonalSystem.getZones().keySet()){
			double areaShare = zonalSystem.getZone(zone).getArea() / areaSum;
			if(areaShare > 1. || areaShare < 0.) throw new IllegalStateException();
			vehiclesPerZone.put(zone,new MutableInt(Math.floor(areaShare * fleetSize)));
		}
	}
}
