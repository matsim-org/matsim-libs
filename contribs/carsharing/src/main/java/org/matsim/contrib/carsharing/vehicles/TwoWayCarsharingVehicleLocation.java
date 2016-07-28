package org.matsim.contrib.carsharing.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.core.utils.collections.QuadTree;

public class TwoWayCarsharingVehicleLocation {
	
	private QuadTree<TwoWayCarsharingStation> vehicleLocationQuadTree;	
	private Map<String, TwoWayCarsharingStation> twowaycarsharingstationsMap;

	public TwoWayCarsharingVehicleLocation(Scenario scenario, ArrayList<TwoWayCarsharingStation> stations) {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

        for (Link l : scenario.getNetwork().getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    vehicleLocationQuadTree = new QuadTree<TwoWayCarsharingStation>(minx, miny, maxx, maxy);
	    twowaycarsharingstationsMap = new HashMap<String, TwoWayCarsharingStation>();
	    
	    for(TwoWayCarsharingStation f: stations) {  
	    	
	    	vehicleLocationQuadTree.put(f.getCoord().getX(), f.getCoord().getY(), f);
	    	twowaycarsharingstationsMap.put(f.getStationId(), f);
	    }
	   
	  }
	public QuadTree<TwoWayCarsharingStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
		
	public void addVehicle( String type, StationBasedVehicle vehicle) {
		
		TwoWayCarsharingStation station = this.twowaycarsharingstationsMap.get(vehicle.getStationId());
		
		station.addCar(type, vehicle);
	}
	
	public void removeVehicle( String type, StationBasedVehicle vehicle) {
		
		TwoWayCarsharingStation station = this.twowaycarsharingstationsMap.get(vehicle.getStationId());

		station.removeCar(type, vehicle);
		
	}		
	
	public TwoWayCarsharingStation getStationWIthId(String stationId) {
		
		return twowaycarsharingstationsMap.get(stationId);
	}
	
}
