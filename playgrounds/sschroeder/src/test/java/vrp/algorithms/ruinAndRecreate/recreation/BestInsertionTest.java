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
import vrp.api.VRP;
import vrp.basics.InitialSolutionFactoryImpl;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.VRPBuilder;
import vrp.basics.VRPWithMultipleDepotsAndVehiclesImpl;
import vrp.basics.VehicleType;

public class BestInsertionTest extends VRPTestCase{
	BestInsertion bestInsertion;
	
	VRP vrp;
	
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
		init();
		iniAlgo();
		depot1 = customerMap.get(makeId(0,0));
		depot1.setDemand(0);
		depot2 = customerMap.get(makeId(10,0));
		depot2.setDemand(0);
		VRPBuilder vrpBuilder = new VRPBuilder();
		vrpBuilder.addCustomer(depot1, true);
		vrpBuilder.addCustomer(depot2, true);
		
		vrpBuilder.addCustomer(customerMap.get(makeId(0,10)), false);
		vrpBuilder.addCustomer(customerMap.get(makeId(10,10)), false);
		c1 = customerMap.get(makeId(1,4));
		vrpBuilder.addCustomer(c1, false);
		c2 = customerMap.get(makeId(1,5));
		vrpBuilder.addCustomer(c2, false);
		setRelation(c1,c2);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		vrpBuilder.assignVehicleType(depot1.getId(), new VehicleType(2));
		vrpBuilder.assignVehicleType(depot2.getId(), new VehicleType(2));
		vrp = vrpBuilder.buildVRP();
		
		makeIniSolution();
		
		shipmentWithoutService = makeShipment();
	}
	
	

	private List<Shipment> makeShipment() {
		List<Shipment> shipments = new ArrayList<Shipment>();
		shipments.add(new Shipment(c2,c1));
		return shipments;
	}

	private void makeIniSolution() {
		Collection<Customer> tourSequence = new ArrayList<Customer>();
		tourSequence.add(depot1);
		tourSequence.add(customerMap.get(makeId(0,10)));
		tourSequence.add(depot1);
		Tour tour1 = makeTour(tourSequence);
		VehicleType type1 = vrp.getVehicleType(depot1.getId());
		tourAgent1 = getTourAgent(vrp, tour1, type1);
		
		Collection<Customer> anotherTourSequence = new ArrayList<Customer>();
		anotherTourSequence.add(depot2);
		anotherTourSequence.add(customerMap.get(makeId(10,10)));
		anotherTourSequence.add(depot2);
		Tour tour2 = makeTour(anotherTourSequence);
		VehicleType type2 = vrp.getVehicleType(depot2.getId());
		tourAgent2 = getTourAgent(vrp, tour2,type2);
		
		Collection<TourAgent> agents = new ArrayList<TourAgent>();
		agents.add(tourAgent1);
		agents.add(tourAgent2);
		solution = new Solution(agents);
	}


	private void iniAlgo() {
		bestInsertion = new BestInsertion(vrp);
		bestInsertion.setInitialSolutionFactory(new InitialSolutionFactoryImpl());
		bestInsertion.setTourAgentFactory(new RRTourAgentFactory(vrp));
		
	}

	public void testSizeOfNewSolution(){
		bestInsertion.run(solution, shipmentWithoutService);
		assertEquals(2, solution.getTourAgents().size());
		assertEquals(5, tourAgent1.getTourSize());
		assertEquals(3, tourAgent2.getTourSize());
	}
	
	public void testCustomerSequenceOfInsertion(){
		bestInsertion.run(solution, shipmentWithoutService);
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent1.getTourActivities());
		assertEquals(c2,acts.get(2).getCustomer());
		assertEquals(c1,acts.get(3).getCustomer());
	}
	
	public void testSolutionSizesWhenChangingVehicleType(){
		((VRPWithMultipleDepotsAndVehiclesImpl)vrp).assignVehicleType(depot1.getId(), new VehicleType(1));
		c1.setDemand(-2);
		c2.setDemand(2);
		makeIniSolution();
		bestInsertion.run(solution, shipmentWithoutService);
		assertEquals(2, solution.getTourAgents().size());
		assertEquals(3, tourAgent1.getTourSize());
		assertEquals(5, tourAgent2.getTourSize());
	}
	
	public void testSequenceOfSolutionWhenChangingVehicleType(){
		((VRPWithMultipleDepotsAndVehiclesImpl)vrp).assignVehicleType(depot1.getId(), new VehicleType(1));
		c1.setDemand(-2);
		c2.setDemand(2);
		makeIniSolution();
		bestInsertion.run(solution, shipmentWithoutService);
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent2.getTourActivities());
		assertEquals(c2,acts.get(1).getCustomer());
		assertEquals(c1,acts.get(2).getCustomer());
	}
	
}
