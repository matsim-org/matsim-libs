package org.matsim.contrib.sharing.io;

import java.util.Collection;

public interface SharingServiceSpecification {
	Collection<SharingVehicleSpecification> getVehicles();

	Collection<SharingStationSpecification> getStations();

	void addVehicle(SharingVehicleSpecification val);

	void addStation(SharingStationSpecification val);
}
