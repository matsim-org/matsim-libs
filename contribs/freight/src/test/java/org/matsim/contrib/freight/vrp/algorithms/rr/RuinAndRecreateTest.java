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
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.factories.PickupAndDeliveryTourAlgoFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.TimeAndCapacityAndTWConstraints;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.VrpBuilder;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;

/**
 * test case: example is take from: http://web.mit.edu/urban_or_book/www/book/chapter6/6.4.12.html
 * @author stefan schröder
 *
 */

public class RuinAndRecreateTest extends TestCase{
	
	VehicleRoutingProblem vrp;
	
	RuinAndRecreate algo;
	
	List<List<Integer>> distanceMatrix;
	
	List<Integer> demand;
	
	Costs costs;
	
	VrpBuilder vrpBuilder;
	
	public void setUp(){
		Logger.getRootLogger().setLevel(Level.INFO);
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
			public Double getTransportTime(String fromId, String toId, double time) {
				int fromInt = Integer.parseInt(fromId);
				int toInt = Integer.parseInt(toId);
				if(fromInt >= toInt){
					return distanceMatrix.get(fromInt).get(toInt).doubleValue();
				}
				else{
					return distanceMatrix.get(toInt).get(fromInt).doubleValue();
				}
			}
			
			@Override
			public Double getDistance(String fromId, String toId, double time) {
				return getTransportTime(fromId,toId, 0.0);
			}
			
			@Override
			public Double getGeneralizedCost(String fromId, String toId, double time) {
				return getTransportTime(fromId,toId, 0.0);
			}

			@Override
			public Double getBackwardGeneralizedCost(String fromId,String toId, double arrivalTime) {
				return getGeneralizedCost(fromId, toId, arrivalTime);
			}

			@Override
			public Double getBackwardTransportTime(String fromId, String toId,double arrivalTime) {
				return getTransportTime(fromId, toId, arrivalTime);
			}

			@Override
			public Double getBackwardDistance(String fromId, String toId,double arrivalTime) {
				return getDistance(fromId, toId, arrivalTime);
			}
		};
		vrpBuilder = new VrpBuilder(costs, new TimeAndCapacityAndTWConstraints(250));
//		vrpBuilder.setDepot("0", 0.0, 0.0);
		for(Integer i=1;i<demand.size();i++){
			vrpBuilder.addJob(VrpUtils.createShipment(i.toString(), "0", i.toString(), demand.get(i), 
					VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE), VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE)));
		}
		
		RandomNumberGeneration.reset();
	}
	
	public void testSizeOfSolution(){
		vrpBuilder.addVehicle(VrpUtils.createVehicle("1","0", 23));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("2", "0", 23));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("3", "0", 23));
		VehicleRoutingProblem vrp = vrpBuilder.build();
		algo = new PickupAndDeliveryTourAlgoFactory().createAlgorithm(vrp, new InitialSolution().createInitialSolution(vrp));
		algo.run();
		int active = getActiveVehicles(algo.getSolution());
		assertEquals(2,active);
 	}
	
	public void testSolutionValue(){
		vrpBuilder.addVehicle(VrpUtils.createVehicle("1","0", 23));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("2", "0", 23));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("3", "0", 23));
		VehicleRoutingProblem vrp = vrpBuilder.build();
		algo = new PickupAndDeliveryTourAlgoFactory().createAlgorithm(vrp, new InitialSolution().createInitialSolution(vrp));
		algo.run();
		
		Collection<Tour> solution = algo.getSolution();
		int solVal = 0;
		for(Tour t : solution){
			solVal += t.costs.distance;
		}
		assertEquals(397,solVal);
 	}
	
	public void testSolutionSizeWithCapacity16(){
		vrpBuilder.addVehicle(VrpUtils.createVehicle("1","0", 16));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("2","0", 16));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("3","0", 16));
		VehicleRoutingProblem vrp = vrpBuilder.build();
		algo = new PickupAndDeliveryTourAlgoFactory().createAlgorithm(vrp, new InitialSolution().createInitialSolution(vrp));
		algo.run();
		int active = getActiveVehicles(algo.getSolution());
		assertEquals(3,active);
	}
	
	public void testSolutionValueWithCapacity16(){
		vrpBuilder.addVehicle(VrpUtils.createVehicle("1","0", 16));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("2", "0", 16));
		vrpBuilder.addVehicle(VrpUtils.createVehicle("3", "0", 16));
		VehicleRoutingProblem vrp = vrpBuilder.build();
		algo = new PickupAndDeliveryTourAlgoFactory().createAlgorithm(vrp, new InitialSolution().createInitialSolution(vrp));
		algo.run();
		
		Collection<Tour> solution = algo.getSolution();
		int solVal = 0;
		for(Tour t : solution){
			solVal += t.costs.distance;
		}
		assertEquals(solVal,445);
	}

	private int getActiveVehicles(Collection<Tour> solution) {
		int active = 0;
		for(Tour t : solution){
			if(t.getActivities().size()>2){
				active++;
			}
		}
		return active;
	}

}
