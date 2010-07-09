package playground.wrashid.parkingSearch.planLevel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;

public class ClosestParkingMatrix {
	
	// TODO: add constructor: numberOfParkings.
	
	
	// TODO: getNumberOfParkings (for how many this was created).
	
	/**
	 *  create an object, which maintains the 'numberOfParkings' closest parkings to a link.
	 */
	public void getClosestParking(String link){
		
	}
	
	/**
	 * Get the facilityIds of the first 'numberOfParkings' parkings located closest to the 'targetFacility'.
	 * 
	 * The euclidean distance from the targetFacility to each of the parking facilities is measured.
	 * 
	 * @param numberOfParkings
	 * @param network
	 * @param targetLink
	 */
	public void getXCoseParkings(int numberOfParkings, NetworkImpl network, ActivityFacilityImpl targetFacility){
		
		Id facilityLinkId=targetFacility.getLinkId();
		Link link=network.getLinks().get(facilityLinkId);
		
	}
	
	/**
	 * distance in meters.
	 * @param linkId
	 * @param distance
	 */
	public void getClosestLinks(String linkId, String distance){
		
	}
	
	
	
	
	
	
	
	
}
