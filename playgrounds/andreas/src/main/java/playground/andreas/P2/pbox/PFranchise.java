package playground.andreas.P2.pbox;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

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
	
	
	private Map<Id, Set<Id>> startToEndStopIdMap = new HashMap<Id, Set<Id>>();

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
		
		Id startStopId = plan.getStartStop().getId();
		Id endStopId = plan.getEndStop().getId();
		
		// no record for start stop - allow
		if(this.startToEndStopIdMap.get(startStopId) == null){
			this.startToEndStopIdMap.put(startStopId, new TreeSet<Id>());
			this.startToEndStopIdMap.get(startStopId).add(endStopId);
			return false;
		}
		
		// record available for start stop but wrong end stop - allow
		if(!this.startToEndStopIdMap.get(startStopId).contains(endStopId)){
			this.startToEndStopIdMap.get(startStopId).add(endStopId);
			return false;
		}
		
		return true;
	}

	/**
	 * Reset all start and end stops to the stops currently in use
	 * 
	 * @param cooperatives
	 */
	public void reset(LinkedList<Cooperative> cooperatives) {
		this.startToEndStopIdMap = new HashMap<Id, Set<Id>>();
		
		for (Cooperative cooperative : cooperatives) {
			for (PPlan plan : cooperative.getAllPlans()) {
				Id startStopId = plan.getStartStop().getId();
				Id endStopId = plan.getEndStop().getId();
				if(this.startToEndStopIdMap.get(startStopId) == null){
					this.startToEndStopIdMap.put(startStopId, new TreeSet<Id>());
				}
				this.startToEndStopIdMap.get(startStopId).add(endStopId);
			}
		}
		
	}
}
