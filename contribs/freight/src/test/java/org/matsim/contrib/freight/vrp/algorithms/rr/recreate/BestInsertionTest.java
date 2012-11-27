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
package org.matsim.contrib.freight.vrp.algorithms.rr.recreate;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.matsim.contrib.freight.vrp.algorithms.rr.VRPTestCase;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesCostAndTWs;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesLocalActInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesShipmentInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.StandardRouteAgentFactory;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.utils.VrpTourBuilder;

public class BestInsertionTest extends VRPTestCase {
	RecreationBestInsertion bestInsertion;

	VehicleRoutingProblem vrp;
	
	RouteAgentFactory routeAgentFactory;

	VehicleRoute route1;
	
	VehicleRoute route2;
	
	VehicleRoutingProblemSolution solution;

	List<Job> unassignedJobs;

	@Before
	public void setUp() {

		initJobsInPlainCoordinateSystem();

		vrp = getVRP(2, 2);

		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		Shipment s1 = createShipment("1", makeId(0, 10), makeId(0, 0));

		tourBuilder.scheduleStart(makeId(0, 0), 0.0, 0.0);
		tourBuilder.schedulePickup(s1);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.scheduleEnd(makeId(0, 0), 0.0, Double.MAX_VALUE);

		TourImpl tour1 = tourBuilder.build();
		
		Vehicle vehicle = vrp.getVehicles().iterator().next();
		Driver driver = new Driver() {};
		
		CalculatesCostAndTWs tourStateCalculator = new CalculatesCostAndTWs(costs);
		tourStateCalculator.calculate(tour1, vehicle, driver);
		
		RouteAgentFactory routeAgentFactory = new StandardRouteAgentFactory(new CalculatesShipmentInsertion(costs, new CalculatesLocalActInsertion(costs)), tourStateCalculator);;
		
		route1 = new VehicleRoute(tour1, driver, vehicle);
		
		VrpTourBuilder anotherTourBuilder = new VrpTourBuilder();
		Shipment s2 = createShipment("2", makeId(10, 0), makeId(0, 0));
		anotherTourBuilder.scheduleStart(makeId(0, 0), 0.0, 0.0);
		anotherTourBuilder.schedulePickup(s2);
		anotherTourBuilder.scheduleDelivery(s2);
		anotherTourBuilder.scheduleEnd(makeId(0, 0), 0.0, Double.MAX_VALUE);
		
		TourImpl tour2 = anotherTourBuilder.build();
		tourStateCalculator.calculate(tour2, vehicle, driver);
		
		route2 = new VehicleRoute(tour2, driver, vehicle);
		
		Collection<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		routes.add(route1);
		routes.add(route2);
		
		solution = new VehicleRoutingProblemSolution(routes, getTotalCost(routes));

		bestInsertion = new RecreationBestInsertion(routeAgentFactory);
		unassignedJobs = new ArrayList<Job>();

		Shipment s3 = createShipment("3", makeId(0, 5), makeId(2, 10));
		unassignedJobs.add(s3);
	}

	private double getTotalCost(Collection<VehicleRoute> routes) {
		double c = 0.0;
		for(VehicleRoute r : routes){
			c += r.getCost();
		}
		return c;
	}

	@Test
	public void whenRecreatingSolution_sizeOfThisSolution_isTwo() {
		bestInsertion.recreate(solution.getRoutes(), unassignedJobs, Double.MAX_VALUE);
		assertEquals(2, solution.getRoutes().size());
	}

	@Test
	public void whenRecreatingSolution_nOfActOfRoute1_isSix() {
		bestInsertion.recreate(solution.getRoutes(), unassignedJobs,Double.MAX_VALUE);
		assertEquals(6, route1.getTour().getActivities().size());
	}

	@Test
	public void whenRecreatingSolution_nOfActOfRoute2_isFour() {
		bestInsertion.recreate(solution.getRoutes(), unassignedJobs,Double.MAX_VALUE);
		assertEquals(4, route2.getTour().getActivities().size());
	}

	@Test
	public void whenRecreatingSolution_marginalCostOfInsertion_isFour() {
		double oldCost = solution.getTotalCost();
		bestInsertion.recreate(solution.getRoutes(), unassignedJobs, Double.MAX_VALUE);
		double newCost = getTotalCost(solution.getRoutes());
		assertEquals(4.0, newCost - oldCost, 0.1);
	}

	@Test
	public void whenRecreatingSolution_totalCost_is244() {
		double oldCost = solution.getTotalCost();
		assertEquals(40.0, oldCost, 0.1);
		bestInsertion.recreate(solution.getRoutes(), unassignedJobs,Double.MAX_VALUE);
		double newCost = getTotalCost(solution.getRoutes());
		assertEquals(44.0, newCost, 0.1);
	}

}
