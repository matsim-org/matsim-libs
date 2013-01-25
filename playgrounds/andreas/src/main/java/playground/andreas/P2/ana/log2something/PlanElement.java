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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import playground.andreas.P2.replanning.modules.EndRouteExtension;
import playground.andreas.P2.replanning.modules.MaxRandomEndTimeAllocator;
import playground.andreas.P2.replanning.modules.MaxRandomStartTimeAllocator;
import playground.andreas.P2.replanning.modules.ReduceStopsToBeServedRFare;
import playground.andreas.P2.replanning.modules.ReduceTimeServedRFare;
import playground.andreas.P2.replanning.modules.SidewaysRouteExtension;
import playground.andreas.P2.replanning.modules.WeightedEndTimeExtension;
import playground.andreas.P2.replanning.modules.WeightedStartTimeExtension;

public class PlanElement {

	private static final Logger log = Logger.getLogger(PlanElement.class);
	
	private final int iterationFounded;
	private final String coopId;
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
		this.coopId = logElement.getCoopId();
		
		this.status = new ArrayList<Tuple<Integer, String>>();
		this.status.add(new Tuple<Integer, String>(new Integer(this.iterationFounded), logElement.getStatus()));
		
		this.planId = logElement.getPlanId();
		this.creatorId = logElement.getCreatorId();
		
		this.nVeh = new ArrayList<Tuple<Integer, Integer>>();
		this.nVeh.add(new Tuple<Integer, Integer>(new Integer(this.iterationFounded), new Integer(logElement.getnVeh())));
		
		this.nPax = new ArrayList<Tuple<Integer, Integer>>();
		this.nPax.add(new Tuple<Integer, Integer>(new Integer(this.iterationFounded), new Integer(logElement.getnPax())));
		
		this.score = new ArrayList<Tuple<Integer, Double>>();
		this.score.add(new Tuple<Integer, Double>(new Integer(this.iterationFounded), new Double(logElement.getScore())));
		
		this.budget = new ArrayList<Tuple<Integer, Double>>();
		this.budget.add(new Tuple<Integer, Double>(new Integer(this.iterationFounded), new Double(logElement.getBudget())));
		
		this.startTime = logElement.getStartTime();
		this.endTime = logElement.getEndTime();
		this.stopsToBeServed = logElement.getStopsToBeServed();
	}
	
	public String getUniquePlanIdentifier() {
		return this.coopId + "_" + this.planId;
	}
	
	public int getIterationFounded() {
		return iterationFounded;
	}

	public String getCoopId() {
		return coopId;
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
		this.status.add(new Tuple<Integer, String>(new Integer(logElement.getIteration()), logElement.getStatus()));
		this.nVeh.add(new Tuple<Integer, Integer>(new Integer(logElement.getIteration()), new Integer(logElement.getnVeh())));
		this.nPax.add(new Tuple<Integer, Integer>(new Integer(logElement.getIteration()), new Integer(logElement.getnPax())));
		this.score.add(new Tuple<Integer, Double>(new Integer(logElement.getIteration()), new Double(logElement.getScore())));
		this.budget.add(new Tuple<Integer, Double>(new Integer(logElement.getIteration()), new Double(logElement.getBudget())));
	}

	public boolean canBeChild(PlanElement planElement) {
		if (!this.getCoopId().equalsIgnoreCase(planElement.getCoopId())) {
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
