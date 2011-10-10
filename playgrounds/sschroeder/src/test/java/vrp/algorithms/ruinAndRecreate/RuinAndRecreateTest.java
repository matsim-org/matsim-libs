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
package vrp.algorithms.ruinAndRecreate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.algorithms.ruinAndRecreate.factories.StandardRuinAndRecreateFactory;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.SingleDepotVRP;
import vrp.basics.RandomNumberGeneration;
import vrp.basics.SingleDepotInitialSolutionFactoryImpl;
import vrp.basics.SingleDepotVRPBuilder;
import vrp.basics.Tour;

/**
 * test case: example is take from: http://web.mit.edu/urban_or_book/www/book/chapter6/6.4.12.html
 * @author stefan schröder
 *
 */

public class RuinAndRecreateTest extends TestCase{
	
	SingleDepotVRP vrp;
	
	RuinAndRecreate algo;
	
	List<List<Integer>> distanceMatrix;
	
	List<Integer> demand;
	
	Costs costs;
	
	Constraints constraints;
	
	SingleDepotVRPBuilder vrpBuilder;
	
	public void setUp(){
		
		/*
		 * example is take from: 
		 * 
		 */
		List<Integer> row1 = Arrays.asList(0,0,0,0,0,0,0,0,0,0);
		List<Integer> row2 = Arrays.asList(25,0,0,0,0,0,0,0,0,0);
		List<Integer> row3 = Arrays.asList(43,29,0,48,14,8,0,3,2,23);
		List<Integer> row4 = Arrays.asList(57,34,52,0,55,47,15,3,6,20);
		List<Integer> row5 = Arrays.asList(43,43,72,45,0,77,36,19,26,49);
		List<Integer> row6 = Arrays.asList(61,68,96,71,27,0,50,36,47,86);
		List<Integer> row7 = Arrays.asList(29,49,72,71,36,40,0,39,46,57);
		List<Integer> row8 = Arrays.asList(41,66,81,95,65,66,31,0,78,66);
		List<Integer> row9 = Arrays.asList(48,72,89,99,65,62,31,11,0,83);
		List<Integer> row10 = Arrays.asList(71,91,114,108,65,46,43,46,36,0);
		distanceMatrix = Arrays.asList(row1,row2,row3,row4,row5,row6,row7,row8,row9,row10);
		demand = Arrays.asList(0,4,6,5,4,7,3,5,4,4);
		
		costs = new Costs() {
			
			@Override
			public Double getTime(Node from, Node to) {
				int fromInt = Integer.parseInt(from.getId());
				int toInt = Integer.parseInt(to.getId());
				if(fromInt >= toInt){
					return distanceMatrix.get(fromInt).get(toInt).doubleValue();
				}
				else{
					return distanceMatrix.get(toInt).get(fromInt).doubleValue();
				}
			}
			
			@Override
			public Double getDistance(Node from, Node to) {
				return getTime(from,to);
			}
			
			@Override
			public Double getCost(Node from, Node to) {
				return getTime(from,to);
			}
		};
		vrpBuilder = new SingleDepotVRPBuilder();
		Customer depot=null;
		for(Integer i=0;i<demand.size();i++){
			Customer customer = vrpBuilder.createAndAddCustomer(i.toString(), vrpBuilder.getNodeFactory().createNode(i.toString()), demand.get(i), 0.0, Double.MAX_VALUE, 0.0);
			if(i==0){
				depot = customer;
			}
		}
		constraints = new CapacityConstraint();
		
		vrpBuilder.setDepot(depot);
		RandomNumberGeneration.reset();
	}
	
