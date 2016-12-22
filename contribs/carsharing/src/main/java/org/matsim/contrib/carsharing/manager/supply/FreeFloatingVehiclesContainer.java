package org.matsim.contrib.carsharing.manager.supply;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.qsim.FreefloatingAreas;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.FFVehicleImpl;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
/** 
 * @author balac
 */
public class FreeFloatingVehiclesContainer implements VehiclesContainer{	
	
	private QuadTree<CSVehicle> availableFFVehicleLocationQuadTree;	
	private Map<String, CSVehicle> ffvehicleIdMap;
	private Map<CSVehicle, Link> availableFFvehiclesMap;
	private SearchableNetwork network;
	private FreefloatingAreas freefloatingAreas;

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

	public void setNetwork(SearchableNetwork network) {
		this.network = network;
	}

	public SearchableNetwork getNetwork() {
		return this.network;
	}

	public void setFreefloatingAreas(FreefloatingAreas areas) {
		this.freefloatingAreas = areas;
	}

	public FreefloatingAreas getFreefloatingAreas() {
		return this.freefloatingAreas;
	}

	public boolean reserveVehicle(CSVehicle vehicle) {
		Link link = this.availableFFvehiclesMap.get(vehicle);

		if (link == null) {
			return false;
		}

		Coord coord = link.getCoord();
		this.availableFFvehiclesMap.remove(vehicle);
		this.availableFFVehicleLocationQuadTree.remove(coord.getX(), coord.getY(), vehicle);

		return true;
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
		Collection<CSVehicle> location = 
				availableFFVehicleLocationQuadTree.getDisk(startLink.getCoord().getX(), 
						startLink.getCoord().getY(), searchDistance);
		if (location.isEmpty()) return null;

		CSVehicle closest = null;
		double closestFound = searchDistance;

		for (CSVehicle vehicle: location) {
			if (vehicle.getType().equals(typeOfVehicle)) {
				Link vehicleLink = this.availableFFvehiclesMap.get(vehicle);

				if (vehicleLink != null) {
					Coord coord = this.availableFFvehiclesMap.get(vehicle).getCoord();

					if (CoordUtils.calcEuclideanDistance(startLink.getCoord(), coord) < closestFound ) {
						closest = vehicle;
						closestFound = CoordUtils.calcEuclideanDistance(startLink.getCoord(), coord);
					}
				}
			}
		}

		return closest;
	}

	@Override
	public Link findClosestAvailableParkingLocation(Link destinationLink, double searchDistance) {
		if (this.getFreefloatingAreas() == null) {
			return destinationLink;
		}

		Coord destination = destinationLink.getCoord();

		if (this.getFreefloatingAreas().contains(destination)) {
			return destinationLink;
		} else {
			Coord[] nearestPoints = this.getFreefloatingAreas().nearestPoints(destination);

			double distance = CoordUtils.calcEuclideanDistance(nearestPoints[0], nearestPoints[1]);

			if ((this.getNetwork() != null) && (distance <= searchDistance)) {
				return this.getNetwork().getNearestLinkExactly(nearestPoints[0]);
			} else {
				return null;
			}
		}
	}

	@Override
	public void reserveParking(Link destinationLink) {
		
	}
}
