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
package vrp.algorithms.ruinAndRecreate.recreation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import vrp.VRPTestCase;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.api.Customer;
import vrp.api.SingleDepotVRP;
import vrp.basics.SingleDepotSolutionFactoryImpl;
import vrp.basics.SingleDepotVRPBuilder;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.VehicleType;

public class BestInsertionTest extends VRPTestCase{
	BestInsertion bestInsertion;
	
	SingleDepotVRP vrp;
	
	Customer depot1;
	
	Customer depot2; 
	
	Customer c1;
	
	Customer c2; 
	
	TourAgent tourAgent1;
	
	TourAgent tourAgent2;
	
	Solution solution;
	
	List<Shipment> shipmentWithoutService;
	
	@Override
	public void setUp(){
//		vrp = getVRP(2);
		
		initCustomersInPlainCoordinateSystem();

		buildVRP(2);
		
		makeSolutionWithoutC1andC2();
		
		bestInsertion = new BestInsertion(vrp);
		bestInsertion.setInitialSolutionFactory(new SingleDepotSolutionFactoryImpl());
		bestInsertion.setTourAgentFactory(new RRTourAgentFactory(vrp));
		
		shipmentWithoutService = new ArrayList<Shipment>();
		shipmentWithoutService.add(makeShipmentFromC2ToC1());
	}



	private void buildVRP(int capacity) {
		depot1 = customerMap.get(makeId(0,0));
		depot1.setDemand(0);
		SingleDepotVRPBuilder vrpBuilder = new SingleDepotVRPBuilder();
		vrpBuilder.addCustomer(depot1);
		vrpBuilder.setDepot(depot1);
		
		vrpBuilder.addCustomer(customerMap.get(makeId(0,10)));
		vrpBuilder.addCustomer(customerMap.get(makeId(10,10)));
		c1 = customerMap.get(makeId(1,4));
		vrpBuilder.addCustomer(c1);
		c2 = customerMap.get(makeId(1,5));
		vrpBuilder.addCustomer(c2);
		setRelation(c1,c2);
		
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		vrpBuilder.setVehicleType(new VehicleType(capacity));
		vrp = vrpBuilder.buildVRP();
	}
	
	

	public void testSizeOfNewSolution(){
		bestInsertion.run(solution, shipmentWithoutService);
		assertEquals(2, solution.getTourAgents().size());
	}
	
	public void testActivitiesOfFirstTourInSolution(){
		bestInsertion.run(solution, shipmentWithoutService);
		assertEquals(3, tourAgent1.getTourSize());
	}
	
	public void testActivitiesOfSecondTourInSolution(){
		bestInsertion.run(solution, shipmentWithoutService);
		assertEquals(5, tourAgent2.getTourSize());
	}
	
	public void testCustomerSequenceOfInsertion(){
		bestInsertion.run(solution, shipmentWithoutService);
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent2.getTourActivities());
		assertEquals(c2,acts.get(1).getCustomer());
		assertEquals(c1,acts.get(3).getCustomer());
	}
	
	public void testSolutionSizesWhenChangingVehicleType(){
		vrp = getVRP(1);
		bestInsertion.run(solution, shipmentWithoutService);
		assertEquals(2, solution.getTourAgents().size());
	}
	
	public void testCustomerSequenceOfInsertionAfterChangingVehicleType(){
		buildVRP(1);
		makeSolutionWithoutC1andC2();
		shipmentWithoutService = new ArrayList<Shipment>();
		shipmentWithoutService.add(makeShipmentFromC2ToC1());
		bestInsertion.run(solution, shipmentWithoutService);
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent2.getTourActivities());
		assertEquals(c2,acts.get(1).getCustomer());
		assertEquals(c1,acts.get(2).getCustomer());
	}
	
//	public void testSmth(){
//		c1.setDemand(-2);
//		c2.setDemand(2);
//		makeSolutionWithoutC1andC2();
//		bestInsertion.run(solution, shipmentWithoutService);
//		assertEquals(2, solution.getTourAgents().size());
//		assertEquals(3, tourAgent1.getTourSize());
//		assertEquals(5, tourAgent2.getTourSize());
//	}
//	
//	public void testSequenceOfSolutionWhenChangingVehicleType(){
//		((VRPWithMultipleDepotsImpl)vrp).assignVehicleType(depot1.getId(), new VehicleType(1));
//		c1.setDemand(-2);
//		c2.setDemand(2);
//		makeSolutionWithoutC1andC2();
//		bestInsertion.run(solution, shipmentWithoutService);
//		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent2.getTourActivities());
//		assertEquals(c2,acts.get(1).getCustomer());
//		assertEquals(c1,acts.get(2).getCustomer());
//	}



	private void makeSolutionWithoutC1andC2() {
		Collection<Customer> tourSequence = new ArrayList<Customer>();
		tourSequence.add(depot1);
		tourSequence.add(customerMap.get(makeId(0,10)));
		tourSequence.add(depot1);
		Tour tour1 = makeTour(tourSequence);
		VehicleType type1 = vrp.getVehicleType();
		tourAgent1 = getTourAgent(vrp, tour1, type1);
		
		Collection<Customer> anotherTourSequence = new ArrayList<Customer>();
		anotherTourSequence.add(depot1);
		anotherTourSequence.add(customerMap.get(makeId(10,10)));
		anotherTourSequence.add(depot1);
		Tour tour2 = makeTour(anotherTourSequence);
		tourAgent2 = getTourAgent(vrp, tour2, type1);
		
		Collection<TourAgent> agents = new ArrayList<TourAgent>();
		agents.add(tourAgent1);
		agents.add(tourAgent2);
		solution = new Solution(agents);
	}



	private Shipment makeShipmentFromC2ToC1() {
		return new Shipment(c2,c1);
	}
	
}
