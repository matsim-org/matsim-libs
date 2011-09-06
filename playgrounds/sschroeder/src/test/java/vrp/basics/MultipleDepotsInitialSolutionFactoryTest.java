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

import java.util.Collection;

import vrp.VRPTestCase;
import vrp.api.Customer;
import vrp.api.VRP;

public class MultipleDepotsInitialSolutionFactoryTest extends VRPTestCase {
	
	VRP vrp;
	
	Customer depot1;
	
	Customer depot2; 
	
	public void setUp(){
		init();
		depot1 = customerMap.get(makeId(10,0));
		depot1.setDemand(0);
		depot2 = customerMap.get(makeId(0,0));
		depot2.setDemand(0);
		
	}

	public void testSizeOfInitialSolution(){
		VRPBuilder vrpBuilder = new VRPBuilder();
		vrpBuilder.addCustomer(depot1, true);
		vrpBuilder.addCustomer(depot2, true);
		vrpBuilder.addCustomer(customerMap.get(makeId(0,10)), false);
		vrpBuilder.addCustomer(customerMap.get(makeId(10,10)), false);
		vrpBuilder.setCosts(costs);
		vrpBuilder.assignVehicleType(depot1.getId(), new VehicleType(20));
		vrpBuilder.assignVehicleType(depot2.getId(), new VehicleType(20));
		VRP vrp = vrpBuilder.buildVRP();
		Collection<Tour> tours = new MultipleDepotsInitialSolutionFactory().createInitialSolution(vrp);
		assertEquals(2, tours.size());
	}
	
	public void testDepotAssignmentOfInitialSolution(){
		VRPBuilder vrpBuilder = new VRPBuilder();
		vrpBuilder.addCustomer(depot1, true);
		vrpBuilder.addCustomer(depot2, true);
		vrpBuilder.addCustomer(customerMap.get(makeId(0,10)), false);
		vrpBuilder.addCustomer(customerMap.get(makeId(10,10)), false);
		vrpBuilder.assignVehicleType(depot1.getId(), new VehicleType(20));
		vrpBuilder.assignVehicleType(depot2.getId(), new VehicleType(20));
		vrpBuilder.setCosts(costs);
		VRP vrp = vrpBuilder.buildVRP();
		
		Collection<Tour> tours = new MultipleDepotsInitialSolutionFactory().createInitialSolution(vrp);
		for(Tour tour : tours){
			assertEquals(3, tour.getActivities().size());
			if(tour.getActivities().get(1).getCustomer() == customerMap.get(makeId(0,10))){
				assertEquals(customerMap.get(makeId(0,0)), tour.getActivities().get(0).getCustomer());
				assertEquals(customerMap.get(makeId(0,0)), tour.getActivities().get(2).getCustomer());
			}
			else{
				assertEquals(customerMap.get(makeId(10,0)), tour.getActivities().get(0).getCustomer());
				assertEquals(customerMap.get(makeId(10,0)), tour.getActivities().get(2).getCustomer());
			}
		}
		assertEquals(2, tours.size());
	}
	
	public void testSizeOfIniSolutionWithCustomerRelations(){
		Customer customer1 = customerMap.get(makeId(10,10));
		Customer customer2 = customerMap.get(makeId(10,9));
		customer1.setRelation(new Relation(customer2));
		customer2.setRelation(new Relation(customer1));
		
		VRPBuilder vrpBuilder = new VRPBuilder();
		vrpBuilder.addCustomer(depot1, true);
		vrpBuilder.addCustomer(depot2, true);
		vrpBuilder.addCustomer(customer1, false);
		vrpBuilder.addCustomer(customer2, false);
		vrpBuilder.assignVehicleType(depot1.getId(), new VehicleType(20));
		vrpBuilder.assignVehicleType(depot2.getId(), new VehicleType(20));
		vrpBuilder.setCosts(costs);
		VRP vrp = vrpBuilder.buildVRP();
		
		Collection<Tour> tours = new MultipleDepotsInitialSolutionFactory().createInitialSolution(vrp);
		assertEquals(1, tours.size());
	}
	
	public void testDepotAssignmentOfIniSolutionWithCustomerRelations(){
		Customer customer1 = customerMap.get(makeId(10,10));
		Customer customer2 = customerMap.get(makeId(10,9));
		customer1.setRelation(new Relation(customer2));
		customer2.setRelation(new Relation(customer1));
		
		VRPBuilder vrpBuilder = new VRPBuilder();
		vrpBuilder.addCustomer(depot1, true);
		vrpBuilder.addCustomer(depot2, true);
		vrpBuilder.addCustomer(customer1, false);
		vrpBuilder.addCustomer(customer2, false);
		vrpBuilder.assignVehicleType(depot1.getId(), new VehicleType(20));
		vrpBuilder.assignVehicleType(depot2.getId(), new VehicleType(20));
		vrpBuilder.setCosts(costs);
		VRP vrp = vrpBuilder.buildVRP();
		
		Collection<Tour> tours = new MultipleDepotsInitialSolutionFactory().createInitialSolution(vrp);
		Tour tour = tours.iterator().next();
		assertEquals(tour.getActivities().get(0).getCustomer(), depot1);
		assertEquals(tour.getActivities().get(1).getCustomer(), customer1);
		assertEquals(tour.getActivities().get(2).getCustomer(), customer2);
		assertEquals(tour.getActivities().get(3).getCustomer(), depot1);
	}
	
	public void testDepotAssignmentOfIniSolutionWithCustomerRelations2(){
		Customer customer1 = customerMap.get(makeId(0,10));
		Customer customer2 = customerMap.get(makeId(0,9));
		customer1.setRelation(new Relation(customer2));
		customer2.setRelation(new Relation(customer1));
		
		VRPBuilder vrpBuilder = new VRPBuilder();
		vrpBuilder.addCustomer(depot1, true);
		vrpBuilder.addCustomer(depot2, true);
		vrpBuilder.addCustomer(customer1, false);
		vrpBuilder.addCustomer(customer2, false);
		vrpBuilder.assignVehicleType(depot1.getId(), new VehicleType(20));
		vrpBuilder.assignVehicleType(depot2.getId(), new VehicleType(20));
		vrpBuilder.setCosts(costs);
		VRP vrp = vrpBuilder.buildVRP();
		
		Collection<Tour> tours = new MultipleDepotsInitialSolutionFactory().createInitialSolution(vrp);
		Tour tour = tours.iterator().next();
		assertEquals(tour.getActivities().get(0).getCustomer(), depot2);
		assertEquals(tour.getActivities().get(1).getCustomer(), customer1);
		assertEquals(tour.getActivities().get(2).getCustomer(), customer2);
		assertEquals(tour.getActivities().get(3).getCustomer(), depot2);
	}
}
