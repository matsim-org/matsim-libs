/**
 * 
 */
package playground.tschlenther.parkingSearch.Benenson;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import playground.tschlenther.parkingSearch.utils.TSParkingUtils;

/**
 * @author schlenther
 *
 */
public class BenensonParkingSearchLogic implements ParkingSearchLogic {
	private static final Logger logger = Logger.getLogger(BenensonDynLeg.class);
	private static final boolean logForDebug = false;
	
	private Network network;
	private static final double MIN_THRESHOLD_PROB_FUNCTION = 1;
	private static final double MAX_THRESHOLD_PROB_FUNCTION = 3;
	private static final double ACCEPTED_DISTANCE_INCREASING_RATE_PER_MIN = 30;
	private static final double ACCEPTED_DISTANCE_MAX = 400;
	private final Random random = MatsimRandom.getLocalInstance();
	
	//Grenzen für Übergang von Phase 1 -> 2 bzw. 2->3
	private static final double THRESHOLD_OBSERVING_METER = 250;
	private static final double THRESHOLD_PARKING_METER = 100;	

	public BenensonParkingSearchLogic(Network network) {
		this.network = network;
	}
	
	@Override
	public void reset() {

	}
	//----------------------------------------------------Phasenübergänge----------------------------------------------------------------------------
	
