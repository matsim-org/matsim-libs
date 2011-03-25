/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionAccumulator4PC2.java
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general2;

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

/**
 * @author yu
 * 
 */
// in order to get attributes, e.g. actDur, legDurCar, legDurPt, stuck?
public class ScoringFunctionAccumulator4PC2 implements ScoringFunction {
	protected CharyparNagelScoringParameters params;

	protected ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private ArrayList<LegScoring> legScoringFunctions = new ArrayList<LegScoring>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();

	private double perfAttr/* [h] */, travTimeAttrCar/* [h] */, lnPathSizeAttr;
	private int nbSpeedBumpsAttr, nbLeftTurnsAttr, nbIntersectionsAttr;

	public ScoringFunctionAccumulator4PC2(CharyparNagelScoringParameters params) {
		this.params = params;
	}

	// //////////////////////////////////////////////////////
	// SETTERS, GETTERS
	public double getPerfAttr() {
		return perfAttr;
	}

	public double getTravTimeAttrCar() {
		return travTimeAttrCar;
	}

	public int getNbSpeedBumps() {
		return nbSpeedBumpsAttr;
	}

	public int getNbLeftTurns() {
		return nbLeftTurnsAttr;
	}

	public int getNbIntersections() {
		return nbIntersectionsAttr;
	}

	// /////////////////////////////////////////////////////
	// IMPLEMENTED METHODS
	public void addMoney(double amount) {
		for (MoneyScoring moneyScoringFunction : moneyScoringFunctions) {
			moneyScoringFunction.addMoney(amount);
		}
	}

	public void agentStuck(double time) {
		for (AgentStuckScoring agentStuckScoringFunction : agentStuckScoringFunctions) {
			agentStuckScoringFunction.agentStuck(time);
		}
	}

	public void startActivity(double time, Activity act) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.startActivity(time, act);
		}
	}

	public void endActivity(double time) {
		for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
			activityScoringFunction.endActivity(time);
		}
	}

	public void startLeg(double time, Leg leg) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.startLeg(time, leg);
		}
	}

	public void endLeg(double time) {
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.endLeg(time);
		}
	}

	public void finish() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.finish();
		}
	}

	/**
	 * Add the score of all functions.
	 */
	public double getScore() {
		double score = 0.0;
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			double fracScore = basicScoringFunction.getScore();
			score += fracScore;

			if (basicScoringFunction instanceof ActivityScoring) {
				perfAttr = params.marginalUtilityOfPerforming_s != 0d ? fracScore
						/ (params.marginalUtilityOfPerforming_s * 3600d)
						: 0d;
			} else if (basicScoringFunction instanceof LegScoringFunction) {
				LegScoringFunction4PC2 legScoringFunction = (LegScoringFunction4PC2) basicScoringFunction;

				travTimeAttrCar = legScoringFunction.getTravTimeAttrCar();

				nbSpeedBumpsAttr = legScoringFunction.getNbSpeedBumps();
				nbLeftTurnsAttr = legScoringFunction.getNbLeftTurns();
				nbIntersectionsAttr = legScoringFunction.getNbIntersections();
			}// TODO
		}
		return score;
	}

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