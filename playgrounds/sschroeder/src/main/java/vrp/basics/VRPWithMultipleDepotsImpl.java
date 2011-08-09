package vrp.basics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;

public class VRPWithMultipleDepotsImpl implements VRP{

	private Costs costs;
	
	private Constraints constraints;
	
	private Map<Id,Customer> customers;
	
	private Map<Id,Customer> depots;
	
	public VRPWithMultipleDepotsImpl(Collection<Id> depots, Collection<Customer> customers, Costs costs, Constraints constraints) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		mapCustomers(customers);
		mapDepots(depots);
	}

	private void mapDepots(Collection<Id> depots) {
		for(Id id : depots){
			if(customers.containsKey(id)){
				this.depots.put(id, customers.get(id));
			}
			else{
				throw new IllegalStateException("depot not in customerList which contains all customers inclusive depots");
			}
		}
	}

	@Override
	public Id getDepotId() {
		return null;
	}
	
	private void mapCustomers(Collection<Customer> customers) {
		this.customers = new HashMap<Id, Customer>();
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
		return null;
	}

	@Override
	public Map<Id, Customer> getCustomers() {
		return customers;
	}

	@Override
	public Map<Id, Customer> getDepots() {
		return depots;
	}

}
