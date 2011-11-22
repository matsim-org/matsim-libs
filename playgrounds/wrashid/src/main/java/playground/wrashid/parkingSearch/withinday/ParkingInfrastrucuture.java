package playground.wrashid.parkingSearch.withinday;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.geoGrid.QuadTreeInitializer;

public class ParkingInfrastrucuture {

	private static QuadTree<Id> parkingFacilities;

	public static void init(NetworkImpl network, ActivityFacilities facilities) {
		QuadTree<Id> linkIds = (new QuadTreeInitializer<Id>()).getLinkQuadTree(network);

		for (Link link : network.getLinks().values()) {
			linkIds.put(link.getCoord().getX(), link.getCoord().getY(), link.getId());
		}
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			parkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility.getId());
			linkIdParkingFacilityIdsMapping.put(linkIds.get(facility.getCoord().getX(), facility.getCoord().getY()),
					facility.getId());
			
			// init capacities (TODO: assign capacity with the rest of other parking attributes).
			facilityCapacities.incrementBy(facility.getId(), 100);
		}
	}

	private static LinkedListValueHashMap<Id, Id> linkIdParkingFacilityIdsMapping;

	private static IntegerValueHashMap<Id> facilityCapacities;

	public static int getFreeCapacity(Id facilityId) {
		return facilityCapacities.get(facilityId);
	}

	public static void parkVehicle(Id facilityId) {
		facilityCapacities.decrement(facilityId);
	}

	public static void unParkVehicle(Id facilityId) {
		facilityCapacities.increment(facilityId);
	}

	public static LinkedList<Id> getParkingsOnLink(Id linkId) {
		return linkIdParkingFacilityIdsMapping.get(linkId);
	}


	public static Id getClosestFacilityFromLink(Link link) {
		return parkingFacilities.get(link.getCoord().getX(), link.getCoord().getY());
	}

}
