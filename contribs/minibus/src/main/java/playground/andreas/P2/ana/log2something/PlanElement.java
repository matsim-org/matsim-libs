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

package playground.andreas.P2.ana.log2something;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import playground.andreas.P2.replanning.*;

import java.util.ArrayList;

public final class PlanElement {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PlanElement.class);
	
	private final int iterationFounded;
	private final String operatorId;
	private final ArrayList<Tuple<Integer, String>> status;
	private final String planId;
	private final String creatorId;
	private final ArrayList<Tuple<Integer, Integer>> nVeh;
	private final ArrayList<Tuple<Integer, Integer>> nPax;
	private final ArrayList<Tuple<Integer, Double>> score;
	private final ArrayList<Tuple<Integer, Double>> budget;
	private final double startTime;
	private final double endTime;
	private final String[] stopsToBeServed;
	
	private int iterationCeased = Integer.MAX_VALUE;

	private PlanElement ancestor = null;
	
	public PlanElement(LogElement logElement) {
		this.iterationFounded = logElement.getIteration();
		this.operatorId = logElement.getOperatorId();
		
		this.status = new ArrayList<>();
		this.status.add(new Tuple<>(this.iterationFounded, logElement.getStatus()));
		
		this.planId = logElement.getPlanId();
		this.creatorId = logElement.getCreatorId();
		
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
	}
	
	public String getUniquePlanIdentifier() {
		return this.operatorId + "_" + this.planId;
	}
	
	public int getIterationFounded() {
		return iterationFounded;
	}

	public String getOperatorId() {
		return operatorId;
	}

	public ArrayList<Tuple<Integer, String>> getStatus() {
		return status;
	}

	public String getPlanId() {
		return planId;
	}

	public String getCreatorId() {
		return creatorId;
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

	public String[] getStopsToBeServed() {
		return stopsToBeServed;
	}
	
	public int getIterationCeased() {
		return this.iterationCeased;
	}
	
	public void setIterationCeased(int iterationCeased) {
		this.iterationCeased = iterationCeased;
	}

	public void update(LogElement logElement) {
		this.status.add(new Tuple<>(logElement.getIteration(), logElement.getStatus()));
		this.nVeh.add(new Tuple<>(logElement.getIteration(), logElement.getnVeh()));
		this.nPax.add(new Tuple<>(logElement.getIteration(), logElement.getnPax()));
		this.score.add(new Tuple<>(logElement.getIteration(), logElement.getScore()));
		this.budget.add(new Tuple<>(logElement.getIteration(), logElement.getBudget()));
	}

	public boolean canBeChild(PlanElement planElement) {
		if (!this.getOperatorId().equalsIgnoreCase(planElement.getOperatorId())) {
			// wrong family
			return false;
		}
		
		if (this.getIterationFounded() > planElement.getIterationFounded() || this.getIterationCeased() < planElement.getIterationFounded()) {
			// they never met
			return false;
		}
		
		if (planElement.getCreatorId().equalsIgnoreCase(WeightedStartTimeExtension.STRATEGY_NAME) || planElement.getCreatorId().equalsIgnoreCase(MaxRandomStartTimeAllocator.STRATEGY_NAME)) {
			if (this.getStartTime() > planElement.getStartTime() && this.getEndTime() == planElement.getEndTime() && this.getStopsToBeServed().length == planElement.getStopsToBeServed().length) {
				return true;
			} else {
				return false;
			}
		}
		
		if (planElement.getCreatorId().equalsIgnoreCase(WeightedEndTimeExtension.STRATEGY_NAME) || planElement.getCreatorId().equalsIgnoreCase(MaxRandomEndTimeAllocator.STRATEGY_NAME)) {
			if (this.getStartTime() == planElement.getStartTime()  && this.getEndTime() < planElement.getEndTime() && this.getStopsToBeServed().length == planElement.getStopsToBeServed().length) {
				return true;
			} else {
				return false;
			}
		}
		
		if (planElement.getCreatorId().equalsIgnoreCase(EndRouteExtension.STRATEGY_NAME)) {
			if (this.getStartTime() == planElement.getStartTime()  && this.getEndTime() == planElement.getEndTime() && this.getStopsToBeServed().length + 1== planElement.getStopsToBeServed().length) {
				return true;
			} else {
				return false;
			}
		}
		
		if (planElement.getCreatorId().equalsIgnoreCase(SidewaysRouteExtension.STRATEGY_NAME)) {
			if (this.getStartTime() == planElement.getStartTime()  && this.getEndTime() == planElement.getEndTime() && this.getStopsToBeServed().length + 2 == planElement.getStopsToBeServed().length) {
				return true;
			} else {
				return false;
			}
		}
		
		if (planElement.getCreatorId().equalsIgnoreCase(ReduceTimeServedRFare.STRATEGY_NAME)) {
			if (this.getStopsToBeServed().length == planElement.getStopsToBeServed().length) {
				return true;
			} else {
				return false;
			}
		}
		
		if (planElement.getCreatorId().equalsIgnoreCase(ReduceStopsToBeServedRFare.STRATEGY_NAME)) {
			if (this.getStartTime() == planElement.getStartTime()  && this.getEndTime() == planElement.getEndTime()) {
				return true;
			} else {
				return false;
			}
		}
		
		// Couldn't guess the strategy
		return false;
	}

	public PlanElement getAncestor() {
		return this.ancestor;
	}
	
	public void setAncestor(PlanElement candidate) {
		this.ancestor  = candidate;
	}

}
