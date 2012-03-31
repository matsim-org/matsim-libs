package playground.andreas.P2.pbox;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.plan.PPlan;

/**
 * Simple Franchise system rejecting all routes already operated
 * 
 * @author aneumann
 *
 */
public class PFranchise {

	private final static Logger log = Logger.getLogger(PFranchise.class);
	
	private final boolean activated;
	private TreeSet<String> routeHashes = new TreeSet<String>();
	
	public PFranchise(boolean useFranchise) {
		this.activated = useFranchise;
		if(this.activated){
			log.info("Franchise system activated");
		} else{
			log.info("Franchise system NOT activated");
		}
	}

	public boolean planRejected(PPlan plan) {
		
		if(!this.activated){
			return false;
		}
		
		String routeHash = generateRouteHash(plan.getStopsToBeServed());
		
		return this.routeHashes.contains(routeHash);
	}

	/**
	 * Reset all route hashes to the routes currently in use
	 * 
	 * @param cooperatives
	 */
	public void reset(LinkedList<Cooperative> cooperatives) {
		this.routeHashes = new TreeSet<String>();
		
		for (Cooperative cooperative : cooperatives) {
			for (PPlan plan : cooperative.getAllPlans()) {
				String routeHash = generateRouteHash(plan.getStopsToBeServed());
				if (this.routeHashes.contains(routeHash)) {
					log.warn("Cooperative " + cooperative.getId() + " with plan " + plan.getId() + " managed to circumvent the franchise system with route " + routeHash);
				}
				this.routeHashes.add(routeHash);
			}
		}		
	}
	
	/**
	 * Generates a unique String from the stops given
	 * 
	 * @param stopsToBeServed
	 * @return
	 */
	private String generateRouteHash(ArrayList<TransitStopFacility> stopsToBeServed){
		StringBuffer sB = new StringBuffer();
		for (TransitStopFacility transitStopFacility : stopsToBeServed) {
			sB.append(transitStopFacility.getId().toString()); sB.append("-");
		}
		return sB.toString();
	}
}
