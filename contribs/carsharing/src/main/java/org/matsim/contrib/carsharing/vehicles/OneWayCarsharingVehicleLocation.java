package org.matsim.contrib.carsharing.vehicles;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.core.utils.collections.QuadTree;


public class OneWayCarsharingVehicleLocation {


	private QuadTree<OneWayCarsharingStation> vehicleLocationQuadTree;	
	
	
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
	    
	    
	    for(OneWayCarsharingStation f: stations) {  
	    	
	    	ArrayList<String> vehIDs = f.getIDs();
			ArrayList<String> newvehIDs = new ArrayList<String>();
			for (String s : vehIDs) {
				newvehIDs.add(s);
			}
			Link link = sc.getNetwork().getLinks().get( f.getLinkId() ) ;
	    	
			OneWayCarsharingStation fNew = new OneWayCarsharingStation(link, f.getNumberOfVehicles(), newvehIDs,  f.getNumberOfAvailableParkingSpaces());		

	    	vehicleLocationQuadTree.put(fNew.getCoord().getX(), fNew.getCoord().getY(), fNew);
	    }
	    
	  
	    
	   
	  }
	public QuadTree<OneWayCarsharingStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(OneWayCarsharingStation station, String id) {
		
			station.getIDs().add(id);
			station.addCar();
		
	}
	
	public void removeVehicle(OneWayCarsharingStation station, String id) {
		
		station.getIDs().remove(id);
		station.removeCar();
				
	}
	
	public void reserveParkingSpot(OneWayCarsharingStation station) {
		
		station.reserveParkingSpot();
	}
	
	public void freeParkingSpot(OneWayCarsharingStation station) {
		
		station.freeParkingSpot();
	}

	
	
}
