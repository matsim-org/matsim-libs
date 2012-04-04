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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgent;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

/**
 * In the random-ruin strategy, current solution is ruined randomly. I.e. customer are removed randomly from current solution.
 * 
 * @author stefan schroeder
 *
 */

public class VehicleRuin implements RuinStrategy {
	
	private Logger logger = Logger.getLogger(VehicleRuin.class);

	private VehicleRoutingProblem vrp;

	private List<Job> unassignedJobs = new ArrayList<Job>();
	
	public void setRandom(Random random) {
	}

	public VehicleRuin(VehicleRoutingProblem vrp) {
		super();
		this.vrp = vrp;
		logger.info("initialise vehicle ruin");
		logger.info("done");
	}

	@Override
	public void run(RRSolution initialSolution) {
		clear();
		RRTourAgent agent2beRuined = null;
		for(RRTourAgent agent : initialSolution.getTourAgents()){
			if(!agent.isActive()){
				continue;
			}
			if(agent2beRuined == null){
				agent2beRuined = agent; 
			}
			else{
				if(agent.getTour().getActivities().size() < agent2beRuined.getTour().getActivities().size()){
					agent2beRuined = agent;
				}
			}
		}
		Set<Job> removedJobs = new HashSet<Job>();
		for(TourActivity act : agent2beRuined.getTour().getActivities()){
			if(act instanceof JobActivity){
				Job job = ((JobActivity) act).getJob();
				if(!removedJobs.contains(job)){
					removedJobs.add(job);
				}
			}
		}
		for(Job j : removedJobs){
			agent2beRuined.removeJob(j);
			unassignedJobs.add(j);
		}
	}
	
	private void clear() {
		unassignedJobs.clear();
	}

	@Override
	public List<Job> getUnassignedJobs() {
		return unassignedJobs;
	}

}
