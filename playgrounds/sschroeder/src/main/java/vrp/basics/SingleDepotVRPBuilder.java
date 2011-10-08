/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
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
import vrp.api.SingleDepotVRP;

public class SingleDepotVRPBuilder {
	
	private Constraints constraints;
	
	private Costs costs;
	
	private Customer depot;
	
	private VehicleType vehicleType;
	
	private Collection<Customer> customers = new ArrayList<Customer>();
	
	private NodeFactory nodeFactory = new NodeFactory();
	
	public NodeFactory getNodeFactory() {
		return nodeFactory;
	}

	public Customer createAndAddCustomer(String id, Node node, int demand, double start, double end, double serviceTime){
		Customer customer = createCustomer(id, node, demand, start, end, serviceTime);
		addCustomer(customer);
		return customer;
	}
	
	public void setDepot(Customer customer){
		if(!customers.contains(customer)){
			throw new IllegalStateException(customer.getId() + " is not in customer list. add customer with addCustomer() first and then set it as depot");
		}
		depot = customer;
	}
	
	public void addCustomer(Customer customer){
		customers.add(customer);
	}
	
	public void setVehicleType(VehicleType vehicleType){
		this.vehicleType = vehicleType;
	}
	
	public void setVehicleType(int capacity){
		this.vehicleType = new VehicleType(capacity);
	}
	
	private Customer createCustomer(String id, Node node, int demand, double start, double end, double serviceTime){
		Customer customer = new CustomerImpl(id, node);
		customer.setDemand(demand);
		customer.setServiceTime(serviceTime);
		customer.setTheoreticalTimeWindow(start, end);
		return customer;
	}
	
	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	public void setCosts(Costs costs){
		this.costs = costs;
	}
	
	public SingleDepotVRP buildVRP(){
		verify();
		SingleDepotVRP vrp = new SingleDepotVRPImpl(depot.getId(),vehicleType, customers, costs, constraints);
		return vrp;
	}

	private void verify() {
		if(depot == null){
			throw new IllegalStateException("depot not set");
		}
		if(costs == null){
			throw new IllegalStateException("costs not set");
		}
		if(constraints == null){
			throw new IllegalStateException("constraints not set");
		}
		if(vehicleType == null){
			throw new IllegalStateException("vehicleType not set");
		}
	}
}
