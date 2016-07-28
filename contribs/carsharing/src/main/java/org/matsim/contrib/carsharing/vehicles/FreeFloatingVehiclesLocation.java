package org.matsim.contrib.carsharing.vehicles;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

public class FreeFloatingVehiclesLocation {
	
	private QuadTree<FFCSVehicle> vehicleLocationQuadTree;	
	private static final Logger log = Logger.getLogger(FreeFloatingVehiclesLocation.class);

	private Scenario scenario;
	public FreeFloatingVehiclesLocation(Scenario scenario, ArrayList<FFCSVehicle> vehicles) {
		this.scenario = scenario;
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

	    vehicleLocationQuadTree = new QuadTree<FFCSVehicle>(minx, miny, maxx, maxy);
	    
	    
	    for(FFCSVehicle vehicle: vehicles) {  
	    	
	    	vehicleLocationQuadTree.put(vehicle.getLink().getCoord().getX(), vehicle.getLink().getCoord().getY(), vehicle);
	    }
	   
	  }	
	
	public QuadTree<FFCSVehicle> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(FFCSVehicle vehicle) {
		this.vehicleLocationQuadTree.put(vehicle.getLink().getCoord().getX(), vehicle.getLink().getCoord().getY(), vehicle);
		
	}
	
	public void removeVehicle(FFCSVehicle vehicle) {		
		
		this.vehicleLocationQuadTree.remove(vehicle.getLink().getCoord().getX(), vehicle.getLink().getCoord().getY(), vehicle);
				
	}
}
