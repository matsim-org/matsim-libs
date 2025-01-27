package org.matsim.contrib.emissions.analysis;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class EmissionsByVehicleTypeEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	private final Map<Id<VehicleType>, Object2DoubleMap<Pollutant>> byVehicleType = new HashMap<>();
	private final Map<String, Object2DoubleMap<Pollutant>> byNetworkMode = new HashMap<>();
	private final Network network;
	private final Vehicles vehicles;

	public EmissionsByVehicleTypeEventHandler(Vehicles vehicles, @Nullable Network network) {
		this.vehicles = vehicles;
		this.network = network;
	}

	/**
	 * Collects emissions by vehicle type and pollutant.
	 */
	public Map<Id<VehicleType>, Object2DoubleMap<Pollutant>> getByVehicleType() {
		return byVehicleType;
	}

	public Map<String, Object2DoubleMap<Pollutant>> getByNetworkMode() {
		return byNetworkMode;
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {

		if (network != null && !network.getLinks().containsKey(event.getLinkId())) {
			return;
		}

		Vehicle vehicle = vehicles.getVehicles().get(event.getVehicleId());

		if (vehicle == null) return;

		VehicleType type = vehicle.getType();

		Object2DoubleMap<Pollutant> p = byVehicleType.computeIfAbsent(type.getId(), k -> new Object2DoubleOpenHashMap<>());
		Object2DoubleMap<Pollutant> p2 = byNetworkMode.computeIfAbsent(type.getNetworkMode(), k -> new Object2DoubleOpenHashMap<>());

		for (Map.Entry<Pollutant, Double> e : event.getColdEmissions().entrySet()) {
			p.mergeDouble(e.getKey(), e.getValue(), Double::sum);
			p2.mergeDouble(e.getKey(), e.getValue(), Double::sum);
		}
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {

		if (network != null && !network.getLinks().containsKey(event.getLinkId())) {
			return;
		}

		Vehicle vehicle = vehicles.getVehicles().get(event.getVehicleId());

		if (vehicle == null) return;

		VehicleType type = vehicle.getType();

		Object2DoubleMap<Pollutant> p = byVehicleType.computeIfAbsent(type.getId(), k -> new Object2DoubleOpenHashMap<>());
		Object2DoubleMap<Pollutant> p2 = byNetworkMode.computeIfAbsent(type.getNetworkMode(), k -> new Object2DoubleOpenHashMap<>());

		for (Map.Entry<Pollutant, Double> e : event.getWarmEmissions().entrySet()) {
			p.mergeDouble(e.getKey(), e.getValue(), Double::sum);
			p2.mergeDouble(e.getKey(), e.getValue(), Double::sum);
		}

	}
}