	boolean goIntoObserving (Id<Link> currLinkId, Id<Link> endLinkId){
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currLinkId).getCoord(), network.getLinks().get(endLinkId).getCoord());
		return distToDest < THRESHOLD_OBSERVING_METER ;
	}
	
	boolean goIntoParking (Id<Link> currLinkId, Id<Link> endLinkId){
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currLinkId).getCoord(), network.getLinks().get(endLinkId).getCoord());
		return distToDest < THRESHOLD_PARKING_METER ;
	}
	
	//-------------------------------------------------------Routing---------------------------------------------------------------------------
	
	/**
	 * 
	 * @param currentLinkId
	 * @param destinationLinkId
	 * @param vehicleId
	 * @param hasTriedDestLinkBefore
	 * @return
	 */
	public Id<Link> getNextLinkBenensonRouting(Id<Link> currentLinkId, Id<Link> destinationLinkId, Id<Vehicle> vehicleId) {
		Link currentLink = network.getLinks().get(currentLinkId);
		//List<Id<Link>> nextNodes = new ArrayList<>();
		
		//es wird nicht die Distanz zur Aktivität, sondern zum fromNode des Aktivitätenlinks berechnet
		Node destination = network.getLinks().get(destinationLinkId).getFromNode();
		
		double distanceToDest = Double.MAX_VALUE;
		Node nextNode;
		Id<Link> nextLinkId = null;

		for (Id<Link> outlinkId : currentLink.getToNode().getOutLinks().keySet()){
			if(outlinkId.equals(destinationLinkId)){
				return outlinkId;
			}
			nextNode = network.getLinks().get(outlinkId).getToNode();
			double dd = NetworkUtils.getEuclideanDistance(destination.getCoord(),nextNode.getCoord());
			if( dd < distanceToDest){
				nextLinkId = outlinkId;
				distanceToDest = dd;
			}
			else if(dd == distanceToDest){
				if (Math.random() > 0.5){
					nextLinkId = outlinkId;
				}
			}
		}
		return nextLinkId;
	}

	public Id<Link> getNextLinkRandomInAcceptableDistance(Id<Link> currentLinkId, Id<Link> endLinkId, Id<Vehicle> vehicleId, double firstDestLinkEnterTime, double timeOfDay) {

		Id<Link> nextLink = null;
		Link currentLink = network.getLinks().get(currentLinkId);
		List<Id<Link>> keys = new ArrayList<>(currentLink.getToNode().getOutLinks().keySet());
		do{
			if(!(nextLink == null)) keys.remove(keys.indexOf(nextLink));
			
			if(keys.size() == 0){	//kein outlink in acceptaple Distance
				keys = new ArrayList<>(currentLink.getToNode().getOutLinks().keySet());
				logger.error("vehicle " + vehicleId + " finds no outlink in acceptable distance going out from link " + currentLinkId + ". it just takes a random next link");
				return keys.get(random.nextInt(keys.size()));
			}
			nextLink= keys.get(random.nextInt(keys.size()));	
		}
		while(!isDriverInAcceptableDistance(nextLink, endLinkId, firstDestLinkEnterTime, timeOfDay));
		//logger.error("vehicle " + vehicleId  + " turns on link " + nextLink + " after " + (firstDestLinkEnterTime - timeOfDay) + " secs of searching");
		return nextLink;
	}

	//---------------------------------------------------Parkentscheidung-----------------------------------------------------------------------

	/**
	 * 
	 * estimate amount of free parking spaces on the way to destination Link and decide whether to park on currentLink
	 * 
	 * @param pUnoccupied
	 * @param currentLinkId
	 * @param endLinkId
	 * @return
	 */
	public boolean wantToParkHere (double pUnoccupied, Id<Link> currentLinkId, Id<Link> endLinkId) {
		//TODO: Problem wenn pUnoccupied gleich 0
		//=> dann gab es bisher noch keinen freien Slot, also ist es realistisch, den nächsten zu akzeptieren   
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currentLinkId).getToNode().getCoord(), network.getLinks().get(endLinkId).getToNode().getCoord());
		double expectedFreeSlots = (pUnoccupied*distToDest/TSParkingUtils.AVGPARKINGSLOTLENGTH);
		double rnd = Math.random();
		if(logForDebug)logger.error("\n current link: "+ currentLinkId + "\n expected slots: " + expectedFreeSlots + "\n probabilty to continue driving: " + getProbabilityOfContinueDriving(expectedFreeSlots) + "\n rnd: " + rnd);
		if (rnd < getProbabilityOfContinueDriving(expectedFreeSlots)) return false;
		else return true;
	}
	
	/**
	 * linear probability function, depending on maximum and minimium threshold 
	 */
	private double getProbabilityOfContinueDriving (double expectedFreeSlots){
		
		if (expectedFreeSlots < MIN_THRESHOLD_PROB_FUNCTION) return 0.0;
		else if(expectedFreeSlots > MAX_THRESHOLD_PROB_FUNCTION) return 1.0;
		
		return (expectedFreeSlots-MIN_THRESHOLD_PROB_FUNCTION)/(MAX_THRESHOLD_PROB_FUNCTION-MIN_THRESHOLD_PROB_FUNCTION);
	}

	/**
	 * 
	 * @param currentLinkId
	 * @param endLinkId
	 * @param firstDestLinkEnterTimer
	 * @param timeOfDay
	 * @return
	 */
	public boolean isDriverInAcceptableDistance(Id<Link> currentLinkId, Id<Link> endLinkId,	double firstDestLinkEnterTime, double timeOfDay) {

		// if we're on the destinationLink, we always want to park
		if(currentLinkId.equals(endLinkId)) return true;
		
		double distToDest = NetworkUtils.getEuclideanDistance(network.getLinks().get(currentLinkId).getCoord(), network.getLinks().get(endLinkId).getCoord());

		double timeSpent = timeOfDay - firstDestLinkEnterTime;
		double acceptedDistance = 100 + ACCEPTED_DISTANCE_INCREASING_RATE_PER_MIN * (timeSpent / 60);
		
		if (acceptedDistance > ACCEPTED_DISTANCE_MAX) acceptedDistance = ACCEPTED_DISTANCE_MAX;
		
		//logger.error("\n Distanz zum Ziel: " + distToDest + "\n Anfang Phase 3 : " + firstDestLinkEnterTimer + "\n Jetzt: " + timeOfDay +  "\n Zeit (s) in Phase3: " + timeSpent + "\n akzeptierte Distanz: " + acceptedDistance);
		
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
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId) {
		throw new RuntimeException("this should not happen!");
	}
	
}
