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
import java.util.HashSet;
import java.util.Set;

import vrp.api.Customer;
import vrp.api.VRP;

public class TrivialInitialSolutionFactory implements InitialSolutionFactory{


	@Override
	public Collection<Tour> createInitialSolution(VRP vrp) {
		Collection<Tour> tours = new ArrayList<Tour>();
		Customer depot = vrp.getDepot();
		Set<String> customersWithService = new HashSet<String>();
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
