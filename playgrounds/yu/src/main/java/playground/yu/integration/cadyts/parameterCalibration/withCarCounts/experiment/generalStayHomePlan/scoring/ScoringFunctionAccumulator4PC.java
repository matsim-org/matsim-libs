/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionAccumulator4PC.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalStayHomePlan.scoring;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.AgentStuckScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.LegScoring;
import org.matsim.core.scoring.interfaces.MoneyScoring;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalStayHomePlan.withLegModeASC.LegScoringFunction4PC;

/**
 * @author yu
 * 
 */
// in order to get attributes, e.g. actDur, legDurCar, legDurPt, stuck?
public class ScoringFunctionAccumulator4PC implements ScoringFunction {
	protected CharyparNagelScoringParameters params;

	protected ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private final ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private final ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private final ArrayList<LegScoring> legScoringFunctions = new ArrayList<LegScoring>();
	private final ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();
	private double perfAttr/* [h] */, travTimeAttrCar/* [h] */,
			travTimeAttrPt/* [h] */, stuckAttr = 0d/* [utils] */,
			travTimeAttrWalk/* [h] */, distanceCar/* [m] */, distancePt/* [m] */,
			distanceWalk/* [m] */;
	private int carLegNo, ptLegNo, walkLegNo;

	public ScoringFunctionAccumulator4PC(CharyparNagelScoringParameters params) {
		this.params = params;
	}

	public double getPerfAttr() {
		return perfAttr;
	}

	public double getTravTimeAttrCar() {
		return travTimeAttrCar;
	}

	public double getTravTimeAttrPt() {
		return travTimeAttrPt;
	}

	public Double getTravTimeAttrWalk() {
		return travTimeAttrWalk;
	}

	public double getStuckAttr() {
		return stuckAttr;
	}

	public int getCarLegNo() {
		return carLegNo;
	}

	public int getPtLegNo() {
		return ptLegNo;
	}

	public int getWalkLegNo() {
		return walkLegNo;
	}

	public double getDistanceCar() {
		return distanceCar;
	}

	public double getDistancePt() {
		return distancePt;
	}

	public double getDistanceWalk() {
		return distanceWalk;
	}

	@Override
	public void addMoney(double amount) {
		for (MoneyScoring moneyScoringFunction : moneyScoringFunctions) {
			moneyScoringFunction.addMoney(amount);
		}
	}

	@Override
	public void agentStuck(double time) {
		for (AgentStuckScoring agentStuckScoringFunction : agentStuckScoringFunctions) {
			agentStuckScoringFunction.agentStuck(time);
		}
	}

	@Override
	public void startActivity(double time, Activity act) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.startActivity(time, act);
		}
	}

	@Override
	public void endActivity(double time) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.endActivity(time);
		}
	}

	@Override
	public void startLeg(double time, Leg leg) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.startLeg(time, leg);
		}
	}

	@Override
	public void endLeg(double time) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.endLeg(time);
		}
	}

	@Override
	public void finish() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.finish();
		}
	}

	/**
	 * Add the score of all functions.
	 */
	@Override
	public double getScore() {
		double score = 0.0;
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			double fracScore = basicScoringFunction.getScore();
			score += fracScore;
			// System.out.println("SCORE:\tafter scoringCfg function: "
			// + basicScoringFunction.getClass().getName() + " is: "
			// + basicScoringFunction.getScore());
			if (basicScoringFunction instanceof ActivityScoring) {
				perfAttr = params.marginalUtilityOfPerforming_s != 0d ? fracScore
						/ (params.marginalUtilityOfPerforming_s * 3600d)
						: 0d;
			} else if (basicScoringFunction instanceof LegScoringFunction) {
				LegScoringFunction4PC legScoringFunction = (LegScoringFunction4PC) basicScoringFunction;

				travTimeAttrCar = legScoringFunction.getTravTimeAttrCar();
				travTimeAttrPt = legScoringFunction.getTravTimeAttrPt();
				travTimeAttrWalk = legScoringFunction.getTravTimeAttrWalk();

				carLegNo = legScoringFunction.getCarLegNo();
				ptLegNo = legScoringFunction.getPtLegNo();
				walkLegNo = legScoringFunction.getWalkLegNo();

				distanceCar = legScoringFunction.getDistanceAttrCar();
				distancePt = legScoringFunction.getDistanceAttrPt();
				distanceWalk = legScoringFunction.getDistanceAttrWalk();
			} else if (basicScoringFunction instanceof AgentStuckScoring) {
				double betaStuck = Math.min(Math.min(
						params.marginalUtilityOfLateArrival_s,
						params.marginalUtilityOfEarlyDeparture_s), Math.min(
						params.marginalUtilityOfTraveling_s,
						params.marginalUtilityOfWaiting_s));

				stuckAttr = betaStuck != 0d ? fracScore / (betaStuck * 3600d)
						: 0d;
			}
		}
		return score;
	}

	@Override
	public void reset() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.reset();
		}
	}

	/**
	 * add the scoring function the list of functions, it implemented the
	 * interfaces.
	 * 
	 * @param scoringFunction
	 */
	public void addScoringFunction(BasicScoring scoringFunction) {
		basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoring) {
			activityScoringFunctions.add((ActivityScoring) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoring) {
			agentStuckScoringFunctions.add((AgentStuckScoring) scoringFunction);
		}

		if (scoringFunction instanceof LegScoring) {
			legScoringFunctions.add((LegScoring) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoring) {
			moneyScoringFunctions.add((MoneyScoring) scoringFunction);
		}

	}

	public ArrayList<ActivityScoring> getActivityScoringFunctions() {
		return activityScoringFunctions;
	}

}