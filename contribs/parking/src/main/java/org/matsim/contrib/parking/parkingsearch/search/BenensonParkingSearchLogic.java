package org.matsim.contrib.parking.parkingsearch.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.contrib.parking.parkingchoice.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.parkingsearch.DynAgent.BenensonDynLeg;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * @author schlenther
 *
 *the matsim version of the parking strategy used in PARKAGENT. see the following paper for more information:
 *doi: 10.1016/j.compenvurbsys.2008.09.011
 *
 */
public class BenensonParkingSearchLogic implements ParkingSearchLogic {
	private static final Logger logger = LogManager.getLogger(BenensonDynLeg.class);
	private static final boolean logForDebug = false;
	private final ParkingSearchManager parkingManager;


	private Network network;
	private static final double MIN_THRESHOLD_PROB_FUNCTION = 2;
	private static final double MAX_THRESHOLD_PROB_FUNCTION = 4;

    //thresholds for phase transitions: 1->2 and 2->3
	private static final double THRESHOLD_OBSERVING_METER = 1000;
	private static final double THRESHOLD_PARKING_METER = 500;

	private static final double ACCEPTED_DISTANCE_START = 100;
	private static final double ACCEPTED_DISTANCE_INCREASING_RATE_PER_MIN = 100;
	private static final double ACCEPTED_DISTANCE_MAX = 600;
	private static final double range = 500;
	private final Random random = MatsimRandom.getLocalInstance();
	//private HashSet<Id<Link>> knownLinks;
	private ParkingSearchConfigGroup configGroup;
	//private DoubleValueHashMap<Id<Link>> FreeKnownParkLinks;

	public BenensonParkingSearchLogic(Network network, ParkingSearchConfigGroup cfgGroup, ParkingSearchManager parkingManager) {
		this.network = network;
		this.configGroup = cfgGroup;
		this.parkingManager = parkingManager;
	}

	@Override
	public void reset() {

	}
	//----------------------------------------------------phase transitions----------------------------------------------------------------------------

