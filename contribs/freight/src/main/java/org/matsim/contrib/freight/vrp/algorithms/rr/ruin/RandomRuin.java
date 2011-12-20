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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

/**
 * In the random-ruin strategy, current solution is ruined randomly. I.e. customer are removed randomly from current solution.
 * 
 * @author stefan schroeder
 *
 */

public class RandomRuin implements RuinStrategy {
	
	private Logger logger = Logger.getLogger(RandomRuin.class);

	private VehicleRoutingProblem vrp;

	private double fractionOfAllNodes2beRuined;
	
	private List<Job> unassignedJobs = new ArrayList<Job>();
	
	private Random random = RandomNumberGeneration.getRandom();
	
	public void setRandom(Random random) {
		this.random = random;
	}

	public RandomRuin(VehicleRoutingProblem vrp) {
		super();
		this.vrp = vrp;
		logger.info("initialise random ruin");
		logger.info("done");
	}

	@Override
	public void run(RRSolution initialSolution) {
		clear();
		int nOfJobs2BeRemoved = selectNuOfJobs2BeRemoved();
		LinkedList<Job> availableJobs = new LinkedList<Job>(vrp.getJobs().values());
		for(int i=0;i<nOfJobs2BeRemoved;i++){
			Job job = pickRandomJob(availableJobs);
			logger.debug("randomJob: " + job);
			for(TourAgent agent : initialSolution.getTourAgents()){
				if(agent.hasJob(job)){
					agent.removeJob(job);
					unassignedJobs.add(job);
					availableJobs.remove(job);
				}
			}
		}
	}

	private Job pickRandomJob(LinkedList<Job> availableJobs) {
		int randomIndex = random.nextInt(availableJobs.size());
		return availableJobs.get(randomIndex);
	}

	public void setFractionOfAllNodes2beRuined(double fractionOfAllNodes2beRuined) {
		this.fractionOfAllNodes2beRuined = fractionOfAllNodes2beRuined;
	}
	
	private int selectNuOfJobs2BeRemoved(){
		return (int)Math.round(vrp.getJobs().values().size()*fractionOfAllNodes2beRuined);
	}
	
	private void clear() {
		unassignedJobs.clear();
	}

	@Override
	public List<Job> getUnassignedJobs() {
		return unassignedJobs;
	}

}
