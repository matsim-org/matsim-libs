/**
 * 
 */
package playground.tschlenther.parkingSearch.memoryBased;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import playground.tschlenther.parkingSearch.Benenson.BenensonDynLeg;

/**
 * @author Work
 *
 */
public class SlotMemoryBasedParkingSearchLogic implements ParkingSearchLogic {

	private FacilityBasedParkingManager parkingManager;

	private static final Logger logger = Logger.getLogger(SlotMemoryBasedParkingSearchLogic.class);

	private Network network;
	private HashSet<Id<Link>> knownLinks;   
	
	/**
	 * @param network 
	 * 
	 */
	public SlotMemoryBasedParkingSearchLogic(Network network, FacilityBasedParkingManager fBasedParkingManager) {
		this.network = network;
		this.knownLinks = new HashSet<Id<Link>>();
		this.parkingManager = fBasedParkingManager;
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId) {
		
		Node toNode = network.getLinks().get(currentLinkId).getToNode();
		
		Set<Id<Link>> outLinks = toNode.getOutLinks().keySet();
		double mostParkingSlots = 0;
		int nrKnownLinks = 0;
		Id<Link> nextLink = null;
		
		for(Id<Link> outLink : outLinks){
			if(this.knownLinks.contains(outLink)) nrKnownLinks ++;
			else{
				if(parkingManager == null){
					throw new RuntimeException("parkingManager is null");
				}
				double nrSlots = parkingManager.getNrOfAllParkingSpacesOnLink(outLink) ;
				if (nrSlots > mostParkingSlots){
					nextLink = outLink;
					mostParkingSlots = nrSlots;
				}
				else if(nrSlots == mostParkingSlots){
					if(nrSlots == 0) nextLink = outLink;
					else{
						if (Math.random() > 0.5) nextLink = outLink;
					}
				}
			}
		}
		
		if(outLinks.size() == nrKnownLinks ){
			logger.error("vehicle " + vehicleId + " knows all outlinks of link " + currentLinkId);
			for(Id<Link> outLink : outLinks){
				double nrSlots = parkingManager.getNrOfAllParkingSpacesOnLink(outLink); 
				if (nrSlots> mostParkingSlots){
					nextLink = outLink;
					mostParkingSlots = nrSlots;
				}
				else if(nrSlots == mostParkingSlots){
					if(nrSlots == 0) nextLink = outLink;
					else{
						if (Math.random() > 0.5) nextLink = outLink;
					}
				}
			}
		}				
		
		if(nextLink == null){
			throw new RuntimeException("the next Link Id for vehicle " + vehicleId + " on current link " + currentLinkId + " couldn't be calculated.");
		}
		
		this.knownLinks.add(nextLink);
		return nextLink;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

}
