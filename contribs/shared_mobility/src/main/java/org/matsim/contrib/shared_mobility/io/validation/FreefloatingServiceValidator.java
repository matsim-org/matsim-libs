package org.matsim.contrib.shared_mobility.io.validation;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingVehicleSpecification;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;

import com.google.common.base.Verify;

public class FreefloatingServiceValidator implements SharingServiceValidator {
	private final Id<SharingService> serviceId;

	public FreefloatingServiceValidator(Id<SharingService> serviceId) {
		this.serviceId = serviceId;
	}

	@Override
	public void validate(SharingServiceSpecification specification) {
		Set<Id<SharingVehicle>> vehicleIds = new HashSet<>();

		for (SharingVehicleSpecification vehicle : specification.getVehicles()) {
			Verify.verify(!vehicleIds.contains(vehicle.getId()), "Service %s has duplicate vehicle %s",
					serviceId.toString(), vehicle.getId().toString());

			Verify.verify(vehicle.getStartLinkId().isPresent(), "Vehicle %s of service %s needs start link",
					vehicle.getId().toString(), serviceId.toString());
		}
	}
}
