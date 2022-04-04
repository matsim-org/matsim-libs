package org.matsim.contrib.shared_mobility.analysis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.service.SharingUtils.SHARING_VEHICLE_STATES;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.contrib.shared_mobility.service.events.SharingReservingEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingDropoffEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingPickupEvent;
import org.matsim.core.controler.events.IterationStartsEvent;

public class VehicleStateCollectorImpl implements VehicleStateCollector {

	SharingServiceSpecification sharingServiceSpecification;

	private final IdMap<SharingVehicle, SHARING_VEHICLE_STATES> vehicle2VehicleStatus = new IdMap<>(
			SharingVehicle.class);
	private final Set<Id<SharingVehicle>> sharingVehicles = new HashSet<Id<SharingVehicle>>();

	public VehicleStateCollectorImpl(SharingServiceSpecification sharingServiceSpecification) {
		this.sharingServiceSpecification = sharingServiceSpecification;
	}

	@Override
	public void handleEvent(SharingPickupEvent event) {
		if (this.sharingVehicles.contains(event.getSharingVehicleId())) {
			vehicle2VehicleStatus.put(event.getSharingVehicleId(), SHARING_VEHICLE_STATES.BOOKED);
		}
	}

	@Override
	public void handleEvent(SharingDropoffEvent event) {
		if (this.sharingVehicles.contains(event.getSharingVehicleId())) {
			vehicle2VehicleStatus.put(event.getSharingVehicleId(), SHARING_VEHICLE_STATES.IDLE);
		}
	}

	@Override
	public void handleEvent(SharingReservingEvent event) {
		if (this.sharingVehicles.contains(event.getSharingVehicleId())) {
			vehicle2VehicleStatus.put(event.getSharingVehicleId(), SHARING_VEHICLE_STATES.RESERVED);
		}
	}

	@Override
	public Map<Id<SharingVehicle>, SHARING_VEHICLE_STATES> getVehicleStatus() {
		return vehicle2VehicleStatus;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		vehicle2VehicleStatus.clear();

		// Initialize with idle state
		sharingServiceSpecification.getVehicles().forEach(v -> {
			vehicle2VehicleStatus.put(v.getId(), SHARING_VEHICLE_STATES.IDLE);
			sharingVehicles.add(v.getId());
		});
	}
}
