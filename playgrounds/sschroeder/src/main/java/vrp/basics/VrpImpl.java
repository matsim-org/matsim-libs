package vrp.basics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;


/**
 * 
 * @author stefan schroeder
 *
 */

public class VrpImpl implements VRP{
	
	private Id depotId;
	
	private Costs costs;
	
	private Constraints constraints;
	
	private Map<Id,Customer> customers;
	
	private Map<Id,Customer> depots;
	
	public VrpImpl(Id depotId, Collection<Customer> customers, Costs costs, Constraints constraints){
		this.depotId = depotId;
		this.costs = costs;
		this.constraints = constraints;
		mapCustomers(customers);
		depots = new HashMap<Id, Customer>();
		depots.put(depotId, getDepot());
	}
	
	private void mapCustomers(Collection<Customer> customers) {
		this.customers = new HashMap<Id, Customer>();
		for(Customer customer : customers){
			this.customers.put(customer.getId(), customer);
		}
	}

	public Id getDepotId() {
		return depotId;
	}

	public Constraints getConstraints() {
		return constraints;
	}

	public Costs getCosts() {
		return costs;
	}

	public Customer getDepot() {
		return customers.get(depotId);
	}

	public Map<Id, Customer> getCustomers() {
		return customers;
	}

	@Override
	public Map<Id, Customer> getDepots() {
		return depots;
	}

	@Override
	public VehicleType getVehicleType(Id depotId) {
		// TODO Auto-generated method stub
		return null;
	}

}
