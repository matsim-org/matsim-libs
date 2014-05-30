package org.matsim.contrib.parking.parkingChoice.carsharing;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

public class DummyParkingModuleWithFreeFloatingCarSharing implements ParkingModuleWithFreeFloatingCarSharing {

	private Controler controler;
	private Collection<Id> allVehicles;
	private LinkedList<Id> availableVehicles;

	public DummyParkingModuleWithFreeFloatingCarSharing(Controler controler, Collection<Id> vehicles){
		this.controler=controler;
		this.allVehicles = vehicles;
		availableVehicles=new LinkedList<Id>(vehicles);
	}
	
	@Override
	public ParkingInfo getNextFreeFloatingVehicle(Coord coord) {
		Id vehicleId=null;
		if (availableVehicles.size()>0){
			vehicleId= availableVehicles.poll();
		} else {
			DebugLib.stopSystemAndReportInconsistency("no vehicle available");
		}
		
		return new ParkingInfo(vehicleId, coord);
	}

	@Override
	public ParkingInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord) {
		availableVehicles.add(vehicleId);
		return new ParkingInfo(vehicleId, destCoord);
	}

}
