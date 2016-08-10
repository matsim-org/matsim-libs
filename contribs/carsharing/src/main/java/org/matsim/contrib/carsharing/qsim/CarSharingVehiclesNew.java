package org.matsim.contrib.carsharing.qsim;


import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReader;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.FFVehicle;
import org.matsim.core.utils.collections.QuadTree;

public class CarSharingVehiclesNew {
	
	//private static final Logger log = Logger.getLogger(CarSharingVehicles.class);

	private Scenario scenario;	
	private QuadTree<FFVehicle> ffVehicleLocationQuadTree;	
	private Map<String, FFVehicle> ffvehicleIdMap = new HashMap<String, FFVehicle>();

	public Map<String, FFVehicle> getFfvehicleIdMap() {
		return ffvehicleIdMap;
	}

	private QuadTree<OneWayCarsharingStation> owvehicleLocationQuadTree;	
	private QuadTree<TwoWayCarsharingStation> twvehicleLocationQuadTree;
	private Map<String, TwoWayCarsharingStation> twowaycarsharingstationsMap;
	public Map<String, TwoWayCarsharingStation> getTwowaycarsharingstationsMap() {
		return twowaycarsharingstationsMap;
	}

	public Map<String, OneWayCarsharingStation> getOnewaycarsharingstationsMap() {
		return onewaycarsharingstationsMap;
	}

	private Map<String, OneWayCarsharingStation> onewaycarsharingstationsMap;

	private Map<FFVehicle, Link> ffvehiclesMap = new HashMap<FFVehicle, Link>();
	
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
		this.ffvehicleIdMap = carsharingReader.getFfvehicleIdMap();
	}
	
	public QuadTree<FFVehicle> getFfVehicleLocationQuadTree() {
		return ffVehicleLocationQuadTree;
	}

	public QuadTree<OneWayCarsharingStation> getOwvehicleLocationQuadTree() {
		return owvehicleLocationQuadTree;
	}

	public QuadTree<TwoWayCarsharingStation> getTwvehicleLocationQuadTree() {
		return twvehicleLocationQuadTree;
	}

	public Map<FFVehicle, Link> getFfvehiclesMap() {
		return ffvehiclesMap;
	}
	
	
		

}
