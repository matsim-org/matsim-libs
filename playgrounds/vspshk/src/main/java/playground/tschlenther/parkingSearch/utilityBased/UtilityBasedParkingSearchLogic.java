/**
 * 
 */
package playground.tschlenther.parkingSearch.utilityBased;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author Work
 *
 */
public class UtilityBasedParkingSearchLogic implements ParkingSearchLogic {

	
	private Network network;
	
	private static final double ALPHA = 0.5;
	private static final double BETA = 0.5;
	
	private Map<Id<Link>,Double> lastTimeOnLink;   
	
	/**
	 * @param network 
	 * 
	 */
	public UtilityBasedParkingSearchLogic(Network network) {
		this.network = network;
		this.lastTimeOnLink = new HashMap<Id<Link>,Double>();
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId) {
		throw new RuntimeException("shouldn't happen");
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Link> endLinkId, double timeOfDay) {
		double highestUtility = Double.MIN_VALUE;
		double currentUtility;
		double lastEnterTime; 
		Id<Link> nextLinkId = null;
		
		Set<Id<Link>> outLinks = this.network.getLinks().get(currentLinkId).getToNode().getOutLinks().keySet();
		
		for(Id<Link> ll : outLinks){
			double distanceToDest = NetworkUtils.getEuclideanDistance(network.getLinks().get(ll).getCoord(), network.getLinks().get(endLinkId).getCoord());
			if (lastTimeOnLink.containsKey(ll)){
				lastEnterTime = lastTimeOnLink.get(ll);	
			}
			else{
				lastEnterTime = 0;
			}
			double timeSinceVisit = timeOfDay - lastEnterTime; 
			currentUtility = ALPHA * distanceToDest + BETA * timeSinceVisit;
			if (currentUtility > highestUtility){
				highestUtility = currentUtility;
				nextLinkId = ll;
			}
		}
		
		if(nextLinkId == null){
			throw new RuntimeException("couldn't make out a new link to drive on. current link: " + currentLinkId);
		}
		
		return nextLinkId;
		
	}

}
