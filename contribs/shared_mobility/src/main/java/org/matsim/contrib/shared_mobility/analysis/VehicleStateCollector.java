package org.matsim.contrib.shared_mobility.analysis;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.service.SharingUtils.SHARING_VEHICLE_STATES;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.contrib.shared_mobility.service.events.SharingReservingEventHandler;
import org.matsim.contrib.shared_mobility.service.events.SharingDropoffEventHandler;
import org.matsim.contrib.shared_mobility.service.events.SharingPickupEventHandler;
import org.matsim.core.controler.listener.IterationStartsListener;

public interface VehicleStateCollector
		extends SharingPickupEventHandler, SharingDropoffEventHandler, SharingReservingEventHandler, IterationStartsListener {
	Map<Id<SharingVehicle>, SHARING_VEHICLE_STATES> getVehicleStatus();
}
