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
	
	public static class TourData {
		public double transportTime;
		public double transportCosts;
		public int totalLoad;
		
		public void reset() {
			transportTime=0.0;
			transportCosts=0.0;
			totalLoad=0;
		}
		
	}
	
	private final LinkedList<TourActivity> tourActivities = new LinkedList<TourActivity>();

	public TourData tourData = new TourData();
	
	public Tour(Tour tour2copy){
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		for(TourActivity tourAct : tour2copy.getActivities()){
			tourBuilder.copyAndScheduleActivity(tourAct);
		}
		Tour t = tourBuilder.build();
		this.tourActivities.addAll(t.getActivities());
		this.tourData.transportCosts = tour2copy.tourData.transportCosts;
		this.tourData.transportTime = tour2copy.tourData.transportTime;
		this.tourData.totalLoad = tour2copy.tourData.totalLoad;
	}
	
	public Tour() {
		super();
	}

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
		tour += "[transportTime=" + tourData.transportTime +  
			"][transportCosts=" + tourData.transportCosts + "]"; 
		return tour;
	}

}
