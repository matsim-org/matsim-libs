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

import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;

public class RuinAndRecreateReport implements RuinAndRecreateListener{

	private int nOfIteration = 0;
	
	private double bestResult;
	
	private RuinAndRecreateSolution bestSolution;
	
	@Override
	public void inform(RuinAndRecreateEvent event) {
		nOfIteration++;
		bestResult = event.getCurrentResult();
		bestSolution = event.getCurrentSolution();
	}

	@Override
	public void finish() {
		System.out.println("totalCosts="+Math.round(bestResult));
		System.out.println("#vehicles="+getActiveTours());
		System.out.println("transportCosts="+getGenCosts());
		System.out.println("transportTime=" + getTime());
	}
	
	public double getTime() {
		double time = 0.0;
		for(ServiceProviderAgent t : bestSolution.getTourAgents()){
			if(t.isActive()){
				time+=t.getTour().tourData.transportTime;
			}
		}
		return time;
	}

	public double getGenCosts(){
		double dist = 0.0;
		for(ServiceProviderAgent t : bestSolution.getTourAgents()){
			if(t.isActive()){
				dist+=t.getTour().tourData.transportCosts;
			}
		}
		return dist;
	}
	
	public int getActiveTours() {
		int nOfTours = 0;
		for(ServiceProviderAgent t : bestSolution.getTourAgents()){
			if(t.isActive()){
				nOfTours++;
			}
		}
		return nOfTours;
	}

	private Long round(double time) {
		return Math.round(time);
	}
	
	

}
