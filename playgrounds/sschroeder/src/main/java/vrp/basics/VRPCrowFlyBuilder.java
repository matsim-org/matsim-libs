package vrp.basics;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.VRP;


/**
 * 
 * @author stefan schroeder
 *
 */

public class VRPCrowFlyBuilder {
	
	private Id depotId;
	
	private boolean depotSet = false;
	
	private Constraints constraints;
	
	private Collection<Customer> customers = new ArrayList<Customer>();
	
	public void setDepot(Id depotId){
		this.depotId = depotId;
		depotSet = true;
	}
	
	public void setConstraints(Constraints constraint){
		this.constraints = constraint;
	}

	public Customer createAndAddCustomerWithTimeWindows(Id id, Coord coord, int demand, double start, double end, double serviceTime){
		Node node = makeNode(id,coord);
		Customer customer = VrpUtils.createCustomer(id, node, demand, start, end, serviceTime);
		customers.add(customer);
		return customer;
	}
	
	private Node makeNode(Id id, Coord coord) {
		Node node = new NodeImpl(id);
		node.setCoord(coord);
		return node;
	}

	public VRP build(){
		if(!depotSet){
			throw new RuntimeException("depot not set");
		}
		Costs costs = new CrowFlyDistance();
		VRP vrp = new VrpImpl(depotId, customers, costs, constraints);
		return vrp;
	}


}
