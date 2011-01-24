/**
 * 
 */
package playground.mzilske.freight.vrp;

import org.apache.log4j.Logger;

/**
 * @author schroeder
 *
 */
public class CapacityConstraint implements Constraints {
	private static Logger logger = Logger.getLogger(CapacityConstraint.class);
	public Double capacity = 16.0;
	
	public CapacityConstraint(){
		
	}
	
	public CapacityConstraint(double capacity){
		this.capacity = capacity;
	}
	
	/* (non-Javadoc)
	 * @see core.basic.Contraints#tourDoesNotViolateConstraints(core.basic.Tour)
	 */
	
	@Override
	public boolean tourDoesNotViolateConstraints(VehicleTour newTour, Costs costs) {
		int demand = 0;
		for(Node node : newTour.getNodes()){
			demand += node.getDemand();
		}
		if(demand <= capacity){
			return true;
		}
		else{
			return false;
		}
		
	}

}
