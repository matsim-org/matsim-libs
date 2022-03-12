package org.matsim.contrib.shared_mobility.io;

import java.util.Collection;
import java.util.LinkedList;

public class DefaultSharingServiceSpecification implements SharingServiceSpecification {
	private final Collection<SharingVehicleSpecification> vehicles = new LinkedList<>();
	private final Collection<SharingStationSpecification> stations = new LinkedList<>();

	@Override
	public Collection<SharingVehicleSpecification> getVehicles() {
		return vehicles;
	}

	@Override
	public Collection<SharingStationSpecification> getStations() {
		return stations;
	}

	@Override
	public void addVehicle(SharingVehicleSpecification val) {
		vehicles.add(val);
	}

	@Override
	public void addStation(SharingStationSpecification val) {
		stations.add(val);
	}
}
