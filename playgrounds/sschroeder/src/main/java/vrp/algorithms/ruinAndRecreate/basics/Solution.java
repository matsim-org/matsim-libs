/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
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
package vrp.algorithms.ruinAndRecreate.basics;

import java.util.Collection;

import vrp.algorithms.ruinAndRecreate.api.TourAgent;


/**
 * 
 * @author stefan schroeder
 *
 */

public class Solution {
	
	private Collection<TourAgent> tourAgents;
	
	private CostFunction costFunction;

	public Solution(Collection<TourAgent> tourAgents) {
		super();
		this.tourAgents = tourAgents;
		this.costFunction = new CostFunction();
	}

	public double getResult() {
		return computeTotalResult();
	}
	
	public Collection<TourAgent> getTourAgents(){
		return tourAgents;
	}

	private double computeTotalResult() {
		costFunction.reset();
		for(TourAgent a : tourAgents){
			costFunction.add(a.getTotalCost());
		}
		return costFunction.getTotalCost();
	}
	
	public void setCostFunction(CostFunction costFunction) {
		this.costFunction = costFunction;
	}

	@Override
	public String toString() {
		return "totalResult="+getResult();
	}
}