    public boolean transitionToObservingBehaviour(Id<Link> currLinkId, Id<Link> endLinkId) {
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currLinkId).getCoord(), network.getLinks().get(endLinkId).getCoord());
		return distToDest < THRESHOLD_OBSERVING_METER ;
	}

    public boolean transitionToParkingBehaviour(Id<Link> currLinkId, Id<Link> endLinkId) {
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currLinkId).getCoord(), network.getLinks().get(endLinkId).getCoord());
		return distToDest < THRESHOLD_PARKING_METER ;
	}


	/**
	 * @author  Talha
	 *
	 */
	/*public boolean isWithinRange(Person person) {
		Map<String, Id<Vehicle>> vehicles = VehicleUtils.getVehicleIds(person);


		// Check if the current vehicle is within 500 meters of any other vehicle

		double distance = Double.MAX_VALUE;
		for (int i = 0; i < vehicles.size(); i++) {

			for (int j = 0; j < vehicles.size(); j++) {

				if (j != i) {
					distance=( DistanceUtils.calculateDistance(network.getLinks().get(vehicles[i]).getCoord(), network.getLinks().get(vehicles[j]).getCoord()));

				}

			}
		}

		// Check if the distance is within the specified range
		return distance <= range;
	}
*/

	//-------------------------------------------------------routing---------------------------------------------------------------------------

	/**
	 *
	 * @param currentLinkId
	 * @param destinationLinkId
	 * @return
	 */
	public Id<Link> getNextLinkBenensonRouting(Id<Link> currentLinkId, Id<Link> destinationLinkId, String mode) {
		Link currentLink = network.getLinks().get(currentLinkId);
		//int nrKnownLinks = 0;
        //calculate the distance to fromNode of destination link instead of distance to activity
		Node destination = network.getLinks().get(destinationLinkId).getFromNode();

		double distanceToDest = Double.MAX_VALUE;
		Node nextNode;
		Id<Link> nextLinkId = null;

		for (Link outLink : ParkingUtils.getOutgoingLinksForMode(currentLink, mode)) {
			Id<Link> outLinkId = outLink.getId();
			//if (this.knownLinks.contains(outLinkId)) nrKnownLinks++;
			if (outLinkId.equals(destinationLinkId)) {
				return outLinkId;
			}
			nextNode = outLink.getToNode();
			double dd = NetworkUtils.getEuclideanDistance(destination.getCoord(),nextNode.getCoord());
			if( dd < distanceToDest){
				nextLinkId = outLinkId;
				distanceToDest = dd;
			}
			else if(dd == distanceToDest){
				if (Math.random() > 0.5){
					nextLinkId = outLinkId;
				}
			}
		}
		//this.knownLinks.add(nextLinkId);
		return nextLinkId;
	}

	public Id<Link> getNextLinkRandomInAcceptableDistance(Id<Link> currentLinkId, Id<Link> endLinkId, Id<Vehicle> vehicleId, double firstDestLinkEnterTime, double timeOfDay, String mode) {

		Link nextLink;
		Link currentLink = network.getLinks().get(currentLinkId);
		List<Link> outGoingLinks = ParkingUtils.getOutgoingLinksForMode(currentLink, mode);
		List<Link> outGoingLinksCopy = new ArrayList<>(outGoingLinks);
		int nrOfOutGoingLinks = outGoingLinks.size();
		for (int i = 1; i <= nrOfOutGoingLinks; i++) {
			nextLink = outGoingLinks.get(random.nextInt(outGoingLinks.size()));
            if (isDriverInAcceptableDistance(nextLink.getId(), endLinkId, firstDestLinkEnterTime, timeOfDay)) return nextLink.getId();
			outGoingLinks.remove(nextLink);
        }
        logger.error("vehicle " + vehicleId + " finds no outlink in acceptable distance going out from link " + currentLinkId + ". it just takes a random next link");
		return outGoingLinksCopy.get(random.nextInt(outGoingLinksCopy.size())).getId();

	}


	public Id<Link> getNextLinkAfterCommunication( Id<Link> destinationLinkId,Id<Link> currentLinkId) {
		//Map<Id<Link>, Set<Id<ActivityFacility>>> FreeParkLinks = ((FacilityBasedParkingManager) this.parkingManager).getFreeParkingSpacesLinks();

		Map<Id<Link>, Set<Id<ActivityFacility>>>  FreeParkLinks=((FacilityBasedParkingManager) this.parkingManager).getLinkIdifParkingavailible(currentLinkId);







		//calculate the distance to fromNode of destination link instead of distance to activity
		Node destination = network.getLinks().get(destinationLinkId).getFromNode();


		// The next link is determined by calculating the Euclidean distance between the destination node and the to-node of the outgoing links from the current link.
		// The link with the minimum distance to the destination node is selected as the next link.
		// The routing stops when the current link is equal to the destination link.

		double distanceToDest = Double.MAX_VALUE;
		Node nextNode;
		Id<Link> ParkLinkId = null;

		for (Id<Link> outLinkId : FreeParkLinks.keySet()) {
			if (((FacilityBasedParkingManager) this.parkingManager).getNrOfFreeParkingSpacesOnLink(outLinkId)>0){
				Link outLink = network.getLinks().get(outLinkId);


				//If the destination link is not found among the outgoing links of the current link,
				// the method calculates the Euclidean distance from the to-node of each outgoing link to the destination node.
				//The outgoing link with the smallest distance is chosen as the next link to be followed.

				nextNode = outLink.getToNode();
				double ddd = NetworkUtils.getEuclideanDistance(destination.getCoord(),nextNode.getCoord());
				if( ddd < distanceToDest){
					ParkLinkId = outLinkId;
					distanceToDest = ddd;
				}
				else if(ddd == distanceToDest){
					if (Math.random() > 0.5){
						ParkLinkId = outLinkId;
					}
				}
			}
		}

		return ParkLinkId;

	}

/*

   public Id<Link> getNextLinkCommunicationCenterRouting( Id<Link> destinationLinkId) {
      Map<Id<Link>, Set<Id<ActivityFacility>>> FreeParkLinks = ((FacilityBasedParkingManager) this.parkingManager).getFreeParkingSpacesLinks();

      //calculate the distance to fromNode of destination link instead of distance to activity
      Node destination = network.getLinks().get(destinationLinkId).getFromNode();


      // The next link is determined by calculating the Euclidean distance between the destination node and the to-node of the outgoing links from the current link.
      // The link with the minimum distance to the destination node is selected as the next link.
      // The routing stops when the current link is equal to the destination link.

      double distanceToDest = Double.MAX_VALUE;
      Node nextNode;
      Id<Link> ParkLinkId = null;

      for (Id<Link> outLinkId : FreeParkLinks.keySet()) {
         if (((FacilityBasedParkingManager) this.parkingManager).getNrOfFreeParkingSpacesOnLink(outLinkId)>0){
         Link outLink = network.getLinks().get(outLinkId);


         //If the destination link is not found among the outgoing links of the current link,
         // the method calculates the Euclidean distance from the to-node of each outgoing link to the destination node.
         //The outgoing link with the smallest distance is chosen as the next link to be followed.

         nextNode = outLink.getToNode();
         double ddd = NetworkUtils.getEuclideanDistance(destination.getCoord(),nextNode.getCoord());
         if( ddd < distanceToDest){
            ParkLinkId = outLinkId;
            distanceToDest = ddd;
         }
         else if(ddd == distanceToDest){
            if (Math.random() > 0.5){
               ParkLinkId = outLinkId;
            }
         }
      }
      }

      return ParkLinkId;

   }
*/





