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
package org.matsim.contrib.freight.vrp.algorithms.rr.recreation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgent.Offer;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;



/**
 * Simplest recreation strategy. All removed customers are inserted where insertion costs are minimal. I.e. each tour-agent is asked for
 * minimal marginal insertion costs. The tour-agent offering the lowest marginal insertion costs gets the customer/shipment.
 * 
 * @author stefan schroeder
 *
 */

public class BestInsertion implements RecreationStrategy{

	private Logger logger = Logger.getLogger(BestInsertion.class);
	
	private Map<String, String> cheapestAgents = new HashMap<String, String>();
	
	private Random random = RandomNumberGeneration.getRandom();
	
	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public void run(RRSolution tentativeSolution, List<Job> unassignedJobs) {
		Collections.shuffle(unassignedJobs,random);
		for(Job unassignedJob : unassignedJobs){
			Offer bestOffer = null;
			boolean firstAgent = true;
			double bestKnownPrice = Double.MAX_VALUE;
			RRTourAgent cheapestAgent = getCheapestAgent(tentativeSolution, unassignedJob);
			if(cheapestAgent != null){
				bestOffer = cheapestAgent.requestService(unassignedJob, bestKnownPrice);
				if(bestOffer != null){
					bestKnownPrice = bestOffer.getPrice();
					firstAgent = false;
				}
			}
			for(RRTourAgent agent : tentativeSolution.getTourAgents()){
				if(agent == cheapestAgent){
					continue;
				}
				Offer offer = agent.requestService(unassignedJob,bestKnownPrice);
				if(offer == null){
					continue;
				}
				if(firstAgent){
					bestOffer = offer;
					bestKnownPrice = offer.getPrice();
					firstAgent = false;
				}
				else if(offer.getPrice() < bestOffer.getPrice()){
					bestOffer = offer;
					bestKnownPrice = offer.getPrice();
				}
			}
			if(bestOffer != null){
				bestOffer.getTourAgent().offerGranted(unassignedJob);
				cheapestAgents.put(unassignedJob.getId(), bestOffer.getTourAgent().getId());
			}
			else{
				throw new IllegalStateException("given the vehicles, could not create a valid solution");
			}
		}
	}

	private RRTourAgent getCheapestAgent(RRSolution tentativeSolution, Job unassignedJob) {
		String agentId = null;
		if(cheapestAgents.containsKey(unassignedJob.getId())){
			agentId = cheapestAgents.get(unassignedJob.getId());
		}
		if(agentId != null){
			for(RRTourAgent agent : tentativeSolution.getTourAgents()){
				if(agent.getId().equals(agentId)){
					return agent;
				}
			}
		}
		return null;
	}
}
