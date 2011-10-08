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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.SingleDepotVRP;

public class SingleDepotVRPImpl implements SingleDepotVRP{

	private static Logger logger = Logger.getLogger(SingleDepotVRPImpl.class);
	
	private Costs costs;
	
	private Constraints constraints;
	
	private Map<String,Customer> customers;
	
	private String depot;
	
	private VehicleType vehicleType;
	
	public SingleDepotVRPImpl(String depot, VehicleType vehicleType, Collection<Customer> customers, Costs costs, Constraints constraints) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		mapCustomers(customers);
		this.vehicleType = vehicleType;
		this.depot = depot;
	}
	
	@Override
	public VehicleType getVehicleType(){
		return vehicleType;
	}
	
	private void mapCustomers(Collection<Customer> customers) {
		this.customers = new HashMap<String, Customer>();
		for(Customer customer : customers){
			this.customers.put(customer.getId(), customer);
		}
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
	public Customer getDepot() {
		return customers.get(depot);
	}

	@Override
	public Map<String, Customer> getCustomers() {
		return Collections.unmodifiableMap(customers);
	}
}
