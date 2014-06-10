package org.matsim.contrib.parking.PC2;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingLinkInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;

public class ParkingModuleWithFFCarSharing extends GeneralParkingModule implements ParkingModuleWithFreeFloatingCarSharing {

	private LinkedList<Id> availableVehicles;
	
	public ParkingModuleWithFFCarSharing(Controler controler,Collection<ParkingCoordInfo> initialDesiredVehicleCoordinates) {
		super(controler);
		//TODO: initialize parkings car to parking
		
		availableVehicles = new LinkedList<Id>();

		for (ParkingCoordInfo parkInfo : initialDesiredVehicleCoordinates) {
			availableVehicles.add(parkInfo.getVehicleId());
		}
		
	}

	@Override
	public ParkingLinkInfo getNextFreeFloatingVehicle(Coord coord) {
		Id vehicleId = null;
		if (availableVehicles.size() > 0) {
			vehicleId = availableVehicles.poll();
		} else {
			DebugLib.stopSystemAndReportInconsistency("no vehicle available");
		}

		NetworkImpl network = (NetworkImpl) controler.getNetwork();

		return new ParkingLinkInfo(vehicleId, network.getNearestLink(coord)
				.getId());
	}

	@Override
	public ParkingLinkInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord) {
		availableVehicles.add(vehicleId);
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		return new ParkingLinkInfo(vehicleId, network.getNearestLink(destCoord)
				.getId());
	}

	@Override
	public void resetForNewIterationStart() {
		// TODO Auto-generated method stub
		
	}

}
