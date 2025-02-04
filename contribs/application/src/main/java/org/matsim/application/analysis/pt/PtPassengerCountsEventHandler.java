package org.matsim.application.analysis.pt;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;
import java.util.Map;

/**
 * Event handler to count the number of passengers using public transport services.
 *
 * Passenger will be counted once per vehicle type per trip.
 */
public class PtPassengerCountsEventHandler implements PersonEntersVehicleEventHandler, ActivityEndEventHandler {

	/**
	 * Maps vehicle to their pt information.
	 */
	private final Vehicles vehicles;

	/**
	 * Store which passengers have been counted for each vehicle type.
	 */
	private final Map<Id<VehicleType>, IntSet> countedPassengers = new HashMap<>();

	/**
	 * Counts per vehicle type.
	 */
	private final Object2IntMap<Id<VehicleType>> counts = new Object2IntLinkedOpenHashMap<>();

	public PtPassengerCountsEventHandler(Vehicles vehicles) {
		this.vehicles = vehicles;
	}

	/**
	 * Map containing the counts of passengers per vehicle type.
	 */
	public Object2IntMap<Id<VehicleType>> getCounts() {
		return counts;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		Id<Vehicle> vehicleId = event.getVehicleId();
		Vehicle vehicle = vehicles.getVehicles().get(vehicleId);

		if (vehicle == null)
			return;

		VehicleType vehicleType = vehicle.getType();
		Id<VehicleType> id = vehicleType.getId();

		IntSet counted = countedPassengers.computeIfAbsent(id, k -> new IntOpenHashSet());

		// Count passenger only once per vehicle type per trip
		if (!counted.contains(event.getPersonId().index())) {
			counted.add(event.getPersonId().index());
			counts.merge(id, 1, Integer::sum);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// When trip is finished, a passenger can be counted again.
		countedPassengers.values().forEach(p -> p.remove(event.getPersonId().index()));
	}

	@Override
	public void reset(int iteration) {
		countedPassengers.clear();
		counts.clear();
	}
}
