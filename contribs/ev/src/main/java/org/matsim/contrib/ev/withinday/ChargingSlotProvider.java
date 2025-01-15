package org.matsim.contrib.ev.withinday;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * A charging slot provider is called in the beginning of the day. It is
 * supposed to return all the planned charging slots for an agent. Those can be
 * leg-based charging slots (where an agent stops and charges along a leg) or
 * activity-based charging slots where the agent plugs the vehicle before going
 * to an activity, and comes back after that or another acitvity to unplug the
 * vehicle.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargingSlotProvider {
	List<ChargingSlot> findSlots(Person person, Plan plan, ElectricVehicle vehicle);

	static public final ChargingSlotProvider NOOP = (person, plan, vehicle) -> Collections.emptyList();
}
