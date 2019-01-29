/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.minibus.stats.operatorLogger;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public final class PlanElement {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PlanElement.class);
	
	private final int iterationFounded;
	private final Id<Operator> operatorId;
	private final ArrayList<Tuple<Integer, OperatorState>> status;
	private final Id<PPlan> planId;
	private final String creatorId;
	private final Id<PPlan> parentId;
	private final ArrayList<Tuple<Integer, Integer>> nVeh;
	private final ArrayList<Tuple<Integer, Integer>> nPax;
	private final ArrayList<Tuple<Integer, Double>> score;
	private final ArrayList<Tuple<Integer, Double>> budget;
	private final double startTime;
	private final double endTime;
	private final ArrayList<Id<TransitStopFacility>> stopsToBeServed;
	private final ArrayList<Id<Link>> linksServed;
	
	private PlanElement parentPlan;
	
	private int iterationCeased = Integer.MAX_VALUE;


	public PlanElement(LogElement logElement) {
		this.iterationFounded = logElement.getIteration();
		this.operatorId = logElement.getOperatorId();
		
		this.status = new ArrayList<>();
		this.status.add(new Tuple<>(this.iterationFounded, logElement.getStatus()));
		
		this.planId = logElement.getPlanId();
		this.creatorId = logElement.getCreatorId();
		this.parentId = logElement.getParentId();
		
		this.nVeh = new ArrayList<>();
		this.nVeh.add(new Tuple<>(this.iterationFounded, logElement.getnVeh()));
		
		this.nPax = new ArrayList<>();
		this.nPax.add(new Tuple<>(this.iterationFounded, logElement.getnPax()));
		
		this.score = new ArrayList<>();
		this.score.add(new Tuple<>(this.iterationFounded, logElement.getScore()));
		
		this.budget = new ArrayList<>();
		this.budget.add(new Tuple<>(this.iterationFounded, logElement.getBudget()));
		
		this.startTime = logElement.getStartTime();
		this.endTime = logElement.getEndTime();
		this.stopsToBeServed = logElement.getStopsToBeServed();
		this.linksServed = logElement.getLinksServed();
	}
	
	public String getUniquePlanIdentifier() {
		return this.operatorId + "_" + this.planId;
	}
	
	public int getIterationFounded() {
		return iterationFounded;
	}

	public Id<Operator> getOperatorId() {
		return operatorId;
	}

	public ArrayList<Tuple<Integer, OperatorState>> getStatus() {
		return status;
	}

	public Id<PPlan> getPlanId() {
		return planId;
	}

	public String getCreatorId() {
		return creatorId;
	}
	
	public Id<PPlan> getParentId() {
		return parentId;
	}

	public ArrayList<Tuple<Integer, Integer>> getnVeh() {
		return nVeh;
	}

	public ArrayList<Tuple<Integer, Integer>> getnPax() {
		return nPax;
	}

	public ArrayList<Tuple<Integer, Double>> getScore() {
		return score;
	}

	public ArrayList<Tuple<Integer, Double>> getBudget() {
		return budget;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public ArrayList<Id<TransitStopFacility>> getStopsToBeServed() {
		return stopsToBeServed;
	}
	
	public ArrayList<Id<Link>> getLinksServed() {
		return linksServed;
	}
	
	public int getIterationCeased() {
		return this.iterationCeased;
	}
	
	public void setIterationCeased(int iterationCeased) {
		this.iterationCeased = iterationCeased;
	}

	public PlanElement getParentPlan() {
		return this.parentPlan;
	}
	
	public void setParentPlan(PlanElement parent) {
		this.parentPlan = parent;
	}
	
	public void update(LogElement logElement) {
		this.status.add(new Tuple<>(logElement.getIteration(), logElement.getStatus()));
		this.nVeh.add(new Tuple<>(logElement.getIteration(), logElement.getnVeh()));
		this.nPax.add(new Tuple<>(logElement.getIteration(), logElement.getnPax()));
		this.score.add(new Tuple<>(logElement.getIteration(), logElement.getScore()));
		this.budget.add(new Tuple<>(logElement.getIteration(), logElement.getBudget()));
	}

}
