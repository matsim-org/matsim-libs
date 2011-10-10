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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import vrp.api.Customer;
import vrp.api.SingleDepotVRP;

public class SingleDepotInitialSolutionFactoryImpl implements SingleDepotInitialSolutionFactory{

	private static Logger logger = Logger.getLogger(SingleDepotInitialSolutionFactoryImpl.class);
	
	@Override
	public Collection<Tour> createInitialSolution(SingleDepotVRP vrp) {
		logger.info("create initial solution");
		Collection<Tour> tours = new ArrayList<Tour>();
		Set<String> customersWithService = new HashSet<String>();
		for(Customer customer : vrp.getCustomers().values()){
			if(customersWithService.contains(customer.getId())){
				continue;
			}
			if(vrp.getDepot().getId().equals(customer.getId())){
				continue;
			}
			Customer depot = vrp.getDepot();
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
		logger.info("done");
		return tours;
	}
	
	@Override
	public Tour createRoundTour(SingleDepotVRP vrp, Customer from, Customer to){
		Tour tour = null;
		Customer depot = vrp.getDepot();
		if(vrp.getDepot().getId().equals(from.getId())){
			tour = VrpUtils.createRoundTour(depot, to);
		}
		else if(vrp.getDepot().getId().equals(to.getId())){
			tour = VrpUtils.createRoundTour(depot, from);
		}
		else {
			tour = VrpUtils.createRoundTour(depot, from, to);
		}
		return tour;
	}
	
	@Override
	public Vehicle createVehicle(SingleDepotVRP vrp, Tour tour) {
		if(tour.getActivities().size()<1){
			throw new IllegalStateException("number of tourActivities smaller than 1");
		}
		String depotId = tour.getActivities().get(0).getCustomer().getId();
		assertNotNull(depotId);
		Vehicle vehicle = VrpUtils.createVehicle(vrp.getVehicleType());
		assertNotNull(vehicle);
		return vehicle;
	}

	private void assertNotNull(Vehicle vehicle) {
		if(vehicle == null){
			throw new IllegalStateException("vehicle is null. this cannot be");
		}
	}

	private void assertNotNull(String depotId) {
		if(depotId == null){
			throw new IllegalStateException("id null. this cannot be.");
		}
	}

}
