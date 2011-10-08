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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.MultipleDepotsVRP;
import vrp.api.Node;
import vrp.api.VRP;

public class MultipleDepotVRPBuilder {
	
	private Constraints constraints = new Constraints(){

		@Override
		public boolean judge(Tour tour, Vehicle vehicle) {
			return true;
		}
		
	};
	
	private Costs costs = new CrowFlyDistance();
	
	private List<String> depots = new ArrayList<String>();
	
	private Collection<Customer> customers = new ArrayList<Customer>();
	
	private Map<String,VehicleType> types = new HashMap<String, VehicleType>();

	public Customer createAndAddCustomer(String id, Node node, int demand, double start, double end, double serviceTime, boolean isDepot){
		Customer customer = createCustomer(id, node, demand, start, end, serviceTime);
		addCustomer(customer,isDepot);
		return customer;
	}
	
	public void addCustomer(Customer customer, boolean isDepot){
		customers.add(customer);
		if(isDepot){
			depots.add(customer.getId());
		}
	}
	
	public void assignVehicleType(String depotId, VehicleType vehicleType){
		types.put(depotId, vehicleType);
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
	
	public VRP buildVRP(){
		verify();
		VRPWithMultipleDepotsImpl vrp = new VRPWithMultipleDepotsImpl(depots, customers, costs, constraints);
		for(String id : types.keySet()){
			vrp.assignVehicleType(id, types.get(id));
		}
		assertEachDepotHasVehicleType(vrp);
		return vrp;
	}
	
	private void assertEachDepotHasVehicleType(MultipleDepotsVRP vrp) {
		for(String id : depots){
			VehicleType type = vrp.getVehicleType(id);
			if(type == null){
				throw new IllegalStateException("each depot must have one vehicleType. Depot " + id + " does not have!");
			}
		}
		
	}

	private void verify() {
		if(depots.isEmpty()){
			throw new IllegalStateException("at least one depot must be set");
		}
	}
}
