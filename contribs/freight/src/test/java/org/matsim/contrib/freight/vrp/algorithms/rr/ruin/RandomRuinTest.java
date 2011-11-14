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
package org.matsim.contrib.freight.vrp.algorithms.rr.ruin;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.contrib.freight.vrp.algorithms.rr.VRPTestCase;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.TourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.Shipment;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.Solution;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RandomRuin;
import org.matsim.contrib.freight.vrp.api.Customer;
import org.matsim.contrib.freight.vrp.api.SingleDepotVRP;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.TourActivity;

public class RandomRuinTest extends VRPTestCase{

	SingleDepotVRP vrp;
	
	Solution solution;
	
	RandomRuin randomRuin;
	
	@Override
	public void setUp(){
		initCustomersInPlainCoordinateSystem();
		vrp = getVRP(2);
		solution = getInitialSolution(vrp);
		randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.5);
		RandomNumberGeneration.reset();
		/*
		 * fraction=0.5
		 * picks 0,10
		 * rem 0,10
		 * rem 1,5
		 */
	}
	
	public void testIniSolution(){
		assertEquals(3, solution.getTourAgents().size());
	}
	
	public void testSizeOfRuinedSolution(){
		randomRuin.run(solution);
		assertEquals(1, solution.getTourAgents().size());
	}
	
	public void testRemainingSolution(){
		randomRuin.run(solution);
		TourAgent tourAgent = solution.getTourAgents().iterator().next();
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent.getTourActivities());
		assertEquals(3, tourAgent.getTourActivities().size());
		assertEquals(customerMap.get(makeId(10,10)),acts.get(1).getCustomer());
	}
	
	public void testCustomerWithoutService(){
		randomRuin.run(solution);
		assertEquals(2,randomRuin.getShipmentsWithoutService().size());
	}
	
	public void testShipmentsWithoutService(){
		randomRuin.run(solution);
		Shipment s1 = randomRuin.getShipmentsWithoutService().get(0);
		Shipment s2 = randomRuin.getShipmentsWithoutService().get(1);
		assertEquals(vrp.getDepot(),s1.getTo());
		assertEquals(customerMap.get(makeId(0, 10)),s1.getFrom());
		
		assertEquals(customerMap.get(makeId(1, 4)),s2.getTo());
		assertEquals(customerMap.get(makeId(1, 5)),s2.getFrom());
	}
	
	public void testRuinedSolutionWithoutRelation(){
		removeRelations();
		randomRuin.run(solution);
		assertEquals(2, solution.getTourAgents().size());
	}

	private void removeRelations() {
		for(Customer c : customerMap.values()){
			if(c.hasRelation()){
				c.removeRelation();
			}
		}
	}
	
	public void testRemainingSolutionWithoutRelation(){
		removeRelations();
		randomRuin.run(solution);
		List<TourAgent> agents = new ArrayList<TourAgent>(solution.getTourAgents());
		List<TourActivity> acts = new ArrayList<TourActivity>(agents.get(1).getTourActivities());
		assertEquals(3, agents.get(1).getTourActivities().size());
		assertEquals(customerMap.get(makeId(1,4)),acts.get(1).getCustomer());
	}
	
	public void testIncreasingFraction2BeRemovedSolutionWithoutRelation(){
		removeRelations();
		randomRuin.setFractionOfAllNodes2beRuined(0.75);
		randomRuin.run(solution);
		assertEquals(1, solution.getTourAgents().size());
		TourAgent tourAgent = solution.getTourAgents().iterator().next();
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent.getTourActivities());
		assertEquals(3, tourAgent.getTourActivities().size());
		assertEquals(customerMap.get(makeId(1,4)),acts.get(1).getCustomer());
	}
	
	public void testRandomRuinWithNoCustomer(){
		try{
			randomRuin.run(new Solution(Collections.EMPTY_LIST));
			assertTrue(true);
		}
		catch(Exception e){
			assertTrue(false);
		}
	}
}
