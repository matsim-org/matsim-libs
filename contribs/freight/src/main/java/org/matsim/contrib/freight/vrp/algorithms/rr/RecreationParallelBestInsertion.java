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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.Offer;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;



/**
 * Simplest recreation strategy. All removed customers are inserted where insertion costs are minimal. I.e. each tour-agent is asked for
 * minimal marginal insertion costs. The tour-agent offering the lowest marginal insertion costs gets the customer/shipment.
 * 
 * @author stefan schroeder
 *
 */

public final class RecreationParallelBestInsertion implements RecreationStrategy{
	
	private Logger logger = Logger.getLogger(RecreationParallelBestInsertion.class);
	
	private Random random = RandomNumberGeneration.getRandom();
	
	private ExecutorService executor;
	
	public RecreationParallelBestInsertion(ExecutorService executor) {
		super();
		this.executor = executor;
	}

	public void setRandom(Random random) {
		this.random = random;
	}
	
	@Override
	public void recreate(Collection<? extends ServiceProviderAgent> serviceProviders, Collection<Job> unassignedJobs) {
		List<Job> unassignedJobList = new ArrayList<Job>(unassignedJobs);
		Collections.shuffle(unassignedJobList,random);
		
		for(final Job unassignedJob : unassignedJobList){
			Offer bestOffer = new Offer(null,Double.MAX_VALUE);
			CompletionService<Offer> completionService = new ExecutorCompletionService<Offer>(executor);
			for(final ServiceProviderAgent sp : serviceProviders){
				completionService.submit(new Callable<Offer>(){

					@Override
					public Offer call() throws Exception {
						return sp.requestService(unassignedJob, Double.MAX_VALUE);
					}
					
				});
			}
			for(int i=0;i<serviceProviders.size();i++){
				try {
					Future<Offer> fo = completionService.take();
					Offer o = fo.get();;
					if(o.getPrice() < bestOffer.getPrice()){
						bestOffer = o;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			if(!isNull(bestOffer.getServiceProvider())){
				bestOffer.getServiceProvider().offerGranted(unassignedJob);
			}
			else{
				throw new IllegalStateException("given the vehicles, could not create a valid solution");
			}
			
		}
	}

	private boolean isNull(ServiceProviderAgent sp) {
		return (sp == null);
	}

	
}
