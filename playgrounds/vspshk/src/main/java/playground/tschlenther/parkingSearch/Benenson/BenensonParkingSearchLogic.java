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
	private Network network;
	private static final double MIN_THRESHOLD_PROB_FUNCTION = 10;
	private static final double MAX_THRESHOLD_PROB_FUNCTION = 30;
	private static final double ACCEPTED_DISTANCE_INCREASING_RATE_PER_MIN = 30;
	private static final double ACCEPTED_DISTANCE_MAX = 600;
	private final Random random = MatsimRandom.getLocalInstance();
	
	private static final double THRESHOLD_EXPECTED_FREE_PARKINGSPACES = 5;
	private static final double THRESHOLD_ALL_EXPECTED_PARKINGSPACES_TO_DEST = 290/TSParkingUtils.AVGPARKINGSLOTLENGTH;
	

	public BenensonParkingSearchLogic(Network network) {
		this.network = network;
	}
	
	@Override
	public void reset() {

	}
	
	/**
	 * 
	 * @param currentLinkId
	 * @param destinationLinkId
	 * @param vehicleId
	 * @param hasTriedDestLinkBefore
	 * @return
	 */
	// sollte unbenannt werden: wird in PHASE 1 und 2 benutzt
	public Id<Link> getNextLinkPhase2(Id<Link> currentLinkId, Id<Link> destinationLinkId, Id<Vehicle> vehicleId, boolean hasTriedDestLinkBefore) {
		Link currentLink = network.getLinks().get(currentLinkId);
		//List<Id<Link>> nextNodes = new ArrayList<>();
		
		//es wird nicht die Distanz zur Aktivität, sondern zum fromNode des Aktivitätenlinks berechnet
		Node destination = network.getLinks().get(destinationLinkId).getFromNode();
		
		double distanceToDest = Double.MAX_VALUE;
		Node nextNode;
		Id<Link> nextLinkId = null;
		
		
		/* 10.02: nicht mehr aktuell
		 * TODO: Problem (beim grid-Net) (wenn in PHASE 3 verwendet):
		 * wenn agent auf destLink fährt wird er immer umkehren und immer "auf der selben Seite" der aktivität suchen. => generelles Benenson-Problem.
		 * => Lösung ist Annahme zufälligen Routens in PHASE3
		 */
		for (Id<Link> outlinkId : currentLink.getToNode().getOutLinks().keySet()){
			if(outlinkId.equals(destinationLinkId)){
				if(!hasTriedDestLinkBefore) return outlinkId;			//TODO: nicht nötig wenn nextLink methoden nach Phasen aufgeteilt
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



	public Id<Link> getNextLinkPhase3(Id<Link> currentLinkId, Id<Link> endLinkId, Id<Vehicle> vehicleId, double firstDestLinkEnterTime, double timeOfDay) {
		//throw new RuntimeException("i don't want this to happen");
		Id<Link> nextLink = null;
		Link currentLink = network.getLinks().get(currentLinkId);
		do{
			List<Id<Link>> keys = new ArrayList<>(currentLink.getToNode().getOutLinks().keySet());
			if(!(nextLink == null)) keys.remove(keys.indexOf(nextLink));
			nextLink= keys.get(random.nextInt(keys.size()));	
		}
		while(!isDriverInAcceptableDistance(nextLink, endLinkId, firstDestLinkEnterTime, timeOfDay));
		//logger.error("vehicle " + vehicleId  + " turns on link " + nextLink + " after " + (firstDestLinkEnterTime - timeOfDay) + " secs of searching");
		return nextLink;
	}

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
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currentLinkId).getToNode().getCoord(), network.getLinks().get(endLinkId).getFromNode().getCoord());
		//TODO: falls der agent schon auf dem endLink ist, dann auf jeden Fall ja?
		//
		if(currentLinkId.equals(endLinkId)) return true;
		//TODO: problem wenn distToDest = 0, dann will auf jeden Fall geparkt werden (z.B. auf dem Link gegenüber)
		//=> man sollte eigentlich auf jeden Fall auf den Ziellink.
		//=> was wenn man schon weiß dass der voll ist.
		/*
		 * wenn man fromNode vom EndLink zur Berechnung der Distanz verwendet hat man obiges Problem (umgehbar)
		 * wenn man toNode verwendet hat man anderes Problem:
		 * agenten müssen über den fromNode zur Aktivität laufen -> u.U. längere Distanz
		 * 
		 */
		//TODO: Problem wenn pUnoccupied gleich 0
		//=> dann gab es bisher noch keinen freien Slot, also ist es realistisch, den nächsten zu akzeptieren   
		if( (pUnoccupied*distToDest/TSParkingUtils.AVGPARKINGSLOTLENGTH) <= THRESHOLD_EXPECTED_FREE_PARKINGSPACES ) return true;
		return false;
	}
	
	public boolean wantToParkHereV2 (double pUnoccupied, Id<Link> currentLinkId, Id<Link> endLinkId) {
				
		//TODO: Problem wenn pUnoccupied gleich 0
		//=> dann gab es bisher noch keinen freien Slot, also ist es realistisch, den nächsten zu akzeptieren   
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currentLinkId).getToNode().getCoord(), network.getLinks().get(endLinkId).getToNode().getCoord());
		double expectedFreeSlots = (pUnoccupied*distToDest/TSParkingUtils.AVGPARKINGSLOTLENGTH);
		double rnd = Math.random();
		logger.error("\n current link: "+ currentLinkId + "\n expected slots: " + expectedFreeSlots + "\n probabilty to continue driving: " + getProbabilityOfContinueDriving(expectedFreeSlots) + "\n rnd: " + rnd);
		if (rnd < getProbabilityOfContinueDriving(expectedFreeSlots)) return false;
		else return true;
	}
	
	public boolean becomeActive (Id<Link> currentLinkId, Id<Link> destinationLinkId){
		double distToDest = NetworkUtils.getEuclideanDistance(
				network.getLinks().get(currentLinkId).getToNode().getCoord(), network.getLinks().get(destinationLinkId).getFromNode().getCoord());
		if( (distToDest/TSParkingUtils.AVGPARKINGSLOTLENGTH) <= THRESHOLD_ALL_EXPECTED_PARKINGSPACES_TO_DEST ) return true;
		return false;
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
		

		/*
		 * 
		Coord toCoord;
		Coord fromCoord;
		double startX;
		double startY;
		double endX;
		double endY;
		
		
		 
		fromCoord = network.getLinks().get(currentLinkId).getToNode().getCoord();
		toCoord = network.getLinks().get(currentLinkId).getFromNode().getCoord();
		
		startX = toCoord.getX() - fromCoord.getX();
		startY = toCoord.getX() - fromCoord.getY();
		
		fromCoord = network.getLinks().get(endLinkId).getToNode().getCoord();
		toCoord = network.getLinks().get(endLinkId).getFromNode().getCoord();
		
		endX = ( (toCoord.getX() - fromCoord.getX()) / 2) ;
		endY = toCoord.getX() - fromCoord.getY();
				
		double distToDest = NetworkUtils.getEuclideanDistance(startX, startY, endX, endY);
		
		*/
		
		double distToDest = NetworkUtils.getEuclideanDistance(network.getLinks().get(currentLinkId).getCoord(), network.getLinks().get(endLinkId).getCoord());

		double timeSpent = timeOfDay - firstDestLinkEnterTime;
		double acceptedDistance = 100 + ACCEPTED_DISTANCE_INCREASING_RATE_PER_MIN * (timeSpent / 60);
		
		if (acceptedDistance > ACCEPTED_DISTANCE_MAX) acceptedDistance = ACCEPTED_DISTANCE_MAX;
		
		//logger.error("\n Distanz zum Ziel: " + distToDest + "\n Anfang Phase 3 : " + firstDestLinkEnterTimer + "\n Jetzt: " + timeOfDay +  "\n Zeit (s) in Phase3: " + timeSpent + "\n akzeptierte Distanz: " + acceptedDistance);
		
		if (distToDest <= acceptedDistance){
			logger.error(" distance between link " + currentLinkId + " and destLink " + endLinkId + ": " + distToDest);
			logger.error("accepted : " + acceptedDistance);
			logger.error("time spent: " + timeSpent);
			return true;		
		}
		
		return false;
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
