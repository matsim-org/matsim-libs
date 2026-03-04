package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Route;
import org.matsim.vehicles.Vehicle;

/**
 * BackpackRoutes collect events from the simulation to re-create a route which the person actually experienced during the simulation. A new
 * BackpackRoute is created for each leg an agent starts. It is expected that one {@link BackpackRouteProvider} per mode is bound. See
 * {@link BackpackRouteProvider} on how to add custom BackpackRoutes to the simulation.
 * <p>
 * A BackpackRoute receives all events for a single person. When the person has finished its leg, the finishRoute method is called.
 */
public interface BackpackRoute {

	/**
	 * Will receive all events generated during the mobsim for one person.
	 */
	void handleEvent(Event e);

	/**
	 * Will be called when a person finishes a leg and is expected to return a suitable {@link Route} for the resulting experienced plan.
	 *
	 * @return the finished route of the leg
	 */
	Route finishRoute();

	/**
	 * When a person switches network partitions, the corresponding backpack plan, including its current state is transferred as well.
	 * A BackpackRoute must provide a suitable representation of its internal state, which can be used to reconstruct it on the new partition.
	 *
	 * @return a compact representation of the BackpackRoute's state.
	 */
	Message toMessage();

	/**
	 * The vehicle id, the person currently travels in. This is necessary to dispatch {@link org.matsim.api.core.v01.events.LinkEnterEvent} and
	 * {@link org.matsim.api.core.v01.events.LinkLeaveEvent} to the correct Backpack. {@code null} values indicates that the person is currently
	 * not inside a vehicle. (A person may never enter a vehicle, for example, during a teleported leg)
	 */
	Id<Vehicle> getVehicleId();
}
