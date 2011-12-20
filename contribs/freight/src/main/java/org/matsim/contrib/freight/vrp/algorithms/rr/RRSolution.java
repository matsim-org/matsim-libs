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
package org.matsim.contrib.freight.vrp.algorithms.rr;


import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;

/**
 * 
 * @author stefan schroeder
 *
 */

public class RRSolution {
	
	private Collection<TourAgent> tourAgents;

	public RRSolution(Collection<TourAgent> tourAgents) {
		super();
		this.tourAgents = tourAgents;
	}

	public double getResult() {
		double total = 0.0;
		for(TourAgent a : tourAgents){
			total += a.getTourCost();
		}
		return total;
	}
	
	public Collection<TourAgent> getTourAgents(){
		return tourAgents;
	}

	@Override
	public String toString() {
		return "totalResult="+getResult();
	}
}
