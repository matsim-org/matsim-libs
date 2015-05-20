package org.matsim.contrib.carsharing.vehicles;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.core.utils.collections.QuadTree;

public class TwoWayCarsharingVehicleLocation {
	
	private QuadTree<TwoWayCarsharingStation> vehicleLocationQuadTree;	
	
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
	    
	    
	    for(TwoWayCarsharingStation f: stations) {  
	    	
	    	vehicleLocationQuadTree.put(f.getLink().getCoord().getX(), f.getLink().getCoord().getY(), f);
	    }
	   
	  }
	public QuadTree<TwoWayCarsharingStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(Link link, String id) {
		
		TwoWayCarsharingStation f = vehicleLocationQuadTree.get(link.getCoord().getX(), link.getCoord().getY());
		
		if (f == null || !f.getLink().getId().toString().equals(link.getId().toString())) {
			
			
			ArrayList<String> vehIDs = new ArrayList<String>();
			
			vehIDs.add(id);
			
			TwoWayCarsharingStation fNew = new TwoWayCarsharingStation(link, 1, vehIDs);		
			
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
			
		}
		else {
			ArrayList<String> vehIDs = f.getIDs();
			ArrayList<String> newvehIDs = new ArrayList<String>();
			for (String s : vehIDs) {
				newvehIDs.add(s);
			}
			newvehIDs.add(0, id);
			TwoWayCarsharingStation fNew = new TwoWayCarsharingStation(link, f.getNumberOfVehicles() + 1, newvehIDs);		
			vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), f);
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
		}
		
		
	}
	
	public void removeVehicle(TwoWayCarsharingStation station, String id) {
		
		
			ArrayList<String> vehIDs = station.getIDs();
			ArrayList<String> newvehIDs = new ArrayList<String>();
			for (String s : vehIDs) {
				newvehIDs.add(s);
			}
			
			if (!newvehIDs.remove(id))
				throw new NullPointerException("Removing the vehicle did not wok");

			TwoWayCarsharingStation fNew = new TwoWayCarsharingStation(station.getLink(), station.getNumberOfVehicles() - 1, newvehIDs);	
			
						
			if (!vehicleLocationQuadTree.remove(station.getLink().getCoord().getX(), station.getLink().getCoord().getY(), station)) 
				throw new NullPointerException("Removing the station did not wok");
			vehicleLocationQuadTree.put(station.getLink().getCoord().getX(), station.getLink().getCoord().getY(), fNew);
			
		
	}
	
	
}
