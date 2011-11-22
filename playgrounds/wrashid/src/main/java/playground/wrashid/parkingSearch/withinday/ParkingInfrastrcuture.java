package playground.wrashid.parkingSearch.withinday;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class ParkingInfrastrcuture {

	public static void init(NetworkImpl network){
		for (Link link:network.getLinks().values()){
			linkIdParkingFacilityIdsMapping.put(link.getId(), link.getId());
		}
	}
	
	private static LinkedListValueHashMap<Id, Id> linkIdParkingFacilityIdsMapping;
	
	private static IntegerValueHashMap<Id> facilityCapacities;
	
	public static int getFreeCapacity(Id facilityId){
		return facilityCapacities.get(facilityId);
	}
	
	public static void parkVehicle(Id facilityId){
		facilityCapacities.decrement(facilityId);
	}
	
	public static LinkedList<Id> getParkingsOnLink(Id linkId){
		return linkIdParkingFacilityIdsMapping.get(linkId);
	}
	
	public static void assignParkingFacilityToLink(Id facilityId,Id linkId){
		linkIdParkingFacilityIdsMapping.put(linkId, facilityId);
		facilityCapacities.incrementBy(facilityId, 100);
	}
	
	
}
