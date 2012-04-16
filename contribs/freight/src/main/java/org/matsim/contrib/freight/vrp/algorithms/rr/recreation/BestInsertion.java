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
import java.util.List;
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
	
	private Random random = RandomNumberGeneration.getRandom();
	
	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public void run(RRSolution tentativeSolution, List<Job> unassignedJobs, double upperBound) {
		Collections.shuffle(unassignedJobs,random);
		double currentResult = tentativeSolution.getResult();
		for(Job unassignedJob : unassignedJobs){
			if(currentResult >= upperBound){
				return;
			}		
			Offer bestOffer = new Offer(null,Double.MAX_VALUE,Double.MAX_VALUE);
			for(RRTourAgent agent : tentativeSolution.getTourAgents()){
				Offer o = agent.requestService(unassignedJob, bestOffer.getMarginalCosts());
				if(o.getPrice() < bestOffer.getPrice()){
					bestOffer = o;
				}
			}
			if(!isNull(bestOffer.getTourAgent())){
				currentResult += bestOffer.getMarginalCosts();
				bestOffer.getTourAgent().offerGranted(unassignedJob);
			}
			else{
				throw new IllegalStateException("given the vehicles, could not create a valid solution");
			}
			
		}
	}

	private boolean isNull(RRTourAgent tourAgent) {
		return (tourAgent == null);
	}
}
