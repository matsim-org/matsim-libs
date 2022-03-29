package org.matsim.contrib.shared_mobility.io.validation;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingStationSpecification;
import org.matsim.contrib.shared_mobility.io.SharingVehicleSpecification;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingStation;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;

import com.google.common.base.Verify;

public class StationBasedServiceValidator implements SharingServiceValidator {
	private final Id<SharingService> serviceId;

	public StationBasedServiceValidator(Id<SharingService> serviceId) {
		this.serviceId = serviceId;
	}

	@Override
	public void validate(SharingServiceSpecification specification) {
		Set<Id<SharingStation>> stationIds = new HashSet<>();

		for (SharingStationSpecification station : specification.getStations()) {
			Verify.verify(!stationIds.contains(station.getId()), "Service %s has duplicate station %s",
					serviceId.toString(), station.getId().toString());

			stationIds.add(station.getId());
		}

		Set<Id<SharingVehicle>> vehicleIds = new HashSet<>();

		for (SharingVehicleSpecification vehicle : specification.getVehicles()) {
			Verify.verify(!vehicleIds.contains(vehicle.getId()), "Service %s has duplicate vehicle %s",
					serviceId.toString(), vehicle.getId().toString());

			Verify.verify(vehicle.getStartStationId().isPresent(), "Vehicle %s of service %s needs start link",
					vehicle.getId().toString(), serviceId.toString());

			Verify.verify(stationIds.contains(vehicle.getStartStationId().get()),
					"Station %s for vehicle %s does not exist in service %s",
					vehicle.getStartStationId().get().toString(), vehicle.getId().toString(), serviceId.toString());

			vehicleIds.add(vehicle.getId());
		}
	}
}
