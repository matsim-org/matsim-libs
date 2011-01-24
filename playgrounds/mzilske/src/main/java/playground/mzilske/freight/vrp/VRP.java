/**
 * 
 */
package playground.mzilske.freight.vrp;


import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author stscr
 *
 */
public interface VRP { //vehicle routing problem
	public Id getDepotId();	
	public Constraints getConstraints();
	public Costs getCosts();
	public void setSolution(VRPSolution solution);
	public VRPSolution getSolution();
	public Map<Id,Node> getNodes();
	
}
