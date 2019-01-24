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
public class TwoWayContainer implements VehiclesContainer {

	private QuadTree<CarsharingStation> twvehicleLocationQuadTree;
	private Map<String, CarsharingStation> twowaycarsharingstationsMap;
	private Map<CSVehicle, Link> twvehiclesMap;

	public TwoWayContainer(QuadTree<CarsharingStation> twvehicleLocationQuadTree,
			Map<String, CarsharingStation> twowaycarsharingstationsMap, Map<CSVehicle, Link> twvehiclesMap) {

		this.twvehicleLocationQuadTree = twvehicleLocationQuadTree;
		this.twowaycarsharingstationsMap = twowaycarsharingstationsMap;
		this.twvehiclesMap = twvehiclesMap;

	}

	public boolean reserveVehicle(CSVehicle vehicle) {
		String stationId = ((StationBasedVehicle) vehicle).getStationId();

		if (stationId == null) {
			return false;
		}

		this.twvehiclesMap.remove(vehicle);

		CarsharingStation station = twowaycarsharingstationsMap.get(stationId);
		((TwoWayCarsharingStation) station).removeCar(vehicle);

		return true;
	}

	public void parkVehicle(CSVehicle vehicle, Link link) {
		twvehiclesMap.put(vehicle, link);
		String stationId = ((StationBasedVehicle) vehicle).getStationId();
		CarsharingStation station = twowaycarsharingstationsMap.get(stationId);

		((TwoWayCarsharingStation) station).addCar(vehicle.getType(), vehicle);
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
		synchronized (twvehicleLocationQuadTree) {
			Collection<CarsharingStation> location = this.twvehicleLocationQuadTree.getDisk(startLink.getCoord().getX(),
					startLink.getCoord().getY(), searchDistance);
			if (location.isEmpty())
				return null;

			CarsharingStation closest = null;
			double closestFound = searchDistance;

			for (CarsharingStation station : location) {

				Coord coord = station.getLink().getCoord();

				if (CoordUtils.calcEuclideanDistance(startLink.getCoord(), coord) < closestFound
						&& ((TwoWayCarsharingStation) station).getNumberOfVehicles(typeOfVehicle) > 0) {
					closest = station;
					closestFound = CoordUtils.calcEuclideanDistance(startLink.getCoord(), coord);
				}
			}

			if (closest != null) {
				CSVehicle vehicleToBeUsed = ((TwoWayCarsharingStation) closest).getVehicles(typeOfVehicle).get(0);
				return vehicleToBeUsed;
			}

			return null;
		}
	}

	@Override
	public Link findClosestAvailableParkingLocation(Link destinationLink, double searchDistance) {
		return null;
	}

	@Override
	public void reserveParking(Link destinationLink) {
	}
}
