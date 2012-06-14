/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.recreation;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.Offer;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRDriverAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProvider;
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
			for(RRDriverAgent agent : tentativeSolution.getTourAgents()){
				Offer o = agent.requestService(unassignedJob, bestOffer.getPrice());
				if(o.getPrice() < bestOffer.getPrice()){
					bestOffer = o;
				}
			}
			if(!isNull(bestOffer.getServiceProvider())){
				currentResult += bestOffer.getPrice();
				bestOffer.getServiceProvider().offerGranted(unassignedJob);
			}
			else{
				throw new IllegalStateException("given the vehicles, could not create a valid solution");
			}
			
		}
	}

	private boolean isNull(ServiceProvider sp) {
		return (sp == null);
	}
}
