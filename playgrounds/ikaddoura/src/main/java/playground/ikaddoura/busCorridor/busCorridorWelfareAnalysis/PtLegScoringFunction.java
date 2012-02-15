/* *********************************************************************** *
 * project: org.matsim.*
 * InVehicleWaitingScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.busCorridorWelfareAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */
public class PtLegScoringFunction implements BasicScoring {

	protected double score;
	protected Plan plan;
	
	private double TRAVEL_PT_IN_VEHICLE;
	private double TRAVEL_PT_WAITING;
	
	private Map <Id, Double> personId2WaitingTime = new HashMap<Id, Double>();
	private Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();

	/**
	 * @param TRAVEL_PT_WAITING 
	 * @param TRAVEL_PT_IN_VEHICLE 
	 * @param personId2InVehicleTime2
	 * @param personId2WaitingTime2
	 */
	public PtLegScoringFunction(Plan plan, Map<Id, Double> personId2InVehicleTime, Map<Id, Double> personId2WaitingTime, double TRAVEL_PT_IN_VEHICLE, double TRAVEL_PT_WAITING) {
		this.personId2InVehicleTime = personId2InVehicleTime;
		this.personId2WaitingTime = personId2WaitingTime;
		this.TRAVEL_PT_IN_VEHICLE = TRAVEL_PT_IN_VEHICLE;
		this.TRAVEL_PT_WAITING = TRAVEL_PT_WAITING;
		this.plan = plan;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}

	@Override
	public double getScore() {
		double score = 0.0;
		Id personId = this.plan.getPerson().getId();
		if (this.personId2InVehicleTime.containsKey(personId) && this.personId2WaitingTime.containsKey(personId)){
			double inVehTime = this.personId2InVehicleTime.get(personId);
			double waitTime = this.personId2WaitingTime.get(personId);
//			System.out.println("InVehTime: "+Time.writeTime(inVehTime, Time.TIMEFORMAT_HHMMSS));
//			System.out.println("waitTime: "+Time.writeTime(waitTime, Time.TIMEFORMAT_HHMMSS));
			score = this.TRAVEL_PT_IN_VEHICLE/3600 * inVehTime + this.TRAVEL_PT_WAITING/3600 * waitTime;
		}
		return score;
	}

	@Override
	public void reset() {
		this.score = 0;
		this.plan = null;
	}

	/**
	 * @param plan
	 */
	public void setPlan(Plan plan) {
		System.out.println("plan set: "+plan.getPerson().getId());
		this.plan = plan;
	}

}
