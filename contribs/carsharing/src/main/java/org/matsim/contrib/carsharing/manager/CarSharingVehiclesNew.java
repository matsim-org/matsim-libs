package org.matsim.contrib.carsharing.manager;


import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReader;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.utils.collections.QuadTree;

public class CarSharingVehiclesNew {

	private Scenario scenario;	
	private QuadTree<CSVehicle> ffVehicleLocationQuadTree;	
	
	private Map<String, CSVehicle> csvehicleIdMap = new HashMap<String, CSVehicle>();
	public Map<String, CSVehicle> getCsvehicleIdMap() {
		return csvehicleIdMap;
	}

	private Map<String, CSVehicle> ffvehicleIdMap = new HashMap<String, CSVehicle>();
	private Map<String, CSVehicle> owvehicleIdMap = new HashMap<String, CSVehicle>();

	private QuadTree<CarsharingStation> owvehicleLocationQuadTree;	
	private QuadTree<CarsharingStation> twvehicleLocationQuadTree;
	private Map<String, CarsharingStation> twowaycarsharingstationsMap;
	private Map<String, CarsharingStation> onewaycarsharingstationsMap;

	private Map<CSVehicle, Link> ffvehiclesMap = new HashMap<CSVehicle, Link>();
	private Map<CSVehicle, Link> owvehiclesMap = new HashMap<CSVehicle, Link>();
	private Map<CSVehicle, Link> twvehiclesMap = new HashMap<CSVehicle, Link>();
	//private Map<CSVehicle, String> owvehiclesStationMap = new HashMap<CSVehicle, String>();

	public CarSharingVehiclesNew(Scenario scenario) {
		this.scenario = scenario;
	}
	
	public void readVehicleLocations() {
		
		CarsharingXmlReader carsharingReader = new CarsharingXmlReader(scenario.getNetwork());
		
		final TwoWayCarsharingConfigGroup configGrouptw = (TwoWayCarsharingConfigGroup)
				scenario.getConfig().getModule( TwoWayCarsharingConfigGroup.GROUP_NAME );

		carsharingReader.readFile(configGrouptw.getvehiclelocations());
		
		this.ffVehicleLocationQuadTree = carsharingReader.getFfVehicleLocationQuadTree();
		this.owvehicleLocationQuadTree = carsharingReader.getOwvehicleLocationQuadTree();
		this.twvehicleLocationQuadTree = carsharingReader.getTwvehicleLocationQuadTree();
		
		this.onewaycarsharingstationsMap = carsharingReader.getOnewaycarsharingstationsMap();
		this.twowaycarsharingstationsMap = carsharingReader.getTwowaycarsharingstationsMap();
		
		this.ffvehiclesMap = carsharingReader.getFfvehiclesMap();	
		this.owvehiclesMap = carsharingReader.getOwvehiclesMap();	
		this.twvehiclesMap = carsharingReader.getTwvehiclesMap();	

		this.ffvehicleIdMap = carsharingReader.getFfvehicleIdMap();
		this.owvehicleIdMap = carsharingReader.getOwvehicleIdMap();
		this.csvehicleIdMap = carsharingReader.getCsehiclesMap();
	}

	public Map<CSVehicle, Link> getOwvehiclesMap() {
		return owvehiclesMap;
	}

	
	public Map<String, CSVehicle> getFfvehicleIdMap() {
		return ffvehicleIdMap;
	}
	public Map<String, CarsharingStation> getTwowaycarsharingstationsMap() {
		return twowaycarsharingstationsMap;
	}

	public Map<String, CarsharingStation> getOnewaycarsharingstationsMap() {
		return onewaycarsharingstationsMap;
	}	
	
	public QuadTree<CSVehicle> getFfVehicleLocationQuadTree() {
		return ffVehicleLocationQuadTree;
	}

	public QuadTree<CarsharingStation> getOwvehicleLocationQuadTree() {
		return owvehicleLocationQuadTree;
	}

	public QuadTree<CarsharingStation> getTwvehicleLocationQuadTree() {
		return twvehicleLocationQuadTree;
	}

	public Map<CSVehicle, Link> getFfvehiclesMap() {
		return ffvehiclesMap;
	}
	public Map<String, CSVehicle> getOwvehicleIdMap() {
		return owvehicleIdMap;
	}
	public Link getLocationVehicle(CSVehicle vehicle) {
		
		if (vehicle.getCsType().equals("freefloating"))
			return ffvehiclesMap.get(vehicle);
		else if (vehicle.getCsType().equals("oneway")) 			
			return owvehiclesMap.get(vehicle);		
		else if (vehicle.getCsType().equals("twoway")) 			
			return twvehiclesMap.get(vehicle);	
			
		
		return null;
	}
	public void reserveVehicle(CSVehicle vehicle) {

		if (vehicle.getCsType().equals("freefloating")) {
			
			Link link = this.ffvehiclesMap.get(vehicle);
			Coord coord = link.getCoord();
			this.ffvehiclesMap.remove(vehicle);
			this.ffVehicleLocationQuadTree.remove(coord.getX(), coord.getY(), vehicle);
		}
		else if (vehicle.getCsType().equals("oneway")) {
			Link link = this.owvehiclesMap.get(vehicle);
			Coord coord = link.getCoord();
			this.owvehiclesMap.remove(vehicle);			
			CarsharingStation station = owvehicleLocationQuadTree.getClosest(coord.getX(), coord.getY());
			
			((OneWayCarsharingStation)station).removeCar(vehicle);
			
		}
		else if (vehicle.getCsType().equals("twoway")) {
			Link link = this.twvehiclesMap.get(vehicle);
			Coord coord = link.getCoord();
			this.twvehiclesMap.remove(vehicle);			
			CarsharingStation station = twvehicleLocationQuadTree.getClosest(coord.getX(), coord.getY());
			
			boolean c = ((TwoWayCarsharingStation)station).removeCar(vehicle);
			System.out.println(c);			
		}		
	}

	public void parkVehicle(String vehicleId, Link link) {
		CSVehicle vehicle = this.csvehicleIdMap.get(vehicleId);
		Coord coord = link.getCoord();
		if (vehicleId.startsWith("FF")) {
			
			ffVehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), vehicle);
			ffvehiclesMap.put(vehicle, link);
			
		}
		else if (vehicleId.startsWith("OW")) {
			
			owvehiclesMap.put(vehicle, link);
			
			CarsharingStation station = owvehicleLocationQuadTree.getClosest(coord.getX(), coord.getY());
			((OneWayCarsharingStation)station).addCar(((StationBasedVehicle)vehicle).getVehicleType(),  vehicle);

			
		}
		else if (vehicleId.startsWith("TW")) {
			
			twvehiclesMap.put(vehicle, link);
			
			CarsharingStation station = twvehicleLocationQuadTree.getClosest(coord.getX(), coord.getY());
			((TwoWayCarsharingStation)station).addCar(((StationBasedVehicle)vehicle).getVehicleType(),  vehicle);

			
		}
		
	}
	
	public void reserveParkingSlot(CarsharingStation parkingStation) {

		((OneWayCarsharingStation)parkingStation).reserveParkingSpot();		
	}
	
	
}
