package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;

import java.util.Collection;
import java.util.LinkedList;

public class DummyParkingModuleWithFreeFloatingCarSharing implements
		ParkingModuleWithFreeFloatingCarSharing {

	private MatsimServices controler;
	private LinkedList<Id> availableVehicles;

	public DummyParkingModuleWithFreeFloatingCarSharing(MatsimServices controler,
			Collection<ParkingCoordInfo> initialDesiredVehicleCoordinates) {
		this.controler = controler;

		availableVehicles = new LinkedList<Id>();

		for (ParkingCoordInfo parkInfo : initialDesiredVehicleCoordinates) {
			availableVehicles.add(parkInfo.getVehicleId());
		}
		// the vehicle will be parked at the closest free parking from desired
		// coordinate.
	}

	@Override
	public ParkingLinkInfo getNextFreeFloatingVehicle(Coord coord, Id personId, double time) {
		Id vehicleId = null;
		if (availableVehicles.size() > 0) {
			vehicleId = availableVehicles.poll();
		} else {
			DebugLib.stopSystemAndReportInconsistency("no vehicle available");
		}

        NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();

		return new ParkingLinkInfo(vehicleId, NetworkUtils.getNearestLink(network, coord)
				.getId());
	}

	@Override
	public ParkingLinkInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord, Id personI, double time) {
		availableVehicles.add(vehicleId);
        NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
		return new ParkingLinkInfo(vehicleId, NetworkUtils.getNearestLink(network, destCoord)
				.getId());
	}

	@Override
	public void resetForNewIterationStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void makeFFVehicleAvailable(Id vehicleId, PC2Parking parking) {
		// TODO Auto-generated method stub
		
	}

}
