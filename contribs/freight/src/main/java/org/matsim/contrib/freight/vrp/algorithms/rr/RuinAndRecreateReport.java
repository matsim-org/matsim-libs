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

import org.matsim.contrib.freight.vrp.algorithms.rr.listener.AlgorithmEndsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;

public class RuinAndRecreateReport implements AlgorithmEndsListener{

	
	private double getTime(RuinAndRecreateSolution solution) {
		double time = 0.0;
		for(ServiceProviderAgent t : solution.getTourAgents()){
			if(t.isActive()){
				time+=t.getTour().tourData.transportTime;
			}
		}
		return time;
	}

	private double getGenCosts(RuinAndRecreateSolution solution){
		double dist = 0.0;
		for(ServiceProviderAgent t : solution.getTourAgents()){
			if(t.isActive()){
				dist+=t.getTour().tourData.transportCosts;
			}
		}
		return dist;
	}
	
	private int getActiveTours(RuinAndRecreateSolution solution) {
		int nOfTours = 0;
		for(ServiceProviderAgent t : solution.getTourAgents()){
			if(t.isActive()){
				nOfTours++;
			}
		}
		return nOfTours;
	}

	private Long round(double time) {
		return Math.round(time);
	}

	@Override
	public void informAlgorithmEnds(RuinAndRecreateSolution currentSolution) {
		System.out.println("totalCosts="+round(currentSolution.getResult()));
		System.out.println("#vehicles="+getActiveTours(currentSolution));
		System.out.println("transportCosts="+round(getGenCosts(currentSolution)));
		System.out.println("transportTime=" + round(getTime(currentSolution)));
	}
	
	

}
