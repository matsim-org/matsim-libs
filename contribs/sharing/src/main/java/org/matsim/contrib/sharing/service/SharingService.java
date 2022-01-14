package org.matsim.contrib.sharing.service;

import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.sharing.routing.InteractionPoint;
import org.matsim.core.mobsim.framework.MobsimAgent;

public interface SharingService {
	Id<SharingService> getId();

	IdMap<SharingVehicle, SharingVehicle> getVehicles();

	IdMap<SharingStation, SharingStation> getStations();

	void pickupVehicle(SharingVehicle vehicle, MobsimAgent agent);

	void reserveVehicle(MobsimAgent agent,SharingVehicle vehicle);

	void dropoffVehicle(SharingVehicle vehicle, MobsimAgent agent);

	Optional<VehicleInteractionPoint> findClosestVehicle(MobsimAgent agent);

	InteractionPoint findClosestDropoffLocation(SharingVehicle vehicle, MobsimAgent agent);

	SharingVehicle hasReservationElseNull(MobsimAgent agent);

	void releaseReservation(MobsimAgent agent);
}
