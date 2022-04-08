package org.matsim.contrib.shared_mobility.io;

import java.util.Collection;

public interface SharingServiceSpecification {
	Collection<SharingVehicleSpecification> getVehicles();

	Collection<SharingStationSpecification> getStations();

	void addVehicle(SharingVehicleSpecification val);

	void addStation(SharingStationSpecification val);
}