	public void testSizeOfSolution(){
		vrpBuilder.setVehicleType(23);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		SingleDepotVRP vrp = vrpBuilder.buildVRP();
		
		algo = new StandardRuinAndRecreateFactory().createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp), vrp.getVehicleType().capacity);
		algo.run();
		Collection<Tour> solution = algo.getSolution();
		assertEquals(solution.size(),2);
 	}
	
	public void testSolutionValue(){
		vrpBuilder.setVehicleType(23);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		SingleDepotVRP vrp = vrpBuilder.buildVRP();
		
		algo = new StandardRuinAndRecreateFactory().createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp), vrp.getVehicleType().capacity);
		algo.run();
		Collection<Tour> solution = algo.getSolution();
		int solVal = 0;
		for(Tour t : solution){
			solVal += t.costs.distance;
		}
		assertEquals(solVal,397);
 	}
	
	public void testCustomerSequence(){
		vrpBuilder.setVehicleType(23);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		SingleDepotVRP vrp = vrpBuilder.buildVRP();
		
		algo = new StandardRuinAndRecreateFactory().createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp), vrp.getVehicleType().capacity);
		algo.run();
		Collection<Tour> solution = algo.getSolution();
		List<Tour> solList = new ArrayList<Tour>(solution);
		assertEquals(solList.get(0).getActivities().get(0).getCustomer().getId(),"0");
		assertEquals(solList.get(0).getActivities().get(1).getCustomer().getId(),"2");
		assertEquals(solList.get(0).getActivities().get(2).getCustomer().getId(),"1");
		assertEquals(solList.get(0).getActivities().get(3).getCustomer().getId(),"3");
		assertEquals(solList.get(0).getActivities().get(4).getCustomer().getId(),"4");
		assertEquals(solList.get(0).getActivities().get(5).getCustomer().getId(),"0");
		
		assertEquals(solList.get(1).getActivities().get(0).getCustomer().getId(),"0");
		assertEquals(solList.get(1).getActivities().get(1).getCustomer().getId(),"7");
		assertEquals(solList.get(1).getActivities().get(2).getCustomer().getId(),"8");
		assertEquals(solList.get(1).getActivities().get(3).getCustomer().getId(),"9");
		assertEquals(solList.get(1).getActivities().get(4).getCustomer().getId(),"5");
		assertEquals(solList.get(1).getActivities().get(5).getCustomer().getId(),"6");
		assertEquals(solList.get(1).getActivities().get(6).getCustomer().getId(),"0");

 	}
	
	public void testSolutionSizeWithCapacity16(){
		vrpBuilder.setVehicleType(16);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		SingleDepotVRP vrp = vrpBuilder.buildVRP();
		
		algo = new StandardRuinAndRecreateFactory().createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp), vrp.getVehicleType().capacity);
		algo.run();
		Collection<Tour> solution = algo.getSolution();
		assertEquals(solution.size(),3);
	}
	
	public void testSolutionValueWithCapacity16(){
		vrpBuilder.setVehicleType(16);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		SingleDepotVRP vrp = vrpBuilder.buildVRP();
		
		algo = new StandardRuinAndRecreateFactory().createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp), vrp.getVehicleType().capacity);
		algo.run();
		Collection<Tour> solution = algo.getSolution();
		int solVal = 0;
		for(Tour t : solution){
			solVal += t.costs.distance;
		}
		assertEquals(solVal,445);
	}
	
	public void testCustomerSequenceCap16(){
		vrpBuilder.setVehicleType(16);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		SingleDepotVRP vrp = vrpBuilder.buildVRP();
		
		algo = new StandardRuinAndRecreateFactory().createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp), vrp.getVehicleType().capacity);
		algo.run();
		Collection<Tour> solution = algo.getSolution();
		List<Tour> solList = new ArrayList<Tour>(solution);
		assertEquals(solList.get(0).getActivities().get(0).getCustomer().getId(),"0");
		assertEquals(solList.get(0).getActivities().get(1).getCustomer().getId(),"1");
		assertEquals(solList.get(0).getActivities().get(2).getCustomer().getId(),"3");
		assertEquals(solList.get(0).getActivities().get(3).getCustomer().getId(),"2");
		assertEquals(solList.get(0).getActivities().get(4).getCustomer().getId(),"0");
		
		assertEquals(solList.get(1).getActivities().get(0).getCustomer().getId(),"0");
		assertEquals(solList.get(1).getActivities().get(1).getCustomer().getId(),"7");
		assertEquals(solList.get(1).getActivities().get(2).getCustomer().getId(),"8");
		assertEquals(solList.get(1).getActivities().get(3).getCustomer().getId(),"9");
		assertEquals(solList.get(1).getActivities().get(4).getCustomer().getId(),"6");
		assertEquals(solList.get(1).getActivities().get(5).getCustomer().getId(),"0");
		
		assertEquals(solList.get(2).getActivities().get(0).getCustomer().getId(),"0");
		assertEquals(solList.get(2).getActivities().get(1).getCustomer().getId(),"5");
		assertEquals(solList.get(2).getActivities().get(2).getCustomer().getId(),"4");
		assertEquals(solList.get(2).getActivities().get(3).getCustomer().getId(),"0");
 	}

}
