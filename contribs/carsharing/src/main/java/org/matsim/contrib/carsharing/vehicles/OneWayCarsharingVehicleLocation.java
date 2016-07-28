package org.matsim.contrib.carsharing.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.core.utils.collections.QuadTree;


public class OneWayCarsharingVehicleLocation {


	private QuadTree<OneWayCarsharingStation> vehicleLocationQuadTree;	
	private Map<String, OneWayCarsharingStation> onewaycarsharingstationsMap;

	
	public OneWayCarsharingVehicleLocation(Scenario sc, ArrayList<OneWayCarsharingStation> stations)  {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

        for (Link l : sc.getNetwork().getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    
	    vehicleLocationQuadTree = new QuadTree<OneWayCarsharingStation>(minx, miny, maxx, maxy);
	    onewaycarsharingstationsMap = new HashMap<String, OneWayCarsharingStation>();
	    
	    for(OneWayCarsharingStation f: stations) {  
	    	
	    	vehicleLocationQuadTree.put(f.getCoord().getX(), f.getCoord().getY(), f);
	    	onewaycarsharingstationsMap.put(f.getStationId(), f);
	    }	   	    	   
    }
	public QuadTree<OneWayCarsharingStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle( String type, StationBasedVehicle vehicle) {
		
		OneWayCarsharingStation station = this.onewaycarsharingstationsMap.get(vehicle.getStationId());
		
		station.addCar(type, vehicle);
	}
	
	public void removeVehicle( String type, StationBasedVehicle vehicle) {
		
		OneWayCarsharingStation station = this.onewaycarsharingstationsMap.get(vehicle.getStationId());

		station.removeCar(type, vehicle);
		
	}		
	
	public OneWayCarsharingStation getStationWIthId(String stationId) {
		
		return onewaycarsharingstationsMap.get(stationId);
	}
	
	public void reserveParkingSpot(OneWayCarsharingStation station) {
		
		station.reserveParkingSpot();
	}
	
	public void freeParkingSpot(OneWayCarsharingStation station) {
		
		station.freeParkingSpot();
	}

	
	
}
