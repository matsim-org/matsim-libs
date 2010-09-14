package playground.wrashid.parkingSearch.planLevel.ranking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkParkingFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingAttribute;
import playground.wrashid.parkingSearch.planLevel.scoring.OrderedFacility;

/**
 * TODO: The performance could be improved by perform caching here.
 * 
 * 
 * @author rashid_waraich
 * 
 */
public class ClosestParkingMatrix {

	private NetworkImpl network;
	LinkParkingFacilityAssociation parkingAssociations;

	public ClosestParkingMatrix(ActivityFacilitiesImpl facilities, NetworkImpl network) {
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

		return getClosestParkings(coord, minNumberOfParkings, maxNumberOfParkings, null);

		
	}

	/**
	 * Removes the facility, which is most far away from coord (from the list)
	 * 
	 * @param coord
	 * @param list
	 * @return
	 */
	private ArrayList<ActivityFacilityImpl> removeMostFarAwayFacility(Coord coord, ArrayList<ActivityFacilityImpl> list) {
		double maxDistance = Double.MIN_VALUE;
		int maxDistanceIndex = -1;

		for (int i = 0; i < list.size(); i++) {
			double distance = GeneralLib.getDistance(coord, list.get(i).getCoord());
			if (distance > maxDistance) {
				maxDistance = distance;
				maxDistanceIndex = i;
			}
		}

		// remove the parking, which is most far away from the list
		list.remove(maxDistanceIndex);

		return list;
	}

	/**
	 * Get all parkings, which have less than maxDistance from coord
	 * 
	 * 
	 * @param coord
	 * @param maxDistance
	 * @return
	 */
	public ArrayList<ActivityFacilityImpl> getClosestParkings(Coord coord, double maxDistance) {
		return getClosestParkings(coord, maxDistance, null);		
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

			if (GeneralLib.getDistance(coord, selectedLink) <= maxDistance) {
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
					if (!checkedLinks.contains(curLink) && !untestedLinks.contains(curLink)) {
						untestedLinks.add(curLink);
					}
				}

			}
		}
		return resultLinks;

	}

	/**
	 * possible refactoring: move this static method to some other class, where
	 * it would fit better...
	 * 
	 * attention: probably concurrency not possible, if list can be modified by
	 * different threads. (if that is required, make a new list within the
	 * method). This means, in the result list the facility, which is closest
	 * will be first in the queue.
	 * 
	 * @param coord
	 * @param list
	 * @return
	 */
	public static ArrayList<ActivityFacilityImpl> getOrderedListAccordingToDistanceFromCoord(Coord coord,
			ArrayList<ActivityFacilityImpl> list) {
		PriorityQueue<OrderedFacility> prioQueue = new PriorityQueue<OrderedFacility>();

		// sort list
		while (list.size() > 0) {
			ActivityFacilityImpl curFac = list.remove(0);
			prioQueue.add(new OrderedFacility(curFac, GeneralLib.getDistance(coord, curFac.getCoord())));
		}

		// write list
		while (prioQueue.size() > 0) {
			ActivityFacilityImpl curFac = prioQueue.poll().getFacility();
			list.add(curFac);
		}

		return list;
	}

	public ArrayList<ActivityFacilityImpl> getClosestParkings(Coord coord, double maxDistance, ParkingAttribute personParkingAttribute) {
		// TODO Auto-generated method stub
		LinkedList<Link> links = getClosestLinks(coord, maxDistance);
		ArrayList<ActivityFacilityImpl> resultFacilities = new ArrayList<ActivityFacilityImpl>();

		for (int i = 0; i < links.size(); i++) {
			resultFacilities.addAll(parkingAssociations.getFacilitiesHavingParkingAttribute(links.get(i).getId(),personParkingAttribute));
		}

		return resultFacilities;
	}

	public ArrayList<ActivityFacilityImpl> getClosestParkings(Coord coord, int minNumberOfParkings, int maxNumberOfParkings, ParkingAttribute personParkingAttribute) {
		// increment distance by to until a parking is found
		ArrayList<ActivityFacilityImpl> list = null;

		double maxDistance = 100;
		list = getClosestParkings(coord, maxDistance, personParkingAttribute);

		int maxTimeSetExtention = 10;

		// try to find more parkings only 'maxTimeSetExtention' times
		for (int i = 0; i < maxTimeSetExtention; i++) {
			if (list.size() >= minNumberOfParkings) {
				// enough parkings found
				break;
			} else {
				// need to find more parkings
				maxDistance *= 2;
				list = getClosestParkings(coord, maxDistance);
			}
		}

		// just return the result, if no trimming if result required
		if (maxNumberOfParkings == 0 || list.size() <= maxNumberOfParkings) {
			return list;
		} else {
			// trim the facility set: discard parkings most far away from the
			// given coord
			while (list.size() > maxNumberOfParkings) {
				list = removeMostFarAwayFacility(coord, list);
			}
			return list;
		}
		
	}

}
