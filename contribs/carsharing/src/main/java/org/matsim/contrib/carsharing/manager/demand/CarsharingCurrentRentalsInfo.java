package org.matsim.contrib.carsharing.manager.demand;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.utils.collections.QuadTree;
/** 
 * @author balac
 */
public class CarsharingCurrentRentalsInfo {
	
	private Map<String, QuadTree<CSVehicle>> currentRentals = new HashMap<String, QuadTree<CSVehicle>>(); 
	
	
	private Network network;
	
	public CarsharingCurrentRentalsInfo(Network network) {
		
		this.network = network;
	}
	
	public void addType(String carsharingType) {
		
		double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
		double maxx = (-1.0D / 0.0D);
		double maxy = (-1.0D / 0.0D);

	    for (Link l : this.network.getLinks().values()) {
	    	
		    if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
		    if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
		    if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
		    if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
		}
		minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
		
		currentRentals.put(carsharingType, new QuadTree<CSVehicle>(minx, miny, maxx, maxy));
	}
	
	public boolean hasVehicleOnLink(Link link, String type) {
		QuadTree<CSVehicle> vehicleLocations = currentRentals.get(type);
		Coord coord = link.getCoord();
		Collection<CSVehicle> vehicles = vehicleLocations.getDisk(coord.getX(), coord.getY(), 0.0);
		if (vehicles.isEmpty())
			return false;
		else
			return true;
	}
	
	public CSVehicle getVehicleOnLink(Link link, String type) {
		QuadTree<CSVehicle> vehicleLocations = currentRentals.get(type);
		if (vehicleLocations != null) {
			Coord coord = link.getCoord();
			Collection<CSVehicle> vehicles = vehicleLocations.getDisk(coord.getX(), coord.getY(), 0.0);
			
			if (!vehicles.isEmpty())
				return (CSVehicle) vehicles.iterator().next();
		}

		return null;
	}

	
	public boolean addVehicle(Link link, CSVehicle vehicle, String type) {
		QuadTree<CSVehicle> vehicleLocations = currentRentals.get(type);
		Coord coord = link.getCoord();
		
		return vehicleLocations.put(coord.getX(), coord.getY(), vehicle);
	}
	
	public boolean removeVehicle(Link link, CSVehicle vehicle, String type) {
		QuadTree<CSVehicle> vehicleLocations = currentRentals.get(type);		
		Coord coord = link.getCoord();
		
		return vehicleLocations.remove(coord.getX(), coord.getY(), vehicle);
	}
	
	public Map<String, QuadTree<CSVehicle>> getCurrentRentals() {
		return currentRentals;
	}
	
}
