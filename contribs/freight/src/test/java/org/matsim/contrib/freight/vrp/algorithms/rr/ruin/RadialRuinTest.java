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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.matsim.contrib.freight.vrp.algorithms.rr.VRPTestCase;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesCostAndTWs;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesLocalActInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesShipmentInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.StandardRouteAgentFactory;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;

public class RadialRuinTest extends VRPTestCase {

	VehicleRoutingProblem vrp;

	VehicleRoutingProblemSolution solution;

	RuinRadial radialRuin;
	
	RouteAgentFactory routeAgentFactory;

	@Before
	public void setUp() {
		initJobsInPlainCoordinateSystem();
		vrp = getVRP(2, 2);
		solution = getInitialSolution(vrp);
		
		CalculatesCostAndTWs tourStateCalculator = new CalculatesCostAndTWs(costs);
		routeAgentFactory = new StandardRouteAgentFactory(new CalculatesShipmentInsertion(costs, new CalculatesLocalActInsertion(costs)), tourStateCalculator);
		radialRuin = new RuinRadial(vrp, routeAgentFactory, new JobDistanceAvgCosts(vrp.getCosts()));
		radialRuin.setRuinFraction(0.25);
		RandomNumberGeneration.reset();
		/*
		 * fraction=0.5 picks 0,10 rem 0,10 rem 1,5
		 */
	}

	@Test
	public void whenRuinSolution_sizeOfRemainingSolution_is2() {
		radialRuin.ruin(solution.getRoutes());
		assertEquals(2, solution.getRoutes().size());
	}

	@Test
	public void whenRuiningSolution_nOfRemainingAct_is8() {
		int nOfRemainingActivities = 0;
		for (VehicleRoute route : solution.getRoutes()) {
			nOfRemainingActivities += route.getTour().getActivities().size();
		}
		assertEquals(10, nOfRemainingActivities);
		
		radialRuin.ruin(solution.getRoutes());
		nOfRemainingActivities = 0;
		for (VehicleRoute r : solution.getRoutes()) {
			nOfRemainingActivities += r.getTour().getActivities().size();
		}
		assertEquals(8, nOfRemainingActivities);
	}

	@Test
	public void whenRuiningASolution_theRemovedJobs_mustBeInUnassignedListAndNotInRouteAnymore() {
		Collection<Job> unassignedJobs = radialRuin.ruin(solution.getRoutes());
		Job unassignedJob = unassignedJobs.iterator().next();
		boolean jobFoundInAgentsTour = false;
		for (VehicleRoute r : solution.getRoutes()) {
			for (TourActivity act : r.getTour().getActivities()) {
				if (act instanceof JobActivity) {
					if (((JobActivity) act).getJob().equals(unassignedJob)) {
						jobFoundInAgentsTour = true;
					}
				}
			}
		}
		assertFalse(jobFoundInAgentsTour);
	}

	@Test
	public void testWhetherRadialRuinActualRemovesClosestJob() {
		Collection<Job> unassignedJs = radialRuin.ruin(solution.getRoutes());
		List<Job> unassignedJobs = new ArrayList<Job>(unassignedJs);
		assertEquals("2", unassignedJobs.get(0).getId());
	}

	@Test
	public void testNuOfUnassignedJobs() {
		Collection<Job> unassignedJobs = radialRuin.ruin(solution.getRoutes());
		assertEquals(1, unassignedJobs.size());
	}

	@Test
	public void testRandomRuinWithNoCustomer() {
		radialRuin.ruin(Collections.EMPTY_LIST);
		assertTrue(true);
		
	}
}
