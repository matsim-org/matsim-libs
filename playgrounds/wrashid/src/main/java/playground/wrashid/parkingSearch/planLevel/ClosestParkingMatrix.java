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

	public ClosestParkingMatrix(ActivityFacilitiesImpl facilities, NetworkLayer network) {
		this.network = network;
		this.parkingAssociations = new LinkParkingFacilityAssociation(facilities, network);
	}

	/**
	 * Find close parkings close to coord, if possible at least
	 * minNumberOfParkings. If more than maxNumberOfParkings are found, then
	 * discard the furtherest away parkings.
	 * 
	 * -maxNumberOfParkings=0 heisst unendlich (no limit)
	 * 
	 * @param coord
	 * @param minNumberOfParkings
	 * @param maxNumberOfParkings
	 */
	public ArrayList<ActivityFacilityImpl> getClosestParkings(Coord coord, int minNumberOfParkings, int maxNumberOfParkings) {
		// increment distance by to until a parking is found
		ArrayList<ActivityFacilityImpl> list=null;
		
		double maxDistance=100;
		list=getClosestParkings(coord,maxDistance);
		
		int maxTimeSetExtention=10;
		
		// try to find more parkings only 'maxTimeSetExtention' times
		for (int i=0;i<maxTimeSetExtention;i++){
			if (list.size()>=minNumberOfParkings){
				// enough parkings found
				break;
			} else {
				// need to find more parkings
				maxDistance*=2;
				list=getClosestParkings(coord,maxDistance);
			}
		}
		
		// just return the result, if no trimming if result required
		if (maxNumberOfParkings==0 || list.size()<=maxNumberOfParkings){
			return list;
		} else {
			// trim the facility set: discard parkings most far away from the given coord
			while (list.size()>maxNumberOfParkings){
				list=removeMostFarAwayFacility(coord,list);
			}
			return list;
		}
	}
	
	/**
	 * Removes the facility, which is most far away from coord (from the list)
	 * @param coord
	 * @param list
	 * @return
	 */
	private ArrayList<ActivityFacilityImpl> removeMostFarAwayFacility(Coord coord, ArrayList<ActivityFacilityImpl> list){
		double maxDistance=Double.MIN_VALUE;
		int maxDistanceIndex=-1;
		
		for (int i=0;i<list.size();i++){
			double distance=getDistance(coord, list.get(i).getCoord());
			if (distance>maxDistance){
				maxDistance=distance;
				maxDistanceIndex=i;
			}
		}
		
		// remove the parking, which is most far away from the list
		list.remove(maxDistanceIndex);
		
		return list;
	}
	
	
	

	/**
	 * Get all parkings, which have less than maxDistance from coord
	 * 
	 * @param coord
	 * @param maxDistance
	 * @return
	 */
	public ArrayList<ActivityFacilityImpl> getClosestParkings(Coord coord, double maxDistance) {
		LinkedList<Link> links = getClosestLinks(coord, maxDistance);
		ArrayList<ActivityFacilityImpl> resultFacilities = new ArrayList<ActivityFacilityImpl>();

		for (int i = 0; i < links.size(); i++) {
			if (parkingAssociations.getFacilities(links.get(i).getId())==null){
				System.out.println(links.get(i).getId());
				System.out.println(parkingAssociations.getFacilities(links.get(i).getId()));
				System.exit(0);
			}
			
			resultFacilities.addAll(parkingAssociations.getFacilities(links.get(i).getId()));
		}

		return resultFacilities;
	}

	/**
	 * maxDistance in meters.
	 * 
	 * @param linkId
	 * @param maxDistance
	 */
	public LinkedList<Link> getClosestLinks(Coord coord, double maxDistance) {
		Link initialLink = network.getNearestLink(coord);

		LinkedList<Link> untestedLinks = new LinkedList<Link>();
		LinkedList<Link> resultLinks = new LinkedList<Link>();
		LinkedList<Link> checkedLinks = new LinkedList<Link>();

		untestedLinks.add(initialLink);

		while (untestedLinks.size() > 0) {
			Link selectedLink = untestedLinks.removeFirst();

			checkedLinks.add(selectedLink);

			if (getDistance(coord, selectedLink) <= maxDistance) {
				resultLinks.add(selectedLink);

				// find out, if need to test the neighbouring links

				LinkedList<Link> neighbourLinks = new LinkedList<Link>();

				Node toNode = selectedLink.getToNode();
				Node fromNode = selectedLink.getFromNode();

				neighbourLinks.addAll(toNode.getOutLinks().values());
				neighbourLinks.addAll(fromNode.getInLinks().values());

				// only add link, if not already checked

				while (neighbourLinks.size() > 0) {
					Link curLink = neighbourLinks.removeFirst();
					if (!checkedLinks.contains(curLink)) {
						untestedLinks.add(curLink);
					}
				}

			}
		}
		return resultLinks;

	}

	private double getDistance(Coord coord, Link link) {
		return getDistance(coord,link.getCoord());
	}
	
	private double getDistance(Coord coordA, Coord coordB) {
		double xDiff = coordA.getX() - coordB.getX();
		double yDiff = coordA.getY() - coordB.getY();
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}

}
