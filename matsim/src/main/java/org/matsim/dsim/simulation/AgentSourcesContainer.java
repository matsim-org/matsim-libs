package org.matsim.dsim.simulation;

import org.matsim.api.core.v01.Message;
import org.matsim.core.mobsim.dsim.DistributedAgentSource;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.VehicleContainer;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentSourcesContainer {

	private final Map<Class<? extends DistributedMobsimAgent>, List<DistributedAgentSource>> agentTypes = new HashMap<>();
	private final Map<Class<? extends DistributedMobsimVehicle>, List<DistributedAgentSource>> vehicleTypes = new HashMap<>();

	private final List<DistributedAgentSource> agentSources = new ArrayList<>();

	public List<DistributedAgentSource> getAgentSources() {
		return agentSources;
	}

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
	@SuppressWarnings("rawtypes")
	public DistributedMobsimVehicle vehicleFromContainer(VehicleContainer container) {

		DistributedMobsimVehicle vehicle = vehicleFromMessage(container.vehicleType(), container.vehicle());
		SourceAgent sa = agentAndSourceFromMessage(container.driver().type(), container.driver().occupant());

		DriverAgent driver = (DriverAgent) sa.agent;
		vehicle.setDriver(driver);
		driver.setVehicle(vehicle);

		for (VehicleContainer.Occupant occ : container.passengers()) {
			PassengerAgent p = (PassengerAgent) agentFromMessage(occ.type(), occ.occupant());
			vehicle.addPassenger(p);
			p.setVehicle(vehicle);
		}

		sa.source.onDriverCreated(driver, vehicle);

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
	public <T extends DistributedMobsimAgent> T agentFromMessage(Class<T> type, Message m) {
		return agentAndSourceFromMessage(type, m).agent;
	}

	@SuppressWarnings("unchecked")
	private <T extends DistributedMobsimAgent> SourceAgent<T> agentAndSourceFromMessage(Class<T> type, Message m) {
		List<DistributedAgentSource> sources = agentTypes.get(type);
		if (sources == null) {
			throw new RuntimeException("No agent provider found for type %s".formatted(type));
		}

		for (DistributedAgentSource source : sources) {
			T agent = (T) source.agentFromMessage(type, m);
			if (agent != null) {
				return new SourceAgent<>(source, agent);
			}
		}

		throw new IllegalStateException("No agent provider found for type %s with message %s".formatted(type, m));
	}

	private record SourceAgent<T extends DistributedMobsimAgent>(DistributedAgentSource source, T agent) {
	}
}
