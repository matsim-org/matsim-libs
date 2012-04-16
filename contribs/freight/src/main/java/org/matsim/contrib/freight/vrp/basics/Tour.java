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
package org.matsim.contrib.freight.vrp.basics;

import java.util.LinkedList;

/**
 * 
 * @author stefan schroeder
 *
 */

public class Tour {
	
	public static class TourStats {
		public double transportTime;
		public double transportCosts;
		public double serviceTime;
		public double waitingTime;
		public double totalDutyTime;
		public int totalLoad;
		
		public void reset() {
			transportTime=0.0;
			transportCosts=0.0;
			serviceTime=0.0;
			waitingTime=0.0;
			totalDutyTime=0.0;
			totalLoad=0;
		}
		
	}
	
	private LinkedList<TourActivity> tourActivities = new LinkedList<TourActivity>();

	public TourStats costs = new TourStats();
	
	public LinkedList<TourActivity> getActivities() {
		return tourActivities;
	}
	
	public boolean isEmpty(){
		return (tourActivities.size() <= 2);
	}
	
	@Override
	public String toString() {
		String tour = "";
		for(TourActivity c : tourActivities){
			tour += "[" + c.getType() + "@" + c.getLocationId() + "@" + c.getEarliestOperationStartTime() + "-" + c.getLatestOperationStartTime() + "]";
		}
		tour += "[totalDutyTime=" + costs.totalDutyTime + "][transportTime=" + costs.transportTime + "][waitingTime="+costs.waitingTime+"][serviceTime=" + costs.serviceTime + 
			"][generalizedCosts=" + costs.transportCosts + "]"; 
		return tour;
	}

	public TourStats getTourStats() {
		return costs;
	}

}
