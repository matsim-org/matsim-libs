package playground.wrashid.parkingChoice.freeFloatingCarSharing;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.GeneralParkingModule;
import org.matsim.contrib.parking.PC2.infrastructure.Parking;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingLinkInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;

//TODO: move this to my playground and rename to Zurich
public class ParkingModuleWithFFCarSharingZH extends GeneralParkingModule implements ParkingModuleWithFreeFloatingCarSharing {

	private Collection<ParkingCoordInfo> initialDesiredVehicleCoordinates;
	private HashMap<Id, Parking> currentVehicleLocation;
	private QuadTree<Id> vehicleLocations;
	
	public ParkingModuleWithFFCarSharingZH(Controler controler,Collection<ParkingCoordInfo> initialDesiredVehicleCoordinates) {
		super(controler);
		this.initialDesiredVehicleCoordinates = initialDesiredVehicleCoordinates;
		//TODO: initialize parkings car to parking
		
		SetupParkingForZHScenario.prepare(this,controler.getConfig());
		
		resetForNewIterationStart();
	}

	
	// TODO: we are not considering, that the number of vehicles is too limited, that no vehicle is available
	@Override
	public ParkingLinkInfo getNextFreeFloatingVehicle(Coord coord, Id personId, double time) {
		Id vehicleId = vehicleLocations.get(coord.getX(), coord.getY());
		
		Parking parking=currentVehicleLocation.get(vehicleId);
		parkingInfrastructureManager.unParkVehicle(parking);
		
		vehicleLocations.remove(parking.getCoordinate().getX(), parking.getCoordinate().getY(), vehicleId);
		
		NetworkImpl network = (NetworkImpl) getControler().getNetwork();
		
		double walkScore = parkingInfrastructureManager.getParkingScoreManager().calcWalkScore(coord, parking.getCoordinate(), personId, getAverageActDuration());
		
		parkingInfrastructureManager.getParkingScoreManager().addScore(personId,walkScore);

		return new ParkingLinkInfo(vehicleId, network.getNearestLink(parking.getCoordinate())
				.getId());
	}


	private double getAverageActDuration() {
		double averageActDuration = 135*60;
		return averageActDuration;
	}

	@Override
	public ParkingLinkInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord, Id personId, double time) {
		NetworkImpl network = (NetworkImpl) getControler().getNetwork();
		
		String groupName = getAcceptableParkingGroupName();
		
		Parking parking=parkingInfrastructureManager.parkAtClosestPublicParkingNonPersonalVehicle(destCoord, groupName, personId, getAverageActDuration());
		currentVehicleLocation.put(vehicleId, parking);
		vehicleLocations.put(parking.getCoordinate().getX(), parking.getCoordinate().getY(), vehicleId);
		
		return new ParkingLinkInfo(vehicleId, network.getNearestLink(parking.getCoordinate())
				.getId());
	}

	@Override
	public void resetForNewIterationStart() {
		EnclosingRectangle vehicleLocationsRect = new EnclosingRectangle();
		
		String groupName = getAcceptableParkingGroupName();
		
		currentVehicleLocation=new HashMap<Id, Parking>();
		
		for (ParkingCoordInfo parkInfo : initialDesiredVehicleCoordinates) {
			DebugLib.emptyFunctionForSettingBreakPoint();
			
			Parking parking=parkingInfrastructureManager.parkAtClosestPublicParkingNonPersonalVehicle(parkInfo.getParkingCoordinate(), groupName);
			currentVehicleLocation.put(parkInfo.getVehicleId(), parking);
			vehicleLocationsRect.registerCoord(parking.getCoordinate());
		}
		
		vehicleLocations = (new QuadTreeInitializer<Id>()).getQuadTree(vehicleLocationsRect);
		
		for (ParkingCoordInfo parkInfo : initialDesiredVehicleCoordinates) {
			Parking parking=currentVehicleLocation.get(parkInfo.getVehicleId());
			vehicleLocations.put(parking.getCoordinate().getX(), parking.getCoordinate().getY(), parkInfo.getVehicleId());
		}
	}


	private String getAcceptableParkingGroupName() {
		boolean restrictFFParkingToStreetParking = Boolean.parseBoolean(getControler().getConfig().getParam("parkingChoice.ZH", "restrictFFParkingToStreetParking"));
		
		String groupName=null;
		if (restrictFFParkingToStreetParking){
			groupName="streetParking";
		}
		return groupName;
	}
}
