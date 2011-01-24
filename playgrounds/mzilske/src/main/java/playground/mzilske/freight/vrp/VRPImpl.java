/**
 * 
 */
package playground.mzilske.freight.vrp;

import java.util.Map;
import java.util.Properties;

import org.matsim.api.core.v01.Id;

/**
 * @author stefan
 *
 */
public class VRPImpl implements VRP {

	private Id depotId;
	
	private Nodes nodes;
	
	private Costs costs;
	
	private VRPSolution solution = null;
	
	private Constraints constraints;
	
	private String cost2optimize;
	
	private Properties properties = new Properties();
	
	public VRPImpl(Id depotId, Nodes nodes, Costs costs, Constraints constraints){
		this.depotId = depotId;
		this.nodes = nodes;
		this.costs = costs;
		this.constraints = constraints;
	}
	
	public Properties getProperties() {
		return properties;
	}

	@Override
	public Id getDepotId() {
		return depotId;
	}

	@Override
	public Constraints getConstraints() {
		return constraints;
	}

	@Override
	public Costs getCosts() {
		return costs;
	}

	@Override
	public void setSolution(VRPSolution solution) {
		this.solution = solution;
	}

	@Override
	public VRPSolution getSolution() {
		return solution;
	}

	@Override
	public Map<Id, Node> getNodes() {
		return nodes.getNodes();
	}

	
}
