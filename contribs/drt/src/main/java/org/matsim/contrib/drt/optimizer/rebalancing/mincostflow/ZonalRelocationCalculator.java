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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;
import java.util.Map;

/**
 * @author michalm
 */
public interface ZonalRelocationCalculator {
	/**
	 * @param vehicleSurplus              could be negative (supply - demand), typically contains only non-zero values (zones with zero surplus are skipped)
	 * @param rebalancableVehiclesPerZone list of rebalancable vehicles per each zone (zones without rebalancable vehicles are usually skipped)
	 * @return vehicle relocations
	 */
	List<Relocation> calcRelocations(List<DrtZoneVehicleSurplus> vehicleSurplus,
			Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone);
}
