package org.matsim.contrib.carsharing.manager.supply;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.utils.collections.QuadTree;
/** 
 * @author balac
 */
public class FreeFloatingVehiclesContainer implements VehiclesContainer{	
	
	private QuadTree<CSVehicle> availableFFVehicleLocationQuadTree;	
	private Map<String, CSVehicle> ffvehicleIdMap ;
	private Map<CSVehicle, Link> availableFFvehiclesMap ;
		
	public FreeFloatingVehiclesContainer(QuadTree<CSVehicle> ffVehicleLocationQuadTree,
			Map<String, CSVehicle> ffvehicleIdMap, Map<CSVehicle, Link> ffvehiclesMap) {
		
		this.availableFFVehicleLocationQuadTree = ffVehicleLocationQuadTree;
		this.availableFFvehiclesMap = ffvehiclesMap;
		this.ffvehicleIdMap = ffvehicleIdMap;
		
	}
	
	public QuadTree<CSVehicle> getFfVehicleLocationQuadTree() {
		return availableFFVehicleLocationQuadTree;
	}
	public Map<String, CSVehicle> getFfvehicleIdMap() {
		return ffvehicleIdMap;
	}
	public Map<CSVehicle, Link> getFfvehiclesMap() {
		return availableFFvehiclesMap;
	}
	
	public void reserveVehicle(CSVehicle vehicle) {	
			
		Link link = this.availableFFvehiclesMap.get(vehicle);
		Coord coord = link.getCoord();
		this.availableFFvehiclesMap.remove(vehicle);
		this.availableFFVehicleLocationQuadTree.remove(coord.getX(), coord.getY(), vehicle);
			
	}

	public void parkVehicle(CSVehicle vehicle, Link link) {
		Coord coord = link.getCoord();
					
		availableFFVehicleLocationQuadTree.put(coord.getX(), coord.getY(), vehicle);
		availableFFvehiclesMap.put(vehicle, link);
				
	}

	@Override
	public Link getVehicleLocation(CSVehicle vehicle) {
		return availableFFvehiclesMap.get(vehicle);
	}

	@Override
	public CSVehicle findClosestAvailableVehicle(Link startLink, String typeOfVehicle, double searchDistance) {
		CSVehicle vehicle = this.availableFFVehicleLocationQuadTree
				.getClosest(startLink.getCoord().getX(), startLink.getCoord().getY());		
		return vehicle;
	}

	@Override
	public Link findClosestAvailableParkingLocation(Link destinationLink, double searchDistance) {
		return null;
	}

	@Override
	public void reserveParking(Link destinationLink) {
		
	}
	
}
