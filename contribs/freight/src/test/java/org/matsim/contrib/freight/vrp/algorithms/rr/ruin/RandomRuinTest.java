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


import java.util.Collection;
import java.util.Collections;

import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.VRPTestCase;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRDriverAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

public class RandomRuinTest extends VRPTestCase{

	VehicleRoutingProblem vrp;
	
	RRSolution solution;
	
	RandomRuin randomRuin;
	
	@Override
	public void setUp(){
		initJobsInPlainCoordinateSystem();
		vrp = getVRP(2, 2);
		solution = getInitialSolution(vrp);
		randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.25);
		RandomNumberGeneration.reset();
	}

	public void testSizeOfRuinedSolution(){
		randomRuin.ruin(solution.getTourAgents());
		assertEquals(2, solution.getTourAgents().size());
	}
	
	public void testRemainingSolution(){
		int nOfRemainingActivities = 0;
		for(ServiceProviderAgent a : solution.getTourAgents()){
			nOfRemainingActivities += a.getTour().getActivities().size();
		}
		assertEquals(10,nOfRemainingActivities);
		randomRuin.ruin(solution.getTourAgents());
		nOfRemainingActivities = 0;
		for(ServiceProviderAgent a : solution.getTourAgents()){
			nOfRemainingActivities += a.getTour().getActivities().size();
		}
		assertEquals(8,nOfRemainingActivities);
	}
	
	public void testWhetherJobInUnassignedJobListIsReallyAnUnassignedJob(){
		Collection<Job> unassignedJobs = randomRuin.ruin(solution.getTourAgents());
		Job unassignedJob = unassignedJobs.iterator().next();
		boolean jobFoundInAgentsTour = false;
		for(ServiceProviderAgent a : solution.getTourAgents()){
			for(TourActivity act : a.getTour().getActivities()){
				if(act instanceof JobActivity){
					if(((JobActivity) act).getJob().equals(unassignedJob)){
						jobFoundInAgentsTour = true;
					}
				}
			}
		}
		assertFalse(jobFoundInAgentsTour);
	}
	
	public void testNuOfUnassignedJobs(){
		Collection<Job> unassignedJobs = randomRuin.ruin(solution.getTourAgents());
		assertEquals(1,unassignedJobs.size());
	}
	
	public void testRandomRuinWithNoCustomer(){
		try{
			randomRuin.ruin(Collections.EMPTY_LIST);
			assertTrue(true);
		}
		catch(Exception e){
			assertTrue(false);
		}
	}
}
