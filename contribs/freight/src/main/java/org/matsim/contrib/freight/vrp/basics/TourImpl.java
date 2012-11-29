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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author stefan schroeder
 * 
 */

public class TourImpl implements Tour {

	public static class TourData {
		public double transportTime;
		public double transportCosts;
		public int totalLoad;
		public double totalCost;

		public void reset() {
			transportTime = 0.0;
			transportCosts = 0.0;
			totalLoad = 0;
			totalCost = 0.0;
		}

	}

	private final LinkedList<TourActivity> tourActivities = new LinkedList<TourActivity>();

	private Set<Job> jobs = new HashSet<Job>();
	
	@Deprecated
	public TourData tourData = new TourData();
	
	private int load = 0;
	
	private double totalCost = 0.0;
	
	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public int getLoad() {
		return load;
	}

	public void setLoad(int load) {
		this.load = load;
	}

	public TourImpl(TourImpl tour2copy) {
		for (TourActivity tourAct : tour2copy.getActivities()) {
			this.tourActivities.add(tourAct.duplicate());
			addJob(tourAct);
		}
		this.load = tour2copy.getLoad();
		this.totalCost = tour2copy.getTotalCost();
		this.tourData.transportCosts = tour2copy.tourData.transportCosts;
		this.tourData.transportTime = tour2copy.tourData.transportTime;
		this.tourData.totalLoad = tour2copy.tourData.totalLoad;
		this.tourData.totalCost = tour2copy.tourData.totalCost;
	}

	public TourImpl() {
		super();
	}

	public boolean removeActivity(TourActivity tourAct){
		removeJob(tourAct);
		return tourActivities.remove(tourAct);
	}
	
	private void removeJob(TourActivity tourAct) {
		if(tourAct instanceof JobActivity){
			jobs.remove(tourAct);
		}
		
	}

	public boolean removeJob(Job job){
		if(!jobs.contains(job)){
			return false;
		}
		else{
			jobs.remove(job);
		}
		boolean removed = false;
		List<TourActivity> acts = new ArrayList<TourActivity>(tourActivities);
		for(TourActivity c : acts){
			if(c instanceof JobActivity){
				if(job.getId().equals(((JobActivity) c).getJob().getId())){
					tourActivities.remove(c);
					removed = true;
				}
			}
		}
		return removed;
	}
	
	@Override
	public List<TourActivity> getActivities() {
		return Collections.unmodifiableList(tourActivities);
	}

	public boolean isEmpty() {
		return (tourActivities.size() <= 2);
	}
	
	public void reset(){
		this.tourData.reset();
		setLoad(0);
		setTotalCost(0.0);
		jobs.clear();
		tourActivities.get(0).setCurrentCost(0.0);
		tourActivities.get(0).setCurrentLoad(0);
		tourActivities.get(tourActivities.size()-1).setCurrentCost(0.0);
		tourActivities.get(tourActivities.size()-1).setCurrentLoad(0);
		
	}

	@Override
	public String toString() {
		String tour = "";
		for (TourActivity c : tourActivities) {
			tour += "[" + c.getClass().getSimpleName() + "@"
					+ c.getLocationId() + "@"
					+ c.getEarliestOperationStartTime() + "-"
					+ c.getLatestOperationStartTime() + "]";
		}
		tour += "[transportTime=" + tourData.transportTime
				+ "][transportCosts=" + tourData.transportCosts + "]";
		return tour;
	}
	
	public TourImpl duplicate(){
		return new TourImpl(this);
	}

	public void addActivity(int deliveryInsertionIndex, TourActivity act) {
		tourActivities.add(deliveryInsertionIndex, act);
		addJob(act);
	}
	
	public void addActivity(TourActivity act){
		tourActivities.add(act);
		addJob(act);
	}

	private void addJob(TourActivity act) {
		if(act instanceof JobActivity){
			jobs.add(((JobActivity) act).getJob());
		}
	}

}
