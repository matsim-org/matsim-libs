package org.matsim.dsim.simulation;

import lombok.Getter;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentSourcesContainer {

	private final Map<Class<? extends DistributedMobsimAgent>, List<DistributedAgentSource>> agentTypes = new HashMap<>();
	private final Map<Class<? extends DistributedMobsimVehicle>, List<DistributedAgentSource>> vehicleTypes = new HashMap<>();

	@Getter
	private final List<DistributedAgentSource> agentSources = new ArrayList<>();

	/**
	 * Add source to this container.
	 */
	public void addSource(DistributedAgentSource as) {
		agentSources.add(as);
		for (Class<? extends DistributedMobsimAgent> agentClass : as.getAgentClasses()) {
			agentTypes.computeIfAbsent(agentClass, (_) -> new ArrayList<>()).add(as);
		}

		for (Class<? extends DistributedMobsimVehicle> vehicleClass : as.getVehicleClasses()) {
			vehicleTypes.computeIfAbsent(vehicleClass, (_) -> new ArrayList<>()).add(as);
		}
	}

	/**
	 * Create a vehicle container, which includes all the occupants.
	 */
	public static VehicleContainer vehicleToContainer(DistributedMobsimVehicle vehicle) {
		var passengers = vehicle.getPassengers().stream()
			.map(p -> new VehicleContainer.Occupant((DistributedMobsimAgent) p))
			.toList();
		return new VehicleContainer(
			vehicle.getClass(),
			vehicle.toMessage(),
			new VehicleContainer.Occupant((DistributedMobsimAgent) vehicle.getDriver()),
			passengers
		);
	}

	/**
	 * Create a vehicle and its occupants from received container.
	 */
	public DistributedMobsimVehicle vehicleFromContainer(VehicleContainer container) {

		DistributedMobsimVehicle vehicle = vehicleFromMessage(container.vehicleType(), container.vehicle());
		DriverAgent driver = (DriverAgent) agentFromMessage(container.driver().type(), container.driver().occupant());
		vehicle.setDriver(driver);
		driver.setVehicle(vehicle);

		for (VehicleContainer.Occupant occ : container.passengers()) {
			PassengerAgent p = (PassengerAgent) agentFromMessage(occ.type(), occ.occupant());
			vehicle.addPassenger(p);
			p.setVehicle(vehicle);
		}

		return vehicle;
	}

	/**
	 * Create a vehicle from a received message. This should normally not be used directly.
	 *
	 * @see #vehicleFromContainer(VehicleContainer)
	 */
	private DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message m) {
		List<DistributedAgentSource> sources = vehicleTypes.get(type);
		if (sources == null) {
			throw new RuntimeException("No vehicle provider found for %s".formatted(type));
		}

		for (DistributedAgentSource source : sources) {
			DistributedMobsimVehicle vehicle = source.vehicleFromMessage(type, m);
			if (vehicle != null) {
				return vehicle;
			}
		}

		throw new IllegalStateException("No vehicle provider found for type %s with message %s".formatted(type, m));
	}

	/**
	 * Create an agent from a received message.
	 */
	@SuppressWarnings("unchecked")
	public <T extends DistributedMobsimAgent> T agentFromMessage(Class<T> type, Message m) {
		List<DistributedAgentSource> sources = agentTypes.get(type);
		if (sources == null) {
			throw new RuntimeException("No agent provider found for type %s".formatted(type));
		}

		for (DistributedAgentSource source : sources) {
			T agent = (T) source.agentFromMessage(type, m);
			if (agent != null) {
				return agent;
			}
		}

		throw new IllegalStateException("No agent provider found for type %s with message %s".formatted(type, m));
	}
}
