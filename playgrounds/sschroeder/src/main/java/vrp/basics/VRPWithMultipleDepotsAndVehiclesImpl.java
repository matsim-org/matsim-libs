package vrp.basics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;

public class VRPWithMultipleDepotsAndVehiclesImpl implements VRP{

	private static Logger logger = Logger.getLogger(VRPWithMultipleDepotsAndVehiclesImpl.class);
	
	private Costs costs;
	
	private Constraints constraints;
	
	private Map<Id,Customer> customers;
	
	private Map<Id,Customer> depots;
	
	private Map<Id,VehicleType> vehicleTypes;
	
	public VRPWithMultipleDepotsAndVehiclesImpl(Collection<Id> depots, Collection<Customer> customers, Costs costs, Constraints constraints) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		mapCustomers(customers);
		mapDepots(depots);
		vehicleTypes = new HashMap<Id,VehicleType>();
	}
	

	public void assignVehicleType(Id depotId, VehicleType vehicleType){
		if(depots.containsKey(depotId)){
			vehicleTypes.put(depotId, vehicleType);
		}
		else{
			logger.warn("cannot assign vehicleType, since depot " + depotId + " does not exist");
		}
	}
	
	public VehicleType getVehicleType(Id depotId){
		return vehicleTypes.get(depotId);
	}

	private void mapDepots(Collection<Id> depots) {
		this.depots = new HashMap<Id,Customer>();
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
