/**
 * 
 */
package playground.tschlenther.parkingSearch.memoryBased;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import playground.tschlenther.parkingSearch.Benenson.BenensonDynLeg;

/**
 * @author Work
 *
 */
public class DistanceMemoryParkingSearchLogic implements ParkingSearchLogic {

	private static final Logger logger = Logger.getLogger(DistanceMemoryParkingSearchLogic.class);

	private static final boolean doLogging = false;
	private int destLinkEnterCount = 1;
	
	private Network network;
	private HashSet<Id<Link>> knownLinks;   
	
	/**
	 * @param network 
	 * 
	 */
	public DistanceMemoryParkingSearchLogic(Network network) {
		this.network = network;
		this.knownLinks = new HashSet<Id<Link>>();
	}

	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Link> destLinkId, Id<Vehicle> vehicleId) {
		
		Node toNode = network.getLinks().get(currentLinkId).getToNode();
		
		Set<Id<Link>> outLinks = toNode.getOutLinks().keySet();
		double shortestDistance = Double.MAX_VALUE;
		int nrKnownLinks = 0;
		Id<Link> nextLink = null;
		
		if(doLogging) logger.info("number of outlinks of link " + currentLinkId + ": " + outLinks.size());
		
		for(Id<Link> outLink : outLinks){
			
			if(this.knownLinks.contains(outLink)) nrKnownLinks ++;
			else{
				double distToDest = NetworkUtils.getEuclideanDistance(network.getLinks().get(outLink).getCoord(), network.getLinks().get(destLinkId).getCoord());
				if (distToDest < shortestDistance){
					nextLink = outLink;
					shortestDistance = distToDest;
					if(doLogging) logger.info("currently chosen link: " + nextLink + " distToDest: " + shortestDistance);
				}
				else if(distToDest == shortestDistance){
					String message = "link " + nextLink + " and link " + outLink + " both are " + distToDest + "m away from destination.";
						if (Math.random() > 0.5)
							nextLink = outLink;	
						if(doLogging) logger.info(message + " link " + nextLink + " is chosen.");
				}
				else{
					if (doLogging){
						logger.info("link " + outLink + " was not chosen because it is " + distToDest + "m away whereas shortest distance is " + shortestDistance);
					}
				}
			}
		}
		if(doLogging)logger.error("vehicle " + vehicleId + " knew " + nrKnownLinks + " out of " + outLinks.size() + " outlinks of link " + currentLinkId);
		if(outLinks.size() == nrKnownLinks ){
			if(doLogging)logger.error("vehicle " + vehicleId + " knows all outlinks of link " + currentLinkId);
//			for(Id<Link> outLink : outLinks){
//				double distToDest = NetworkUtils.getEuclideanDistance(network.getLinks().get(outLink).getCoord(), network.getLinks().get(destLinkId).getCoord()); 
//				if (distToDest < shortestDistance){
//					if(outLink.equals(destLinkId) && !(outLinks.size() == 1)){
//						double rnd = MatsimRandom.getRandom().nextDouble();
//						double thrshd = (2.0/3.0) * (1.0/destLinkEnterCount) ;
//						if( rnd < ( thrshd ) ){
//							destLinkEnterCount ++;
//							if(doLogging)logger.error("vehicle " + vehicleId + " takes destLink " + outLink + " as next link for the " + destLinkEnterCount + ". time");
//							return outLink;
//						}
//					}
//					else{
//						nextLink = outLink;
//						shortestDistance = distToDest;
//					}
//				}
//				else if(distToDest == shortestDistance ){
//						if (Math.random() > 0.5) nextLink = outLink;
//				}
//			}
			
			//gebe zufälligen Link zurück
			int index = MatsimRandom.getRandom().nextInt(outLinks.size());
			Iterator<Id<Link>> iter = outLinks.iterator();
			for (int i = 0; i < index; i++) {
			    iter.next();
			}
			nextLink = iter.next();
		}				
		
		if(nextLink == null){
			throw new RuntimeException("the next Link Id for vehicle " + vehicleId + " on current link " + currentLinkId + " couldn't be calculated.");
		}
		if(doLogging)logger.error("vehicle " + vehicleId + " takes link " + nextLink + " as next link");
		this.knownLinks.add(nextLink);
		return nextLink;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId) {
		throw new RuntimeException("shouldn't happen - method not implemented");
	}

	public void addToKnownLinks(Id<Link> linkId){
		this.knownLinks.add(linkId);
	}
	
}
