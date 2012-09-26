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
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.Insertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.InsertionData;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.InsertionData.NoInsertionFound;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;

/**
 * Simplest recreation strategy. All removed customers are inserted where
 * insertion costs are minimal. I.e. each tour-agent is asked for minimal
 * marginal insertion costs. The tour-agent offering the lowest marginal
 * insertion costs gets the customer/shipment.
 * 
 * @author stefan schroeder
 * 
 */

public final class RecreationBestInsertion implements RecreationStrategy{
	
	private Logger logger = Logger.getLogger(RecreationBestInsertion.class);

	private Random random = RandomNumberGeneration.getRandom();

	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public void recreate(Collection<? extends ServiceProviderAgent> serviceProviders, Collection<Job> unassignedJobs, double result2beat) {
		List<Job> unassignedJobList = new ArrayList<Job>(unassignedJobs);
		Collections.shuffle(unassignedJobList, random);
		for(Job unassignedJob : unassignedJobList){
			Insertion bestInsertion = null;
			double bestInsertionCost = Double.MAX_VALUE;
			for(ServiceProviderAgent sp : serviceProviders){
				InsertionData iData = sp.calculateBestInsertion(unassignedJob, bestInsertionCost);
				if(iData instanceof NoInsertionFound) continue;
				if(iData.getInsertionCost() < bestInsertionCost){
					assert iData.getInsertionIndeces() != null : "no insertionIndeces set";
					bestInsertion = new Insertion(sp,iData);
					bestInsertionCost = iData.getInsertionCost();
				}
			}
			if(bestInsertion != null){
				bestInsertion.getTourAgent().insertJob(unassignedJob, bestInsertion.getInsertionData());
			} 
			else {
				throw new IllegalStateException(
						"given the vehicles, could not create a valid solution");
			}

		}
	}	
}
