/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the in-vehicle-cost depending on the occupancy per route section.
 * It supports increasing costs for high-occupancy trips, and decreasing costs for low-occupancy trips.
 *
 * The costs are modified by a occupancy-dependent factor.
 *
 * <ul>
 * 	<li>Between a <em>lower limit</em> and an <em>upper limit</em>, this factor is 1.0.</li>
 *  <li>The cost factor at 0% occupancy is defined by a <em>minimum factor</em>, and then increases linearly up to a factor of 1.0 at the lower limit.</li>
 *  <li>beginning at the upper limit with a factor of 1.0, the cost factor increases linearly up to a <em>maximum Factor</em> at 100% occupany.</li>
 * </ul>
 *
 * <pre>
 *             |
 *       maxF  |-- - - - - - - - - - - - - - - -  __---+-
 *             |                              __--     |
 *             |                          __--         |
 *        1.0  |-- - - ___-+-------------+             |
 *             | ___---    |             |             |
 *       minF  |-          |             |             |
 *             |           |             |             |
 *             +-----------+-------------+-------------+---
 *             0%         ll%           ul%         100%
 *
 *    minF: minimum cost factor at 0% occupancy
 *    maxF: maximum cost factor at 100% occupancy
 *    ll:   lower limit of occupancy where the cost factor first reaches 1.0
 *    ul:   upper limit of occupancy where the cost factor last is 1.0
 * </pre>
 *
 * @author mrieser / Simunto GmbH
 */
public class CapacityDependentInVehicleCostCalculator implements RaptorInVehicleCostCalculator {

	double minimumCostFactor = 0.4;
	double lowerCapacityLimit = 0.3;
	double higherCapacityLimit = 0.6;
	double maximumCostFactor = 1.8;

	public CapacityDependentInVehicleCostCalculator() {
	}

	public CapacityDependentInVehicleCostCalculator(double minimumCostFactor, double lowerCapacityLimit, double higherCapacityLimit, double maximumCostFactor) {
		this.minimumCostFactor = minimumCostFactor;
		this.lowerCapacityLimit = lowerCapacityLimit;
		this.higherCapacityLimit = higherCapacityLimit;
		this.maximumCostFactor = maximumCostFactor;
	}

	@Override
	public double getInVehicleCost(double inVehicleTime, double marginalUtility_utl_s, Person person, Vehicle vehicle, RaptorParameters paramters, RouteSegmentIterator iterator) {
		double costSum = 0;
		double seatCount = vehicle.getType().getCapacity().getSeats();
		double standingRoom = vehicle.getType().getCapacity().getStandingRoom();

		boolean considerSeats = standingRoom * 2 < seatCount; // at least 2/3 of capacity are seats, so passengers could expect a seat

		double relevantCapacity = considerSeats ? seatCount : (seatCount + standingRoom);

		while (iterator.hasNext()) {
			iterator.next();
			double inVehTime = iterator.getInVehicleTime();
			double paxCount = iterator.getPassengerCount();
			double occupancy = paxCount / relevantCapacity;
			double baseCost = inVehTime * -marginalUtility_utl_s;
			double factor = 1.0;

			if (occupancy < this.lowerCapacityLimit) {
				factor = this.minimumCostFactor + (1.0 - this.minimumCostFactor) / this.lowerCapacityLimit * occupancy;
			}
			if (occupancy > this.higherCapacityLimit) {
				factor = 1.0 + (this.maximumCostFactor - 1.0) / (1.0 - this.higherCapacityLimit) * (occupancy - this.higherCapacityLimit);
			}

			costSum += baseCost * factor;
		}
		return costSum;
	}
}
