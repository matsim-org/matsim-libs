package org.matsim.contrib.carsharing.manager.supply;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
/** 
 * @author balac
 */
public class TwoWayContainer implements VehiclesContainer{
	
	
	private QuadTree<CarsharingStation> twvehicleLocationQuadTree;
	private Map<String, CarsharingStation> twowaycarsharingstationsMap;
	private Map<CSVehicle, Link> twvehiclesMap;

	public TwoWayContainer(QuadTree<CarsharingStation> twvehicleLocationQuadTree,
			Map<String, CarsharingStation> twowaycarsharingstationsMap, Map<CSVehicle, Link> twvehiclesMap) {
		
		this.twvehicleLocationQuadTree = twvehicleLocationQuadTree;
		this.twowaycarsharingstationsMap = twowaycarsharingstationsMap;
		this.twvehiclesMap = twvehiclesMap;
		
	}
	
	
	public void reserveVehicle(CSVehicle vehicle) {		
		Link link = this.twvehiclesMap.get(vehicle);
		Coord coord = link.getCoord();
		this.twvehiclesMap.remove(vehicle);			
		CarsharingStation station = twvehicleLocationQuadTree.getClosest(coord.getX(), coord.getY());			
		((TwoWayCarsharingStation)station).removeCar(vehicle);
				
	}

	public void parkVehicle(CSVehicle vehicle, Link link) {
		Coord coord = link.getCoord();			
		twvehiclesMap.put(vehicle, link);
			
		CarsharingStation station = twvehicleLocationQuadTree.getClosest(coord.getX(), coord.getY());
		((TwoWayCarsharingStation)station).addCar(((StationBasedVehicle)vehicle).getVehicleType(),  vehicle);		
	}

	public Map<String, CarsharingStation> getTwowaycarsharingstationsMap() {
		return twowaycarsharingstationsMap;
	}

	@Override
	public Link getVehicleLocation(CSVehicle vehicle) {
		return twvehiclesMap.get(vehicle);
	}

	@Override
	public CSVehicle findClosestAvailableVehicle(Link startLink, String typeOfVehicle, double searchDistance) {
			Collection<CarsharingStation> location = 
				this.twvehicleLocationQuadTree.getDisk(startLink.getCoord().getX(), 
						startLink.getCoord().getY(), searchDistance);
		if (location.isEmpty()) return null;

		CarsharingStation closest = null;
		double closestFound = searchDistance;

		for(CarsharingStation station: location) {
			
			Coord coord = station.getLink().getCoord();
			
			if (CoordUtils.calcEuclideanDistance(startLink.getCoord(), coord) < closestFound 
					&& ((TwoWayCarsharingStation)station).getNumberOfVehicles(typeOfVehicle) > 0) {
				closest = station;
				closestFound = CoordUtils.calcEuclideanDistance(startLink.getCoord(), coord);
			}
		}
		
		if (closest != null) {
			CSVehicle vehicleToBeUsed = ((TwoWayCarsharingStation)closest).getVehicles(typeOfVehicle).get(0);
			return vehicleToBeUsed;
		}
		
		return null;
	}

	@Override
	public Link findClosestAvailableParkingLocation(Link destinationLink, double searchDistance) {
		return null;
	}


	@Override
	public void reserveParking(Link destinationLink) {
		// TODO Auto-generated method stub
		
	}		
}
