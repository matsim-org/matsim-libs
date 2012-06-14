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
