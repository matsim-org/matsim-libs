package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the in-vehicle-cost depending on the occupancy per route section.
 * It supports different cost-factors for low and high occupany, assuming a cost factor of 1.0 for medium occupancy.
 *
 * @author mrieser / Simunto GmbH
 */
public class CapacityDependentInVehicleCostCalculator implements RaptorInVehicleCostCalculator {

	double lowCapacityCostFactor = 0.9;
	double lowCapacityLimit = 0.3;
	double highCapacityCostFactor = 1.1;
	double highCapacityLimit = 0.7;

	public CapacityDependentInVehicleCostCalculator() {
	}

	public CapacityDependentInVehicleCostCalculator(double lowCapacityCostFactor, double lowCapacityLimit, double highCapacityCostFactor, double highCapacityLimit) {
		this.lowCapacityCostFactor = lowCapacityCostFactor;
		this.lowCapacityLimit = lowCapacityLimit;
		this.highCapacityCostFactor = highCapacityCostFactor;
		this.highCapacityLimit = highCapacityLimit;
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
			double factor = 1;

			if (occupancy < lowCapacityLimit) {
				factor = lowCapacityCostFactor;
			}
			if (occupancy > highCapacityLimit) {
				factor = highCapacityCostFactor;
			}

			costSum += baseCost * factor;
		}
		return costSum;
	}
}
