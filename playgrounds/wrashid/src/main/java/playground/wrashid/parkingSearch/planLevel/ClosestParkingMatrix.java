package playground.wrashid.parkingSearch.planLevel;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;

public class ClosestParkingMatrix {
	
	private NetworkLayer network;
	LinkParkingFacilityAssociation parkingAssociations;

	// TODO: add constructor: numberOfParkings.
	
	public ClosestParkingMatrix(ActivityFacilitiesImpl facilities, NetworkLayer network) {
		this.network=network;
		this.parkingAssociations=new LinkParkingFacilityAssociation(facilities,network);
	}
	
	
	// TODO: getNumberOfParkings (for how many this was created).
	
	/**
	 *  create an object, which maintains the 'numberOfParkings' closest parkings to a link.
	 */
	public void getClosestParkings(String link){
		
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
	
	
	public ArrayList<ActivityFacilityImpl> getClosestParkings(Coord coord, double maxDistance){
		LinkedList<Link> links = getClosestLinks(coord, maxDistance);
		ArrayList<ActivityFacilityImpl> resultFacilities=new ArrayList<ActivityFacilityImpl>();
		
		for (int i=0;i<links.size();i++){
			resultFacilities.addAll(parkingAssociations.getFacilities(links.get(i).getId()));
		}
		
		return resultFacilities;
	}
	
	
	
	/**
	 * maxDistance in meters.
	 * @param linkId
	 * @param maxDistance
	 */
	public LinkedList<Link> getClosestLinks(Coord coord, double maxDistance){
		Link initialLink=network.getNearestLink(coord);
		
		LinkedList<Link> untestedLinks=new LinkedList<Link>();
		LinkedList<Link> resultLinks=new LinkedList<Link>();		
		LinkedList<Link> checkedLinks=new LinkedList<Link>();
		
		untestedLinks.add(initialLink);
		
		while (untestedLinks.size()>0){
			Link selectedLink=untestedLinks.removeFirst();
			
			checkedLinks.add(selectedLink);
			
			if (getDistance(coord,selectedLink)<=maxDistance){
				resultLinks.add(selectedLink);
				
				// find out, if need to test the neighbouring links
				
				LinkedList<Link> neighbourLinks=new LinkedList<Link>();
				
				Node toNode=selectedLink.getToNode();
				Node fromNode=selectedLink.getFromNode();
				
				neighbourLinks.addAll(toNode.getOutLinks().values());
				neighbourLinks.addAll(fromNode.getInLinks().values());
				
				// only add link, if not already checked
				
				while (neighbourLinks.size()>0){
					Link curLink=neighbourLinks.removeFirst();
					if (!checkedLinks.contains(curLink)){
						untestedLinks.add(curLink);
					}
				}
				
			}
		}
		return resultLinks;
		
	}
	
	private double getDistance(Coord coord, Link link){
		double xDiff=coord.getX()-link.getCoord().getX();
		double yDiff=coord.getY()-link.getCoord().getY();
		return Math.sqrt(xDiff*xDiff + yDiff*yDiff);
	}
	
	
	
	
	
	
	
	
	
	
}
