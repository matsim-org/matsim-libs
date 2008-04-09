/* *********************************************************************** *
 * project: org.matsim.*
 * PlanScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.contentment;

import org.apache.log4j.Logger;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.withinday.WithindayAgent;

/**
 * @author fearonni
 * @author dgrether
 *
 */
public class PlanScore implements AgentContentmentI {

	private static final Logger log = Logger.getLogger(PlanScore.class);

	private double contentment;

	private WithindayAgent agent;

	private double[] referenceScore;

	private double[] zeroUtilDuration;

	private double planDeviation;

	private double distanceFactor;

	private double lastCallTime;

	private double gamma_late;

	private double gamma_act;

	private static final int priority = 1;

	// =====================================================================
	// constructor
	// =====================================================================

	public PlanScore(final WithindayAgent agent, final double gammaAct, final double gammaLate) {
		this.agent = agent;
		//from builder
		setGamma_act(gammaAct);
		setGamma_late(gammaLate);
		calcReferenceVals();
	}

	// =====================================================================
	// accessors
	// =====================================================================

	public void setGamma_act(final double gamma_act) {
		this.gamma_act = gamma_act;
	}

	public void setGamma_late(final double gamma_late) {
		this.gamma_late = gamma_late;
	}



	// =====================================================================
	// instance methods
	// =====================================================================

	@SuppressWarnings("unchecked")
	public double getContentment() {
		double time = SimulationTimer.getTime();
		if (time > this.lastCallTime) {
			if (this.agent.isEnRoute()) {
				Leg leg = this.agent.getVehicle().getCurrentLeg();
				if (leg.getRoute() != null) {
					this.contentment = calcContentment(leg, time);
				} else {
					this.contentment = 0;
				}
			} else {
				this.contentment = 0;
			}
		}

		this.lastCallTime = time;
		return this.contentment;

	}

	public void didReplan(final boolean modified) {
		if (modified) {
			this.lastCallTime = -1;
			calcReferenceVals();
		}
	}

	private double calcContentment(final Leg leg, final double time) {
		TravelCostI travelCost = this.agent.getBeliefs();
		TravelTimeI travelTime = this.agent.getBeliefs();
		if (travelCost == null) {
			log.warn("aaent: " + this.agent + " has no TravelCostProvider");
			return 0;
		}
		/*
		 * Determine how the remaining distance and traveltime to the
		 * destination.
		 */
		Link currentLink = this.agent.getVehicle().getCurrentLink();
		int duration = 0;
		double distance = 0;
		double totalDistance = 0;
		double length = 0;

		Link [] route = leg.getRoute().getLinkRoute();
		int posInRoute = 0;
		//determine the actual position in the route
		for (int j = 0; j < route.length; j++) {
			if (route[j].equals(currentLink)) {
				posInRoute = j;
			}
		}
		//determine remaining time
		for (int i = 0; i < route.length; i++) {
			length = route[i].getLength();
			totalDistance += length;
			//do this only for links on which we are or will be in the future
			if (i >= posInRoute) {
				duration += travelTime.getLinkTravelTime(route[i], time);
				distance += length;
			}
		}
		/*
		 * subtract the last link, we assume the agent has arrived when he
		 * enters his last link.
		 */
		//we don't have to do that using the QueueSimulation
//		distance -= leg.getRoute().getDestinationLink().getLength_m();

		int planIdx = 0;
		for (Object o : this.agent.getPerson().getSelectedPlan().getActsLegs()) {
			if (o.equals(this.agent.getVehicle().getCurrentLeg())) {
				break;
			}
			planIdx++;
		}
		//TODO dg remove log
		log.debug("getNextActivity: " + leg + " route: " + leg.getRoute().getRoute());
		Act nextAct = this.agent.getPerson().getSelectedPlan().getNextActivity(leg);
		//TODO dg remove if
		if (nextAct == null) {
			log.debug("next act null of leg: " + leg);
		}
		double nextActDuration = (nextAct.getEndTime() - nextAct.getStartTime());

		double score = calcPlanScore(time + duration, leg.getArrTime(), nextActDuration, this.zeroUtilDuration[planIdx]);
		if (this.referenceScore[planIdx] == 0) {
			this.planDeviation = 0;
		} else {
			this.planDeviation = (1 - (score / this.referenceScore[planIdx])) * -1;
		}

		if (totalDistance == 0) {
			this.distanceFactor = 1;
		}
		else {
			this.distanceFactor = 1 - Math.exp(-(10 / totalDistance) * distance);
		}

		return this.planDeviation * this.distanceFactor;
	}


	private double calcPlanScore(final double predArrvTime, final double schedArrvTime,
			double actDuration, final double zeroUtilDur) {
		double V = 0;

		if (zeroUtilDur == 0) {
			return V;
		}

		double timeMissed = predArrvTime - Math.max(schedArrvTime, 0);

		V += this.gamma_late * (timeMissed / 3600.0);

		actDuration = Math.max(actDuration, 0);
		if (actDuration > timeMissed) {
			V += this.gamma_act
					* (actDuration / 3600.0)
					* Math.log(((actDuration - timeMissed) / 3600.0)
							/ zeroUtilDur);
		}
		else {
			V += Double.MIN_VALUE;
		}

		return V;
	}

	private void calcReferenceVals() {
		Plan plan = this.agent.getPerson().getSelectedPlan();
		int planEntries = plan.getActsLegs().size();
		this.referenceScore = new double[planEntries + 1];
		this.zeroUtilDuration = new double[planEntries + 1];
		int index = 0;
		for (Object o : plan.getActsLegs()) {
			if (o instanceof Leg) {
				Leg leg = (Leg)o;
				Act nextAct = this.agent.getPerson().getSelectedPlan().getNextActivity(leg);
				double actDur = Math.max((nextAct.getEndTime() - nextAct.getStartTime()), 0.0d);
				double endTime = Math.max(leg.getArrTime(), 0.0d);

				this.zeroUtilDuration[index] =  (actDur / 3600.0) * Math
						.exp(-10.0 / ((actDur / 3600.0) * priority));
				this.zeroUtilDuration[index + 1] = this.zeroUtilDuration[index];

				this.referenceScore[index] = calcPlanScore(endTime, endTime, actDur,
						this.zeroUtilDuration[index]);
				this.referenceScore[index + 1] = this.referenceScore[index];
			}
			index++;
		}

	}

}