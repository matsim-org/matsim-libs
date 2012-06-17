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


import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;

/**
 * 
 * @author stefan schroeder
 *
 */

public class RRSolution {
	
	private Collection<ServiceProviderAgent> tourAgents;
	
	private double score = 0.0;
	
	private boolean solutionSet = false;
	
	public RRSolution(Collection<ServiceProviderAgent> tourAgents) {
		super();
		this.tourAgents = tourAgents;
	}
	
	
	
	public double getResult() {
		if(solutionSet){
			return score;
		}
		double total = 0.0;
		for(TourAgent a : tourAgents){
			total += a.getTourCost();
		}
		return total;
	}

	public Collection<ServiceProviderAgent> getTourAgents(){
		return tourAgents;
	}

	@Override
	public String toString() {
		return "totalResult="+getResult();
	}


	public void setScore(double score) {
		solutionSet = true;
		this.score = score;
	}
}
