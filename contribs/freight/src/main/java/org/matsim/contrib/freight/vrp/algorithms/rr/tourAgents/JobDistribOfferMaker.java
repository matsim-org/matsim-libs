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
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;


import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.JobInsertionFinder.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.constraints.Constraints;
import org.matsim.core.utils.misc.Counter;

/**
 * calculates best marginal insertion cost for the single depot distribution problem and returns an 
 * an data-object 'OfferData' with these costs and the corresponding insertion indices.
 */
public class JobDistribOfferMaker implements OfferMaker{
	
	private Costs costs;
	
	private Counter buildTourCounter;
	
	private JobInsertionFinderFactory jifFactory;

	public JobDistribOfferMaker(Costs costs, Constraints constraints, JobInsertionFinderFactory jifFactory) {
		super();
		this.costs = costs;
		this.jifFactory = jifFactory;
		buildTourCounter = new Counter("nOfTourBuilts ");
	}

	
	public OfferData makeOffer(Vehicle vehicle, Tour tour, Job job, double bestKnownPrice){
		Shipment shipment = (Shipment)job;
		//preCheck whether capacity is sufficient
		if(!preCheck(tour,shipment,vehicle)){
			OfferData data = new OfferData(new Offer(Double.MAX_VALUE),null);
			return data;
		}
		InsertionData insertionData = jifFactory.createFinder(costs, vehicle, tour).find(job, bestKnownPrice);
		if(!insertionData.isNull()){
			OfferData data = new OfferData(new Offer(insertionData.mc),new MetaData(insertionData.pickupInsertionIndex, insertionData.deliveryInsertionIndex));
			return data;
		}
		else{
			OfferData data = new OfferData(new Offer(Double.MAX_VALUE),null);
			return data;
		}
	}
	
	private boolean preCheck(Tour tour, Shipment shipment, Vehicle vehicle) {
		if(tour.tourData.totalLoad + shipment.getSize() > vehicle.getCapacity()){
			return false;
		}
		return true;
	}
}
