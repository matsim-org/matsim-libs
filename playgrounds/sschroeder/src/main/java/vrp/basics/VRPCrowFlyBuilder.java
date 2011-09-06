/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.basics;

import java.util.ArrayList;
import java.util.Collection;

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
	
	private String depotId;
	
	private boolean depotSet = false;
	
	private Constraints constraints;
	
	private Collection<Customer> customers = new ArrayList<Customer>();
	
	public void setDepot(String depotId){
		this.depotId = depotId;
		depotSet = true;
	}
	
	public void setConstraints(Constraints constraint){
		this.constraints = constraint;
	}

	public Customer createAndAddCustomerWithTimeWindows(String id, Coordinate coord, int demand, double start, double end, double serviceTime){
		Node node = makeNode(id,coord);
		Customer customer = VrpUtils.createCustomer(id, node, demand, start, end, serviceTime);
		customers.add(customer);
		return customer;
	}
	
	private Node makeNode(String id, Coordinate coord) {
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
