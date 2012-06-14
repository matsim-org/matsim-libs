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

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRDriverAgent;

public class RuinAndRecreateReport implements RuinAndRecreateListener{

	private int nOfIteration = 0;
	
	private double bestResult;
	
	private RRSolution bestSolution;
	
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
		System.out.println("genCosts="+getGenCosts());
		System.out.println("time=" + getTime());
	}
	
	private double getTime() {
		double time = 0.0;
		for(RRDriverAgent t : bestSolution.getTourAgents()){
			if(t.getTour().getActivities().size()>2){
				time+=t.getTour().costs.transportTime;
			}
		}
		return time;
	}

	private double getGenCosts(){
		double dist = 0.0;
		for(RRDriverAgent t : bestSolution.getTourAgents()){
			if(t.getTour().getActivities().size()>2){
				dist+=t.getTour().costs.transportCosts;
			}
		}
		return dist;
	}
	
	private int getActiveTours() {
		int nOfTours = 0;
		for(RRDriverAgent t : bestSolution.getTourAgents()){
			if(t.getTour().getActivities().size()>2){
				nOfTours++;
			}
		}
		return nOfTours;
	}

	private Long round(double time) {
		return Math.round(time);
	}
	
	

}
