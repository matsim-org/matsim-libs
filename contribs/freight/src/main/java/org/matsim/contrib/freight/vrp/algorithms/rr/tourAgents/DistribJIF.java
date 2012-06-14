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

import java.util.Iterator;


import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public class DistribJIF implements JobInsertionFinder{

	private Vehicle veh;
	
	private Tour tour;
	
	private Costs costs;
	
	private MCCalculatorFactory mcCalculatorFactory;
	
	public DistribJIF(Costs costs, Vehicle veh, Tour tour) {
		super();
		this.veh = veh;
		this.tour = tour;
		this.costs = costs;
		mcCalculatorFactory = new LocalMCCalculatorFactory();
	}

	public void setMcCalculatorFactory(MCCalculatorFactory mcCalculatorFactory) {
		this.mcCalculatorFactory = mcCalculatorFactory;
	}

	@Override
	public InsertionData find(Job job, double bestKnownCosts) {
		Double bestMarginalCost = bestKnownCosts;
		Shipment shipment = (Shipment)job;
		Delivery deliveryAct = new Delivery(shipment);
		Integer insertionIndex = null;
		Iterator<TourActivity> actIter = tour.getActivities().listIterator();
		TourActivity prevAct = actIter.next();
		while(actIter.hasNext()){
			TourActivity currAct = actIter.next();
			if(!anotherPreCheck(prevAct,currAct,deliveryAct)){
				prevAct = currAct;
				continue;
			}
			double mc = mcCalculatorFactory.createCalculator(costs, tour).calculateMarginalCosts(prevAct, currAct, deliveryAct);
			if(mc < bestMarginalCost){
				bestMarginalCost = mc;
				insertionIndex = tour.getActivities().indexOf(currAct);
			}
			prevAct = currAct;
		}
		if(insertionIndex == null){
			return new InsertionData(bestMarginalCost, null, null);
		}
		return new InsertionData(bestMarginalCost,1,insertionIndex);
	}
	
	private boolean anotherPreCheck(TourActivity prevAct, TourActivity currAct, TourActivity deliveryAct) {
		if(deliveryAct.getLatestOperationStartTime() < prevAct.getEarliestOperationStartTime()){
			return false;
		}
		if(deliveryAct.getEarliestOperationStartTime() > currAct.getLatestOperationStartTime()){
			return false;
		}
		return true;
	}
}
