package vrp.basics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;


import vrp.api.Customer;
import vrp.api.VRP;

public class TrivialInitialSolutionFactory implements InitialSolutionFactory{


	@Override
	public Collection<Tour> createInitialSolution(VRP vrp) {
		Collection<Tour> tours = new ArrayList<Tour>();
		Customer depot = vrp.getDepot();
		Set<Id> customersWithService = new HashSet<Id>();
		for(Customer customer : vrp.getCustomers().values()){
			if(customersWithService.contains(customer.getId())){
				continue;
			}
			if(depot != customer){
				if(customer.hasRelation()){
					Customer relatedCustomer = customer.getRelation().getCustomer();
					Tour tour = null;
					if(relatedCustomer.getDemand() < 0){
						tour = VrpUtils.createRoundTour(depot, customer, relatedCustomer);
					}
					else{
						tour = VrpUtils.createRoundTour(depot, relatedCustomer, customer);
					}
					customersWithService.add(customer.getId());
					customersWithService.add(relatedCustomer.getId());
					tours.add(tour);
				}
				else{
					Tour tour = VrpUtils.createRoundTour(depot, customer);
					customersWithService.add(customer.getId());
					tours.add(tour);
				}
			}
		}
		return tours;
	}

	@Override
	public Tour createRoundTour(VRP vrp, Customer from, Customer to) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vehicle createVehicle(VRP vrp, Tour tour) {
		// TODO Auto-generated method stub
		return null;
	}

}
