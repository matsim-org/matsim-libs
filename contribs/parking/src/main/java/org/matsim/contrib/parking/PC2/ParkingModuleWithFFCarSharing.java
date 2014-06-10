package org.matsim.contrib.parking.PC2;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingLinkInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.controler.Controler;

public class ParkingModuleWithFFCarSharing extends ParkingModule implements ParkingModuleWithFreeFloatingCarSharing {

	public ParkingModuleWithFFCarSharing(Controler controler,Collection<ParkingCoordInfo> initialDesiredVehicleCoordinates) {
		super(controler);
		//TODO: initialize parkings car to parking
	}

	@Override
	public ParkingLinkInfo getNextFreeFloatingVehicle(Coord coord) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ParkingLinkInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetForNewIterationStart() {
		// TODO Auto-generated method stub
		
	}

}