/*

	private void updateFreeParkingSpaces(Map<Integer, Id<Link>> linkId,Id<Vehicle> vid,Id<Link> currentLinkId) {
		// Update the information about free parking spaces on the current link
		Map<Id<Link>, Set<Id<ActivityFacility>>> CurrentlinksWithParking=((FacilityBasedParkingManager) this.parkingManager).getLinkIdifParkingavailible(currentLinkId);


		for (Map.Entry<Id<Link>, Set<Id<ActivityFacility>>> entry :  CurrentlinksWithParking.entrySet()) {
			Id<Link> parkingSpaceId = entry.getKey();
			Set<Id<ActivityFacility>> isFree = entry.getValue();

			// Update the free parking space information
			if (isFree) {
				freeParkingSpacesOnCurrentLink.put(parkingSpaceId, true);
			} else {
				freeParkingSpacesOnCurrentLink.remove(parkingSpaceId);
			}
		}
	}
*/



	//---------------------------------------------------park decision-----------------------------------------------------------------------

	/**
	 *
	 * estimate amount of free parking spaces on the way to destination Link and decide whether to park on currentLink
	 *
	 * @param pUnoccupied
	 * @param currentLinkId
	 * @param endLinkId
     * @return whether vehicle should be parked here
	 */
	public boolean wantToParkHere (double pUnoccupied, Id<Link> currentLinkId, Id<Link> endLinkId) {

		//if pUnoccupied = 0, no free slot has been detected, so it is realistic to accept the very next free slot
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currentLinkId).getToNode().getCoord(), network.getLinks().get(endLinkId).getToNode().getCoord());
		double expectedFreeSlots = (pUnoccupied*distToDest/configGroup.getAvgparkingslotlength());
		double rnd = Math.random();
        if (logForDebug)
            logger.error("\n current link: " + currentLinkId + "\n expected slots: " + expectedFreeSlots + "\n probabilty to continue driving: " + getProbabilityOfContinuingToDrive(expectedFreeSlots) + "\n rnd: " + rnd);
        return rnd >= getProbabilityOfContinuingToDrive(expectedFreeSlots);
	}

	/**
     * linear probability function, depending on maximum and minimum threshold
	 */
    private double getProbabilityOfContinuingToDrive(double expectedFreeSlots) {

		if (expectedFreeSlots < MIN_THRESHOLD_PROB_FUNCTION) return 0.0;
		else if(expectedFreeSlots > MAX_THRESHOLD_PROB_FUNCTION) return 1.0;

		return (expectedFreeSlots-MIN_THRESHOLD_PROB_FUNCTION)/(MAX_THRESHOLD_PROB_FUNCTION-MIN_THRESHOLD_PROB_FUNCTION);
	}

	/**
	 *
	 * @param currentLinkId
	 * @param endLinkId
     * @param firstDestLinkEnterTime
	 * @param timeOfDay
	 * @return
	 */
    private boolean isDriverInAcceptableDistance(Id<Link> currentLinkId, Id<Link> endLinkId, double firstDestLinkEnterTime, double timeOfDay) {

		// if we're on the destinationLink, we always want to park
		if(currentLinkId.equals(endLinkId)) return true;

		double distToDest = NetworkUtils.getEuclideanDistance(network.getLinks().get(currentLinkId).getCoord(), network.getLinks().get(endLinkId).getCoord());
		double timeSpent = timeOfDay - firstDestLinkEnterTime;
		double acceptedDistance = ACCEPTED_DISTANCE_START + ACCEPTED_DISTANCE_INCREASING_RATE_PER_MIN * (timeSpent / 60);

		if (acceptedDistance > ACCEPTED_DISTANCE_MAX) acceptedDistance = ACCEPTED_DISTANCE_MAX;

		if (distToDest <= acceptedDistance){
			if(logForDebug){
				logger.error(" distance between link " + currentLinkId + " and destLink " + endLinkId + ": " + distToDest);
				logger.error("accepted : " + acceptedDistance);
				logger.error("time spent: " + timeSpent);
			}
			return true;
		}
		return false;
	}











	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId, String mode) {
		throw new RuntimeException("this should not happen!");
	}


}
